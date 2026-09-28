# 계층별 보안 그룹
# 인터넷 -> ALB -> bff
# bff, stock -> account (내부 REST)
# bff -> stock (내역/휴장일 조회 REST)
# bff, stock, account -> RDS, Redis

locals {
  app_sgs = {
    bff     = aws_security_group.bff.id
    stock   = aws_security_group.stock.id
    account = aws_security_group.account.id
  }
}

# ALB
resource "aws_security_group" "alb" {
  name        = "${var.name}-alb-sg"
  description = "Public ALB"
  vpc_id      = var.vpc_id

  tags = { Name = "${var.name}-alb-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "alb_http" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTP (HTTPS redirect)"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 80
  to_port           = 80
}

resource "aws_vpc_security_group_ingress_rule" "alb_https" {
  security_group_id = aws_security_group.alb.id
  description       = "HTTPS"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_security_group_egress_rule" "alb_to_bff" {
  security_group_id            = aws_security_group.alb.id
  description                  = "To bff tasks"
  referenced_security_group_id = aws_security_group.bff.id
  ip_protocol                  = "tcp"
  from_port                    = var.ports.bff
  to_port                      = var.ports.bff
}

# App (ECS tasks)
resource "aws_security_group" "bff" {
  name        = "${var.name}-bff-sg"
  description = "bff-server tasks"
  vpc_id      = var.vpc_id

  tags = { Name = "${var.name}-bff-sg" }
}

resource "aws_security_group" "stock" {
  name        = "${var.name}-stock-sg"
  description = "stock-server tasks"
  vpc_id      = var.vpc_id

  tags = { Name = "${var.name}-stock-sg" }
}

resource "aws_security_group" "account" {
  name        = "${var.name}-account-sg"
  description = "account-server tasks"
  vpc_id      = var.vpc_id

  tags = { Name = "${var.name}-account-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "bff_from_alb" {
  security_group_id            = aws_security_group.bff.id
  description                  = "From ALB"
  referenced_security_group_id = aws_security_group.alb.id
  ip_protocol                  = "tcp"
  from_port                    = var.ports.bff
  to_port                      = var.ports.bff
}

resource "aws_vpc_security_group_ingress_rule" "stock_from_bff" {
  security_group_id            = aws_security_group.stock.id
  description                  = "From bff (history, holiday API)"
  referenced_security_group_id = aws_security_group.bff.id
  ip_protocol                  = "tcp"
  from_port                    = var.ports.stock
  to_port                      = var.ports.stock
}

resource "aws_vpc_security_group_ingress_rule" "account_from_bff" {
  security_group_id            = aws_security_group.account.id
  description                  = "From bff"
  referenced_security_group_id = aws_security_group.bff.id
  ip_protocol                  = "tcp"
  from_port                    = var.ports.account
  to_port                      = var.ports.account
}

resource "aws_vpc_security_group_ingress_rule" "account_from_stock" {
  security_group_id            = aws_security_group.account.id
  description                  = "From stock"
  referenced_security_group_id = aws_security_group.stock.id
  ip_protocol                  = "tcp"
  from_port                    = var.ports.account
  to_port                      = var.ports.account
}

# 앱은 외부 API(NAT), VPC endpoint, 다른 서비스, RDS/Redis 로 나가야 하므로 egress 전체 허용
resource "aws_vpc_security_group_egress_rule" "app_all" {
  for_each = local.app_sgs

  security_group_id = each.value
  description       = "All outbound"
  cidr_ipv4         = "0.0.0.0/0"
  ip_protocol       = "-1"
}

# Data
resource "aws_security_group" "rds" {
  name        = "${var.name}-rds-sg"
  description = "RDS MySQL"
  vpc_id      = var.vpc_id

  tags = { Name = "${var.name}-rds-sg" }
}

resource "aws_security_group" "redis" {
  name        = "${var.name}-redis-sg"
  description = "ElastiCache Redis"
  vpc_id      = var.vpc_id

  tags = { Name = "${var.name}-redis-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "rds_from_app" {
  for_each = local.app_sgs

  security_group_id            = aws_security_group.rds.id
  description                  = "MySQL from ${each.key}"
  referenced_security_group_id = each.value
  ip_protocol                  = "tcp"
  from_port                    = 3306
  to_port                      = 3306
}

resource "aws_vpc_security_group_ingress_rule" "redis_from_app" {
  for_each = local.app_sgs

  security_group_id            = aws_security_group.redis.id
  description                  = "Redis from ${each.key}"
  referenced_security_group_id = each.value
  ip_protocol                  = "tcp"
  from_port                    = 6379
  to_port                      = 6379
}