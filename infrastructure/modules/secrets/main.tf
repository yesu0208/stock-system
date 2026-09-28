# Secrets Manager
# 1) 서비스 DB 계정: 비밀번호를 Terraform 이 생성해 저장 (서비스마다 별도 계정)
# 2) 외부 연동/애플리케이션 키: 자리만 만들고 실제 값은 콘솔/CLI 로 입력
#    -> 값이 Terraform 코드·변수에 남지 않도록 ignore_changes 처리

terraform {
  required_providers {
    random = {
      source = "hashicorp/random"
    }
  }
}

# DB 계정
resource "random_password" "db" {
  for_each = toset(var.db_services)

  length  = 32
  special = true
  # JDBC URL·쉘에서 문제를 일으키지 않는 특수문자만 사용
  override_special = "!#%^*-_=+"
}

resource "aws_secretsmanager_secret" "db" {
  for_each = toset(var.db_services)

  name                    = "${var.name}/db/${each.value}"
  description             = "${each.value} 전용 MySQL 계정"
  recovery_window_in_days = var.recovery_window_in_days
}

resource "aws_secretsmanager_secret_version" "db" {
  for_each = toset(var.db_services)

  secret_id = aws_secretsmanager_secret.db[each.value].id
  secret_string = jsonencode({
    username = "${each.value}_app"
    password = random_password.db[each.value].result
    database = "${each.value}_db"
  })
}

# 애플리케이션 키
resource "aws_secretsmanager_secret" "app" {
  for_each = var.app_secret_keys

  name                    = "${var.name}/app/${each.key}"
  description             = "${each.key} 애플리케이션 시크릿 (값은 수동 입력)"
  recovery_window_in_days = var.recovery_window_in_days
}

resource "aws_secretsmanager_secret_version" "app" {
  for_each = var.app_secret_keys

  secret_id     = aws_secretsmanager_secret.app[each.key].id
  secret_string = jsonencode({ for k in each.value : k => "CHANGE_ME" })

  # 실제 값은 콘솔/CLI 로 입력하므로 이후 변경은 Terraform 이 덮어쓰지 않음
  lifecycle {
    ignore_changes = [secret_string]
  }
}
