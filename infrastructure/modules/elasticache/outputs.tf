output "primary_endpoint" {
  description = "쓰기용 primary 엔드포인트 (장애조치 시 자동 전환)"
  value       = aws_elasticache_replication_group.this.primary_endpoint_address
}

output "port" {
  value = aws_elasticache_replication_group.this.port
}
