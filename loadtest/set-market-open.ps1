# 실행: powershell -ExecutionPolicy Bypass -File loadtest\set-market-open.ps1
# 부하 테스트 전 준비: 테스트 종목의 장 상태를 OPEN 으로 고정 (로컬 전용)
# bff 는 Redis 의 market:phase:snapshot 을 기동 시 / 5분마다 읽으므로, 값을 넣고 bff 를 재시작한다.
# 주의: 로컬 stock-server 의 장 마감 스케줄(평일 15:40, 20:05)이 돌면 다시 CLOSED 가 될 수 있음 → 테스트 직전에 실행
param(
    [string[]]$StockCodes = @("005930", "000660", "373220", "000810", "032830", "086790")
)

$compose = @("-f", "docker-compose.yml", "-f", "docker-compose.monitoring.yml")

foreach ($code in $StockCodes) {
    docker compose @compose exec -T redis redis-cli HSET market:phase:snapshot $code OPEN | Out-Null
}
docker compose @compose exec -T redis redis-cli HGETALL market:phase:snapshot

docker compose @compose restart bff-server
Write-Host "bff-server restarted. Wait about 1 minute before running the test."
