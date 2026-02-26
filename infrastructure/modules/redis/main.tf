resource "aws_elasticache_subnet_group" "this" {
  name = "${var.environment}-redis-subnet"
  subnet_ids = var.subnet_ids
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id = "${var.environment}-redis"
  description = "Redis replication group"
  engine = "redis"
  engine_version = "6.x"
  node_type = "cache.t4g.micro"

  automatic_failover_enabled = false
  multi_az_enabled = false
  replicas_per_node_group = 0

  subnet_group_name = aws_elasticache_subnet_group.this.name
  security_group_ids = [var.redis_sg_id]
  port = 6379
}

output "endpoint" {
  value = aws_elasticache_replication_group.this.primary_endpoint_address
}