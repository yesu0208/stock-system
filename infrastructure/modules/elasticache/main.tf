# ElastiCache Redis (primary + replica, 자동 장애조치)
resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.name}-redis-subnet"
  subnet_ids = var.subnet_ids
}

resource "aws_elasticache_replication_group" "this" {
  replication_group_id = "${var.name}-redis"
  description          = "${var.name} Redis (streams, pub/sub, lock)"

  engine               = "redis"
  engine_version       = var.engine_version
  parameter_group_name = var.parameter_group_name
  node_type            = var.node_type
  port                 = 6379

  # primary 1 + replica (num_cache_clusters - 1)
  num_cache_clusters         = var.num_cache_clusters
  automatic_failover_enabled = var.num_cache_clusters > 1
  multi_az_enabled           = var.num_cache_clusters > 1

  subnet_group_name  = aws_elasticache_subnet_group.this.name
  security_group_ids = [var.security_group_id]

  at_rest_encryption_enabled = true
  # 애플리케이션이 현재 평문 연결(host/port)만 사용하므로 기본값 false.
  # TLS 로 바꾸려면 Spring Redis ssl 설정과 함께 true 로 변경
  transit_encryption_enabled = var.transit_encryption_enabled

  snapshot_retention_limit = var.snapshot_retention_days
  snapshot_window          = "17:00-18:00" # KST 02:00-03:00
  maintenance_window       = "sun:20:00-sun:21:00"

  auto_minor_version_upgrade = true
  apply_immediately          = false

  tags = { Name = "${var.name}-redis" }
}
