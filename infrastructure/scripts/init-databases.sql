-- 서비스별 데이터베이스와 전용 계정 생성 (최초 1회, 마스터 계정으로 실행)
--
-- 1) 마스터 계정 비밀번호: Secrets Manager 의 RDS 관리 시크릿 (terraform output rds_master_user_secret_arn)
-- 2) 서비스 계정 비밀번호: Secrets Manager 의 <name>/db/{bff,stock,account} 시크릿의 password 값
-- 3) 아래 <..._PASSWORD> 를 실제 값으로 바꿔서 VPC 내부(ECS Exec, 배스천 등)에서 실행
--
-- 서비스끼리 같은 테이블을 보지 않으므로 계정마다 자기 데이터베이스 권한만 부여한다.
-- stock 그룹(A, B …)이 늘어나도 모두 stock_db 를 함께 사용한다.

CREATE DATABASE IF NOT EXISTS bff_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS stock_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS account_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'bff_app'@'%'     IDENTIFIED BY '<BFF_PASSWORD>';
CREATE USER IF NOT EXISTS 'stock_app'@'%'   IDENTIFIED BY '<STOCK_PASSWORD>';
CREATE USER IF NOT EXISTS 'account_app'@'%' IDENTIFIED BY '<ACCOUNT_PASSWORD>';

GRANT ALL PRIVILEGES ON bff_db.*     TO 'bff_app'@'%';
GRANT ALL PRIVILEGES ON stock_db.*   TO 'stock_app'@'%';
GRANT ALL PRIVILEGES ON account_db.* TO 'account_app'@'%';

FLUSH PRIVILEGES;
