# EC2 배포 (docker compose)

실제 서비스([stock-system.cloud](https://stock-system.cloud))는 비용 문제로 **EC2 한 대**에 배포되어 있습니다.
AWS 관리형 서비스 기반의 목표 아키텍처는 [`infrastructure/`](../infrastructure)(Terraform)를 참고하세요.
두 방식 모두 같은 이미지 빌드 파일(`backend/Dockerfile.module`)을 사용합니다.

## 구성

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

| 서비스 | DB | 비고 |
|---|---|---|
| bff-server | `bff_db` | 유일하게 호스트 8080 포트 공개 (nginx 전용) |
| account-server | `account_db` | |
| stock-server / stock-server-b | `stock_db` (공유) | 그룹 A / B, 서로 다른 증권사 API 키 사용 |

## 파일

| 파일 | 역할 |
|---|---|
| `docker-compose.yml` | MySQL, Redis, 서버 4개 |
| `.env.example` | 필요한 환경변수 목록 (`.env`는 커밋 금지) |
| `backend/Dockerfile.module` | 서버 모듈별 이미지 빌드 (`MODULE` 빌드 인자) |
| `deploy/mysql/init-db.sh` | MySQL 최초 기동 시 서비스별 DB 생성, 앱 계정 권한 부여 |
| `deploy/nginx/stock-system.conf` | nginx 설정 (`/etc/nginx/sites-available/stock-system`) |

## 스키마 관리

- 서버는 `SPRING_PROFILES_ACTIVE=prod`로 실행되며, 스키마는 **Flyway**가 관리합니다.
    - 마이그레이션: `backend/<서버>/src/main/resources/db/migration/`
    - JPA는 `ddl-auto: validate`로 엔티티와 테이블 일치 여부만 검증합니다 (불일치 시 기동 실패).
- 이력 테이블은 서버별로 분리됩니다: `flyway_schema_history_bff`, `_stock`, `_account`
- stock A/B는 같은 `stock_db`를 사용하며, 동시 기동 시 Flyway 잠금으로 한 번만 적용됩니다.
- **이미 적용된 마이그레이션 파일(`V1__init.sql` 등)은 수정하지 않습니다.** 공백 변경도 체크섬이 달라져 기동에 실패합니다. 스키마 변경은 `V2__...sql`부터 새 파일로 추가합니다.

## 최초 구축

### 1. 준비
- Docker, Docker Compose, nginx, Certbot 설치
- 저장소 클론

### 2. 환경변수
```bash
cp .env.example .env
vi .env
```
- `DB_USER`는 `root`를 사용할 수 없습니다 (MySQL 이미지 제약).
- `REDIS_HOST=redis`, `REDIS_PORT=6379`
- stock A/B는 서로 다른 증권사 API 키를 사용합니다 (`APP_KEY` / `APP_KEY_B`).

### 3. 실행
```bash
docker compose up -d --build
docker compose ps
```
- MySQL 볼륨이 비어 있을 때만 `init-db.sh`가 실행됩니다.
  기존 볼륨을 초기화하려면 `docker compose down -v` (데이터 삭제).

### 4. 확인
```bash
# DB 생성
docker compose logs mysql | grep init-db
# Flyway 적용 (stock A/B 중 한쪽은 "up to date")
docker compose logs | grep -E "Successfully applied|up to date|Started .*Application"
```

### 5. nginx, HTTPS
```bash
sudo cp deploy/nginx/stock-system.conf /etc/nginx/sites-available/stock-system
sudo ln -s /etc/nginx/sites-available/stock-system /etc/nginx/sites-enabled/
sudo certbot --nginx -d stock-system.cloud -d www.stock-system.cloud
sudo nginx -t && sudo systemctl reload nginx
```

### 6. 프론트엔드
```bash
cd frontend
# .env.production: VITE_API_BASE_URL, VITE_WS_BASE_URL (서비스 도메인 기준)
npm ci && npm run build
sudo mkdir -p /var/www/stock-system
sudo rm -rf /var/www/stock-system/* && sudo cp -r dist/* /var/www/stock-system/
```

## 배포

평소 배포는 [자동 배포 (CD)](#자동-배포-cd)를 사용합니다. EC2 에서 직접 빌드하지 않습니다.

- **stock-server는 장 마감 이후에 배포합니다.** 재시작 동안 담당 그룹 종목의 체결이 멈춥니다.
- EC2 에서 서비스를 직접 재시작할 때는 반드시 서비스를 지정하고 `--no-deps` 를 붙입니다.
  서비스 없이 `docker compose up -d` 를 실행하거나 `--no-deps` 를 빼면, 의존 관계(`depends_on`)에 있는 stock-server 까지 재생성될 수 있습니다.
- EC2 에서는 커밋하지 않습니다. `git pull --ff-only` 가 실패하면 `git reset --hard origin/main` 으로 맞춥니다 (`.env` 는 영향 없음).

### nginx 설정 변경
저장소의 `deploy/nginx/stock-system.conf`를 수정한 뒤 서버에 반영합니다.
```bash
sudo cp deploy/nginx/stock-system.conf /etc/nginx/sites-available/stock-system
sudo nginx -t && sudo systemctl reload nginx
```

## 운영 명령

```bash
docker compose ps                          # 상태
docker compose logs -f --tail 100 <서비스>  # 로그
docker compose restart <서비스>             # 재시작 (데이터 유지)
docker compose exec mysql mysql -uroot -p   # DB 접속
```

## 문제 해결

| 증상 | 원인 / 해결 |
|---|---|
| `Access denied for user`, `Unknown database` | `init-db.sh`가 실행되지 않음 (기존 볼륨). `docker compose down -v` 후 재기동 |
| MySQL 기동 실패 (`MYSQL_USER` 관련) | `.env`의 `DB_USER`가 `root` |
| `Schema-validation` 에러로 서버 기동 실패 | 엔티티 변경 후 마이그레이션 누락. `V<n>__...sql` 추가 |
| `Migration checksum mismatch` | 적용된 마이그레이션 파일이 수정됨. 원래 내용으로 되돌림 |
| 프로필 이미지 업로드 `413` | nginx `client_max_body_size` 확인 (5MB) |
| `init-db.sh: bad interpreter` | CRLF 줄바꿈. `.gitattributes`로 LF 고정됨 (`sed -i 's/\r$//'`) |


## 자동 배포 (CD)

main 에 머지되면 GitHub Actions 가 이미지를 빌드해 GHCR 에 올리고, AWS SSM 으로 EC2 에 배포합니다.
EC2 에 SSH 로 접속할 필요가 없습니다.

| 워크플로 | 대상 | 실행 시점 |
|---|---|---|
| `deploy.yaml` | bff-server, account-server, frontend | main 머지 시 (변경된 것만), 수동 실행 |
| `deploy-stock.yaml` | stock-server (A/B) | 평일 20:30 KST 자동, 수동 실행 |

- stock-server 이미지는 `deploy.yaml` 에서 빌드만 해 두고, 장 마감 후 `deploy-stock.yaml` 이 배포합니다.
- `backend/common`, Gradle 설정, `Dockerfile.module` 이 바뀌면 모든 서버 이미지를 다시 빌드합니다.
- bff-server, account-server 는 재시작하는 동안 1~2분 요청이 실패할 수 있습니다.
- 두 워크플로는 동시에 실행되지 않습니다 (`concurrency: deploy-ec2`).

### 사전 준비 (최초 1회)

**GitHub 설정** (Settings → Secrets and variables → Actions)

| 종류 | 이름 | 예시 |
|---|---|---|
| Secret | `AWS_DEPLOY_ROLE_ARN` | GitHub OIDC 로 맡을 IAM 역할 ARN (`ssm:SendCommand`, `ssm:GetCommandInvocation`) |
| Variable | `AWS_REGION` | `ap-northeast-2` |
| Variable | `EC2_INSTANCE_ID` | `i-0123456789abcdef0` |
| Variable | `EC2_APP_DIR` | `/home/ubuntu/stock-system` |
| Variable | `EC2_USER` | `ubuntu` |

**EC2 설정**

- SSM Agent 실행 중, 인스턴스 역할에 `AmazonSSMManagedInstanceCore` 연결 (Systems Manager → Fleet Manager 에서 Online 확인)
- `ubuntu` 사용자로 GHCR 로그인 (`read:packages` 권한 토큰)

```bash
  read -s GHCR_TOKEN
  echo "$GHCR_TOKEN" | docker login ghcr.io -u <GitHub 사용자명> --password-stdin
  unset GHCR_TOKEN
```

- `.env` 에 `IMAGE_TAG` 를 넣지 않습니다 (넣으면 자동 배포가 항상 그 태그를 받습니다).

### 수동 배포

Actions 탭 → 워크플로 선택 → **Run workflow**

- `Deploy`: `targets` 에 `all` 또는 `bff-server,frontend` 처럼 쉼표로 입력
- `Deploy Stock`: 평일 08:40~20:15 KST 에는 차단됩니다. 꼭 필요하면 `force` 를 선택합니다 (체결, 시세가 중단됩니다).

### 결과 확인

- Actions 실행 화면의 **Summary** 에 배포 대상이 표시됩니다.
- `SSM 으로 배포 실행` 단계의 **EC2 출력** 그룹에 EC2 에서 실행된 로그가 나옵니다.
- EC2 에서 확인할 때:

```bash
  cd ~/stock-system
  docker compose ps
  docker compose logs --tail 100 bff-server
```

### 되돌리기 (롤백)

이미지는 `latest` 와 커밋 SHA 두 태그로 올라갑니다. 이전 커밋의 SHA(40자리) 로 되돌립니다.

```bash
cd ~/stock-system
IMAGE_TAG=<이전 커밋 SHA> docker compose pull bff-server
IMAGE_TAG=<이전 커밋 SHA> docker compose up -d --no-build --no-deps bff-server
```

- `IMAGE_TAG` 는 명령 앞에만 붙이고 `.env` 에는 넣지 않습니다.
- 다음 자동 배포 때 다시 `latest` 로 돌아가므로, 원인 수정 커밋을 머지해 복구합니다.
- stock-server 는 **장 운영 시간(평일 08:40~20:15 KST)을 피해서** 되돌립니다. A → B 순서로 하나씩 합니다.

### 비상 시: EC2 에서 직접 빌드

GitHub Actions 나 GHCR 을 쓸 수 없을 때만 사용합니다.

```bash
cd ~/stock-system
git pull --ff-only
docker compose up -d --build --no-deps bff-server
```

- EC2 에서 빌드하면 메모리를 많이 쓰므로 한 서비스씩 빌드합니다.
- 복구된 뒤에는 Actions 에서 `Deploy` 를 수동 실행해 GHCR 이미지로 되돌립니다.

### 프론트엔드

`frontend/` 가 바뀌면 EC2 에서 `npm ci && npm run build` 후 `dist/` 를 `/var/www/stock-system` 로 복사합니다.
빌드에 쓰는 `frontend/.env.production` 은 EC2 에만 두고 커밋하지 않습니다.