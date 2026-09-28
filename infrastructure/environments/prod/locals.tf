locals {
  name = "${var.project}-${var.environment}"

  # application.yaml 의 server.port 기준 (bff 는 기본값 8080)
  ports = {
    bff     = 8080
    stock   = 8081
    account = 8082
  }

  tags = {
    Project     = var.project
    Environment = var.environment
    ManagedBy   = "terraform"
  }
}
