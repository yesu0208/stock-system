resource "aws_elasticache_subnet_group" "this" {
  name       = "${var.environment}-redis-subnet"
  subnet_ids = var.subnet_ids
}

resource "aws_elasticache_cluster" "this" {
  cluster_id           = "${var.environment}-redis"
  engine               = "redis"
  node_type            = "cache.t4g.micro"
  num_cache_nodes      = 1
  subnet_group_name    = aws_elasticache_subnet_group.this.name
  parameter_group_name = "default.redis6.x"
}

output "endpoint" {
  value = aws_elasticache_cluster.this.cache_nodes[0].address
}