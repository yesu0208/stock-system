#!/bin/bash
# MySQL 최초 기동 시 1회 실행 (볼륨이 비어 있을 때만)
# 서비스별 DB 를 만들고, 앱 계정(MYSQL_USER)에 세 DB 권한을 부여함.
set -e

mysql -uroot -p"$MYSQL_ROOT_PASSWORD" <<SQL
CREATE DATABASE IF NOT EXISTS bff_db     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS stock_db   CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS account_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON bff_db.*     TO '$MYSQL_USER'@'%';
GRANT ALL PRIVILEGES ON stock_db.*   TO '$MYSQL_USER'@'%';
GRANT ALL PRIVILEGES ON account_db.* TO '$MYSQL_USER'@'%';
FLUSH PRIVILEGES;
SQL
