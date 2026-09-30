# 배포 구조

이 프로젝트는 배포 구조를 두 가지로 정리했습니다.

| 구분 | 목표 아키텍처 (Terraform) | 실제 운영 (EC2) |
| --- | --- | --- |
| 위치 | [`infrastructure/`](../infrastructure) | [`deploy/`](../deploy), `docker-compose.yml` |
| 목적 | 트래픽이 늘었을 때의 운영형 구조를 코드로 설계·검증 | 실제 서비스([stock-system.cloud](https://stock-system.cloud)) 운영 |
| 실행 환경 | ECS Fargate + RDS + ElastiCache + ALB + CloudFront | EC2 1대 + docker compose |
| 상태 | 설계 완료, 실제 AWS에서 임시 적용 테스트 완료 | 운영 중 |

두 방식은 **같은 이미지 빌드 파일(`backend/Dockerfile.module`)과 같은 애플리케이션 설정(`prod` 프로필, Flyway, 서비스별 DB)**을 사용합니다. 그래서 코드 변경 없이 인프라만 바꿔 옮겨 갈 수 있습니다.

---

## 1. 목표 아키텍처 (Terraform)

```
사용자
 ├─ HTTPS ─▶ CloudFront ─▶ S3 (프론트 빌드 결과, 프로필 이미지)
 └─ HTTPS ─▶ ALB (ACM 인증서, /actuator* 404 차단)
              └─▶ bff-server (ECS Fargate, 오토스케일링)
                    ├─▶ account-server (내부 전용, 오토스케일링)
                    └─▶ stock-server-a / stock-server-b (내부 전용, 그룹당 1개 고정)

Private 서브넷 (2 AZ)
  ECS 서비스 ──▶ RDS MySQL (Multi-AZ, bff_db / stock_db / account_db)
             └─▶ ElastiCache Redis
  외부 호출(증권사 실시간 시세 API 등) ──▶ NAT Gateway
```

### 모듈 구성

| 모듈 | 역할 |
| --- | --- |
| `network` | VPC, 2 AZ (ap-northeast-2a / 2c), Public · Private App · Private Data 서브넷, NAT Gateway |
| `security` | ALB, 서비스별(bff / stock / account), RDS, Redis 보안 그룹 |
| `dns` | Route 53 호스팅 영역, ACM 인증서 (API용 서울 리전, CloudFront용 us-east-1) |
| `alb` | HTTPS 리스너, bff 대상 그룹, `/actuator*` 외부 차단 규칙 |
| `frontend` | S3 + CloudFront (프론트), 업로드 버킷 |
| `ecr` | bff / stock / account 이미지 저장소 |
| `ecs-cluster`, `ecs-service` | Fargate 클러스터, Cloud Map 서비스 디스커버리(`stock-system.internal`), 서비스별 오토스케일링 |
| `rds` | MySQL (기본 `db.t4g.medium`, Multi-AZ, 삭제 보호) |
| `elasticache` | Redis (기본 `cache.t4g.small`) |
| `secrets` | Secrets Manager: 서비스별 DB 계정, bff 앱 시크릿, stock 그룹별 증권사 API 키 |
| `monitoring` | CloudWatch 알람 (ALB 5xx·비정상 대상, RDS CPU·스토리지, stock 태스크 중단) + SNS 이메일 알림 |
| `github-oidc` | GitHub Actions가 키 없이 ECR push, ECS 배포, S3/CloudFront 반영을 하도록 OIDC 역할 |
| `bootstrap` | Terraform 상태 저장용 S3 버킷 (`use_lockfile`로 잠금) |

### 서비스별 배치 원칙

| 서비스 | 배치 | 이유 |
| --- | --- | --- |
| bff-server | ALB 뒤, 오토스케일링 (CPU 60%, 대상당 요청 1,000) | 사용자 요청과 WebSocket의 진입점이라 부하에 따라 늘려야 함 |
| account-server | 내부 전용, 오토스케일링 (CPU 60%) | Redis Streams 컨슈머 그룹으로 처리를 나눠 받으므로 수평 확장 가능 |
| stock-server-a/b | 내부 전용, **그룹당 1개 고정**, 오토스케일링 없음 | 그룹마다 담당 종목의 체결, 증권사 WebSocket, 스케줄을 단독으로 처리함. 같은 그룹이 2개 뜨면 체결과 스케줄이 중복되므로, 배포 시에도 이전 태스크를 먼저 내린 뒤 새 태스크를 띄움 (`minimum_healthy_percent = 0`) |

- stock 그룹은 `stock_groups` 변수에 추가하면 서비스와 시크릿이 하나씩 늘어나는 구조입니다. 종목을 그룹으로 나눠 수평 확장합니다.
- 비용을 줄일 때는 `single_nat_gateway = true`, `db_multi_az = false`로 바꿀 수 있게 열어 두었습니다.

---

## 2. 실제 운영 구조 (EC2)

```
사용자 ──HTTPS──▶ nginx (EC2 호스트, Let's Encrypt)
                   ├ /                        → /var/www/stock-system (프론트 빌드 결과)
                   ├ /api, /uploads           → bff-server (127.0.0.1:8080)
                   └ /ws-stock, /ws-order     → bff-server (WebSocket)

docker compose (internal 네트워크)
  bff-server ──▶ account-server (:8082)
             └─▶ stock-server (A 그룹, :8081)
  stock-server, stock-server-b (B 그룹) ──▶ account-server
  모든 서버 ──▶ MySQL (bff_db / stock_db / account_db), Redis
```

| 구성 요소 | 방식 |
| --- | --- |
| 서버 | bff, account, stock A/B를 docker compose로 실행. 외부에는 bff의 8080만 nginx에 공개 |
| DB · 캐시 | MySQL, Redis 컨테이너 (볼륨으로 데이터 유지) |
| 프론트 | EC2에서 빌드해 nginx가 정적 파일로 제공 |
| HTTPS | nginx + Certbot (Let's Encrypt) |
| DNS | 도메인 등록 업체(가비아)에서 관리 (Route 53 미사용) |
| 모니터링 | Prometheus + Grafana (`docker-compose.monitoring.yml`) |
| 스키마 | Flyway (서버별 이력 테이블, stock A/B는 같은 `stock_db`를 Flyway 잠금으로 공유) |

### CD (GitHub Actions)

| 워크플로 | 동작 |
| --- | --- |
| `backend-ci.yaml` | 백엔드 빌드와 테스트 (`./gradlew clean build`) |
| `deploy.yaml` | main에 push되면 변경된 모듈만 이미지를 빌드해 **GHCR**에 push하고, AWS OIDC 인증 후 **SSM**으로 EC2에서 `docker compose pull` + `up -d --no-deps` 실행. 프론트가 바뀌면 EC2에서 빌드 후 교체. stock-server는 여기서 재시작하지 않음 |
| `deploy-stock.yaml` | stock-server A/B만 배포. **평일 20:30 자동 실행** (20:05 애프터마켓 정리 이후), 수동 실행은 평일 08:40~20:15에 차단 (`force`로만 가능). A → B 순서로 재시작 후 상태 확인 |

- SSH 키나 AWS 액세스 키를 GitHub에 저장하지 않습니다. OIDC 역할 + SSM으로 명령만 전달합니다.
- 두 배포 워크플로는 같은 `concurrency` 그룹이라 동시에 실행되지 않습니다.

---

## 3. 왜 이렇게 나눴는가

### 운영은 EC2 한 대: 비용

목표 아키텍처는 사용자가 없어도 **항상 켜져 있는 관리형 리소스**가 많습니다.

| 항목 | 목표 아키텍처에서 계속 나가는 비용 | EC2 구조 |
| --- | --- | --- |
| 네트워크 | NAT Gateway 2개 (AZ마다, 시간당 요금 + 데이터 처리 요금) | 없음 (EC2가 직접 외부 호출) |
| 로드밸런서 | ALB 시간당 요금 + LCU | nginx (무료) |
| DB | RDS Multi-AZ (인스턴스 2대분 요금) | MySQL 컨테이너 |
| 캐시 | ElastiCache 노드 | Redis 컨테이너 |
| 컴퓨팅 | Fargate 태스크 최소 4개 (bff, account, stock A/B) 상시 실행 | EC2 1대에 전부 |
| 기타 | Secrets Manager, CloudWatch 알람, Route 53, CloudFront | .env, Grafana, 가비아 DNS |

- 개인 포트폴리오 서비스라 트래픽이 적어서, 이 고정비를 낼 만큼의 부하가 없습니다.
- 증권사 실시간 시세 API는 장 시간 내내 WebSocket을 유지하므로, 서버를 필요할 때만 켜는 방식으로도 비용을 줄이기 어렵습니다.
- 그래서 실제 서비스는 EC2 한 대에 모든 구성 요소를 컨테이너로 올려 **고정비를 인스턴스 1대 비용으로** 줄였습니다.

### 설계는 Terraform: 확장 가능한 구조를 증명

EC2 한 대 구조의 한계는 분명합니다.

| 한계 | 목표 아키텍처에서의 해결 |
| --- | --- |
| 인스턴스 장애 시 전체 중단 (단일 장애점) | 2 AZ 배치, RDS Multi-AZ |
| 부하가 늘어도 서버를 늘릴 수 없음 | bff, account 오토스케일링, stock은 그룹 추가로 확장 |
| DB 백업·장애 조치를 직접 관리 | RDS 자동 백업, 장애 조치 |
| 시크릿이 서버의 `.env`에 있음 | Secrets Manager, 서비스별 최소 권한 |
| 인프라를 손으로 구성 | Terraform으로 재현 가능 |

- 서비스가 커졌을 때 옮겨 갈 구조를 코드로 먼저 만들어 두고, 실제 AWS에 적용해 동작을 확인한 뒤 내렸습니다.
- 애플리케이션은 처음부터 이 구조를 전제로 만들었습니다. 서비스 분리, Redis Streams 컨슈머 그룹, stock 그룹 분할, 서비스별 DB, 환경변수 기반 설정이 그 예입니다. 그래서 **EC2에서 Terraform 구조로 옮길 때 코드 변경이 필요 없습니다.**

### 두 구조에 공통으로 적용한 원칙

- **stock-server는 장 운영 시간에 재시작하지 않는다.** EC2에서는 배포 워크플로에서 시간을 막고, ECS에서는 같은 그룹 태스크가 겹치지 않게 배포 설정을 잡았습니다.
- **`/actuator`는 외부에 공개하지 않는다.** EC2에서는 nginx가 `/api`, `/uploads`, WebSocket 경로만 전달하고, ALB에서는 리스너 규칙으로 404를 반환합니다.
- **키 없는 배포.** GitHub Actions는 두 구조 모두 OIDC로 AWS 역할을 받아 배포합니다.
