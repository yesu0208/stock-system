output "db_secret_arns" {
  description = "서비스 이름 → DB 계정 시크릿 ARN"
  value       = { for k, s in aws_secretsmanager_secret.db : k => s.arn }
}

output "app_secret_arns" {
  description = "시크릿 이름 → 애플리케이션 시크릿 ARN"
  value       = { for k, s in aws_secretsmanager_secret.app : k => s.arn }
}

output "all_secret_arns" {
  value = concat(
    [for s in aws_secretsmanager_secret.db : s.arn],
    [for s in aws_secretsmanager_secret.app : s.arn],
  )
}
