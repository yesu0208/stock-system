output "endpoint" {
  description = "host:port"
  value       = aws_db_instance.this.endpoint
}

output "address" {
  description = "호스트 이름"
  value       = aws_db_instance.this.address
}

output "port" {
  value = aws_db_instance.this.port
}

output "master_user_secret_arn" {
  description = "RDS 가 관리하는 마스터 계정 시크릿 ARN"
  value       = aws_db_instance.this.master_user_secret[0].secret_arn
}
