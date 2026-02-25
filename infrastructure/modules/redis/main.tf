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
  security_group_ids = [aws_security_group.redis.id]
}

# Redis Security Group
resource "aws_security_group" "redis" {
  name   = "${var.environment}-redis-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 6379
    to_port         = 6379
    protocol        = "tcp"
    security_groups = [var.ecs_security_group_id]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }
}

output "endpoint" {
  value = aws_elasticache_cluster.this.cache_nodes[0].address
}