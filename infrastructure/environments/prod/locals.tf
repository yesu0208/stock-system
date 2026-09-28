locals {
  name = "${var.project}-${var.environment}"

  # application.yaml 의 server.port 기준 (bff 는 기본값 8080)
  ports = {
    bff     = 8080
    stock   = 8081
    account = 8082
  }

  # stock-server 그룹별 시크릿에 들어갈 키 (application.yaml 환경변수 이름)
  stock_app_secret_keys = [
    "APPROVAL_KEY_URL",
    "APP_KEY",
    "APP_SECRET",
    "WS_URL",
    "CHART_API_URL",
    "CHART_API_APPKEY",
    "CHART_API_APPSECRET",
  ]

  tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
