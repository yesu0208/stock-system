# VPC 3계층 구성
# public: ALB, NAT Gateway
# private-app: ECS 태스크
# private-data: RDS, ElastiCache

locals {
  az_count = length(var.availability_zones)

  # NAT Gateway 개수: 비용 절감 모드면 1개, 아니면 AZ 마다 1개
  nat_count = var.single_nat_gateway ? 1 : local.az_count
}

resource "aws_vpc" "this" {
  cidr_block           = var.vpc_cidr
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = { Name = "${var.name}-vpc" }
}

resource "aws_internet_gateway" "this" {
  vpc_id = aws_vpc.this.id

  tags = { Name = "${var.name}-igw" }
}

# Subnets
resource "aws_subnet" "public" {
  count = local.az_count

  vpc_id                  = aws_vpc.this.id
  availability_zone       = var.availability_zones[count.index]
  cidr_block              = cidrsubnet(var.vpc_cidr, 8, count.index)
  map_public_ip_on_launch = true

  tags = { Name = "${var.name}-public-${var.availability_zones[count.index]}", Tier = "public" }
}

resource "aws_subnet" "private_app" {
  count = local.az_count

  vpc_id            = aws_vpc.this.id
  availability_zone = var.availability_zones[count.index]
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 10 + count.index)

  tags = { Name = "${var.name}-app-${var.availability_zones[count.index]}", Tier = "private-app" }
}

resource "aws_subnet" "private_data" {
  count = local.az_count

  vpc_id            = aws_vpc.this.id
  availability_zone = var.availability_zones[count.index]
  cidr_block        = cidrsubnet(var.vpc_cidr, 8, 20 + count.index)

  tags = { Name = "${var.name}-data-${var.availability_zones[count.index]}", Tier = "private-data" }
}

# NAT Gateway
resource "aws_eip" "nat" {
  count  = local.nat_count
  domain = "vpc"

  tags = { Name = "${var.name}-nat-eip-${count.index}" }
}

resource "aws_nat_gateway" "this" {
  count = local.nat_count

  allocation_id = aws_eip.nat[count.index].id
  subnet_id     = aws_subnet.public[count.index].id

  tags = { Name = "${var.name}-nat-${count.index}" }

  depends_on = [aws_internet_gateway.this]
}

# Route Tables
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.this.id
  }

  tags = { Name = "${var.name}-public-rt" }
}

resource "aws_route_table_association" "public" {
  count = local.az_count

  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

# 앱 서브넷: 외부 API(실시간 주가 정보, Naver, Slack) 호출을 위해 NAT 경유
resource "aws_route_table" "private_app" {
  count  = local.az_count
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this[var.single_nat_gateway ? 0 : count.index].id
  }

  tags = { Name = "${var.name}-app-rt-${count.index}" }
}

resource "aws_route_table_association" "private_app" {
  count = local.az_count

  subnet_id      = aws_subnet.private_app[count.index].id
  route_table_id = aws_route_table.private_app[count.index].id
}

# 데이터 서브넷: 인터넷 경로 없음
resource "aws_route_table" "private_data" {
  vpc_id = aws_vpc.this.id

  tags = { Name = "${var.name}-data-rt" }
}

resource "aws_route_table_association" "private_data" {
  count = local.az_count

  subnet_id      = aws_subnet.private_data[count.index].id
  route_table_id = aws_route_table.private_data.id
}

# VPC Endpoints
# ECR 이미지 pull, 로그 전송, 시크릿 조회를 NAT 없이 처리해 비용과 외부 노출을 줄인다.
resource "aws_vpc_endpoint" "s3" {
  vpc_id            = aws_vpc.this.id
  service_name      = "com.amazonaws.${var.region}.s3"
  vpc_endpoint_type = "Gateway"
  route_table_ids   = aws_route_table.private_app[*].id

  tags = { Name = "${var.name}-vpce-s3" }
}

resource "aws_security_group" "vpc_endpoints" {
  name        = "${var.name}-vpce-sg"
  description = "Interface VPC endpoints"
  vpc_id      = aws_vpc.this.id

  tags = { Name = "${var.name}-vpce-sg" }
}

resource "aws_vpc_security_group_ingress_rule" "vpc_endpoints_https" {
  security_group_id = aws_security_group.vpc_endpoints.id
  description       = "HTTPS from VPC"
  cidr_ipv4         = var.vpc_cidr
  ip_protocol       = "tcp"
  from_port         = 443
  to_port           = 443
}

resource "aws_vpc_endpoint" "interface" {
  for_each = var.enable_interface_endpoints ? toset(["ecr.api", "ecr.dkr", "logs", "secretsmanager"]) : toset([])

  vpc_id              = aws_vpc.this.id
  service_name        = "com.amazonaws.${var.region}.${each.value}"
  vpc_endpoint_type   = "Interface"
  subnet_ids          = aws_subnet.private_app[*].id
  security_group_ids  = [aws_security_group.vpc_endpoints.id]
  private_dns_enabled = true

  tags = { Name = "${var.name}-vpce-${each.value}" }
}
