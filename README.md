# 모의투자 시스템 (Stock-System)

## 개요/로직

실제 시장을 기반으로 하는 모의투자 시스템입니다.
모의투자를 시작하기 위해서 회원가입이 필요하며,
회원가입 이후 10억의 모의투자금이 지급됩니다.

- 설계 및 개발(1차): 2026.01 ~ 2026.02, 1차 배포: 2026.03 
- 운영: 2026.04 ~ 2026.07
- 설계 및 개발(2차, 사용자 피드백 반영): 2026.08 ~ 2026.09, 2차 배포: 2026.09
- [#123](https://github.com/yesu0208/stock-system/issues/123) 57. 모의투자 시스템 사용자 피드백 정리
- 설계부터 구현(풀스택), 배포까지 100% 기여하였습니다.
- 배포 링크: https://stock-system.cloud

다음과 같은 기능을 사용할 수 있습니다.

#### 시세 · 종목 정보
- (40종목) 실시간 호가창, 실시간 시세, 차트 제공
- 종목 요약 정보, 시세 제공
- 투자자별 매매 동향(일별/시간별), 외국인 보유 현황
- 업종별 시세, 인기 종목, 거래 순위
- 종목 뉴스
- 관심 종목(Watchlist)
- 휴장일 정보 제

#### 주문(40종목)
- 주문(매수, 매도): 주문가격, 주문수량 지정
- 내 앞 대기 주문 수: 미체결 주문의 대기열 순번을 실시간으로 표시(호가창에 표시)
- 자동주문(매수, 매도): 주문가격, 감시가격, 주문수량 지정
- 고급주문
  - 트레일링 스탑: 고점(저점) 대비 지정 폭만큼 되돌리면 자동 주문
  - OTOCO: 진입 주문 체결 시 익절(TP)/손절(SL) 주문이 함께 활성화되고, 하나가 체결되면 나머지는 자동 취소
- 가격 알림: 지정한 가격 도달 시 알림
- 주문, 자동주문, 고급주문, 알림 취소: 체결 또는 Trigger(감시가격 도달) 이전에 취소 가능
- 레버리지: 담보비율 계산, 마진콜, 청산, 일일 이자 청구

#### 계좌 · 랭크
- 보유 종목 정보(각 종목의 보유 수량, 수익률, 평가금액 등)
- 계좌 정보(총 수익률, 총 평가금액, 주문 가능 금액 등)
- 주문 · 체결 · 취소 내역
- 일별 수익률 기록
- 랭크: 매일 수익률로 점수를 매겨 BRONZE ~ MASTER 등급 부여, 랭크 변동 이력 제공

#### 커뮤니티
- 종목톡: 종목별 실시간 채팅(WebSocket/STOMP)
- 종목게시판: 글/댓글 작성, 반응, 스크랩, 내가 쓴 글·댓글·스크랩 모아보기
- 공지사항

#### 관리자
- 공지사항 관리, 휴장일 관리, 사용자 관리

#### 거래 비용

| 항목 | 요율 | 적용 |
| --- | --- | --- |
| 수수료 | 0.015% | 매수·매도 체결 금액 (레버리지는 포지션 전체 금액 기준) |
| 거래세 | 0.20% | 매도 체결 금액 |
| 신용 이자 | 연 8.5% (일 8.5% / 365) | 레버리지 대출금, 매일 20:10 계좌 현금에서 차감 (주말·휴장일 포함) |

- 금액은 모두 원 단위 반올림
- 매수 수수료는 주문 시 원금과 함께 예약하고, 체결 시 실제 금액 기준으로 정산
- 매도 수수료·거래세는 체결 대금에서 바로 차감

#### 주문 가능 시간
다음과 같은 시간에 주문과 체결이 이루어집니다.
- 8:50 ~ 9:00: 동시호가(주문 가능, 체결은 동시호가 이후)
- 9:00 ~ 15:20: 주문 가능, 체결 즉시 가능
- 15:20 ~ 15:30: 동시호가(주문 가능, 체결은 동시호가 이후)
- 15:40: 정규장 미체결 주문/자동주문 강제 취소 및 계좌 업데이트 작업
- 16:00 ~ 20:00: 애프터마켓
- 20:05: 애프터마켓 미체결 주문/자동주문 강제 취소 → 20:10 이자 청구 → 20:15 수익률·등급 배치
- 동시호가는 시장 상황에 따라 지연될 수 있습니다.

모의투자 시스템의 상세한 로직은 아래 Issue에서 확인 가능합니다.

- [#19](https://github.com/yesu0208/stock-system/issues/19) 10. 모의투자의 기본적인 로직
- [#68](https://github.com/yesu0208/stock-system/issues/68) 35. 동시호가 체결 로직
- [#171](https://github.com/yesu0208/stock-system/issues/171) 79. 고객 등급제(RP 기반 티어 시스템) 도입
- [#179](https://github.com/yesu0208/stock-system/issues/179) Documentation: Leverage(신용 거래) 시스템 도입
- [#182](https://github.com/yesu0208/stock-system/issues/182) Documentation: 고급 주문(Trailing Stop, OTOCO) 도입

## 성능 개선

k6 부하 테스트(초당 20 → 50 → 100건 주문)와 Prometheus/Grafana 지표로 주문 → 체결 → 정산 경로의 병목을 하나씩 찾아 제거했습니다.

| 구간 | 지표 | 개선 전 | 개선 후 |
| --- | --- | --- | --- |
| 주문 접수 | 최대 대기 | 686s | 1.4s |
| 주문 접수 | 처리량 | 9 → 3건/s (하락) | 유입량 그대로 추종 |
| DB 잠금 | 다른 사용자 계좌 잠금 | 5s 대기 후 Lock timeout | 즉시 |
| DB 조회 | 주문 내역 / Outbox 조회 | 194ms / 201ms | 0.22ms / 0.67ms |
| 체결 | tradeMatching p95 | 8.52s | 0.84s |
| 정산 | 최대 대기 | 214.6s | 0.79s |
| 정합성 | 주문 유실 | 5건 | 0건 |

주요 개선 내용
- 대기열 순번 방송을 O(N) 전체 방송에서 변경 지점부터만 방송하도록 변경
- Redis Streams 컨슈머에 드레인 루프와 파티션(종목/사용자) 병렬 처리 적용
- 인덱스 없는 `FOR UPDATE`가 전체 행을 잠그던 문제를 UNIQUE 인덱스로 해결
- 체결 틱당 대기열 방송을 1회로 줄여 체결 지연 제거
- A/B 서버 간 스트림 레코드 ID 충돌로 인한 주문 유실 버그 수정

측정 방법, 원인 분석, PR별 상세 결과는 [성능 개선 기록](docs/performance.md)에서 확인할 수 있습니다.

## 기술적 도전

| 문제 | 해결 |
| --- | --- |
| DB 저장과 이벤트 발행이 따로 실패하면 서비스 간 데이터가 어긋남 | Outbox 패턴으로 같은 트랜잭션에 저장 후 Redis Streams로 발행, 레코드별 처리 키로 중복 처리 방지 |
| 병렬 처리 시 같은 종목/사용자의 처리 순서가 뒤섞일 위험 | 종목(주문)·사용자(정산)를 파티션 키로 사용해 순서가 필요한 곳만 순차 처리 |
| stock 서버 A/B가 장 마감 정리를 중복 실행할 위험 | 분산 락 + 그룹 코디네이터로 한 인스턴스만 실행하고 모든 그룹 완료 후 마감 신호 발행 |
| 일일 배치 중 한 사용자의 예외가 전체 배치를 롤백 | 랭크·레버리지 배치를 사용자/포지션 단위 트랜잭션으로 분리 |
| 트레일링 스탑 고점·저점을 틱마다 DB에 쓰면 체결 처리가 지연 | 틱 처리에서는 변경 표시만 하고 스케줄러가 마지막 값만 모아 저장 |
| 장 마감 후 들어온 주문이 응답 없이 사라짐 | 주문·자동주문·트레일링 스탑·OTOCO 컨슈머에 MARKET_CLOSED 오류 알림 추가 |
| 장 운영 중 배포 시 체결과 시세가 중단 | GitHub Actions에서 평일 08:40~20:15 배포를 차단하고, 20:30 자동 배포 |
| `/actuator` 엔드포인트 외부 노출 | ALB 리스너 규칙으로 외부 요청에 404 반환 |

## 개발 환경
- Intellij IDEA
- Visual Studio Code

## 기술 세부 스택
### 1. Backend
- JAVA 21
- Gradle 8.14.3
- Spring Boot 4.0.1

#### Database & Cache
- MySQL
- Redis (Cache / Real-time)
- Redis Streams (서비스 간 비동기 메시징, Outbox 패턴)
- Flyway (DB 마이그레이션)

#### ORM
- Spring Data JPA
- Hibernate

#### Security
- Spring Security
- JJWT (JWT Authentication)

#### Real-time Communication
- Spring WebSocket

#### Validation
- Spring Validation (JSR-380)

#### Monitoring
- Spring Boot Actuator
- Prometheus + Grafana (Micrometer)

#### Load Testing
- k6

#### Dev Tools
- Lombok
- Spring Boot DevTools

#### Testing
- JUnit Platform
- Spring Boot Test (JPA, Security, Web, Validation, WebSocket)

### 2. Frontend
- React
- TypeScript
- Vite

아래의 Issue에서 해당 기술스택을 채택하게 된 배경을 확인할 수 있습니다.
- [#80](https://github.com/yesu0208/stock-system/issues/80) 40. FE 초기 세팅(React + Vite + TypeScript), Monorepo 구성

### 3. CI/CD
- Github Actions
- Terraform
- AWS

### 4. Tools
- MySQL Workbench 8.0 CE
- Postman
- Git
- GitKraken
- Docker

## 확장성과 트래픽 분산을 고려한 설계(BFF / Stock(A/B) / Account 서버 분리)
- [#22](https://github.com/yesu0208/stock-system/issues/22) 12. 확장성과 트래픽 분산을 고려한 설계
<img width="962" height="416" alt="화면 캡처 2026-03-25 144143" src="https://github.com/user-attachments/assets/64f69772-1f29-47c1-93a9-8f62e9b8825e" />

- [#31](https://github.com/yesu0208/stock-system/issues/31) 17. 주문 서비스 구현(기본 기능)
<img width="990" height="577" alt="화면 캡처 2026-03-25 144156" src="https://github.com/user-attachments/assets/eae7a038-3483-40c6-a8bb-a6de7bbf7a9f" />


## System Architecture
<img width="937" height="493" alt="화면 캡처 2026-03-25 123106" src="https://github.com/user-attachments/assets/390d585e-785b-4800-ba6c-6b22de1cf43e" />


## AWS 배포 구조 (+Terraform IaC)

다음과 같은 배포구조를 채택하게 된 배경은 아래 Issue에서 확인할 수 있습니다.
- [#111](https://github.com/yesu0208/stock-system/issues/111) 52. AWS(+Terraform IaC) 배포 채택
<img width="1137" height="617" alt="1773816372650" src="https://github.com/user-attachments/assets/7f04a61e-43ff-42b9-b868-2d58890b90c9" />
