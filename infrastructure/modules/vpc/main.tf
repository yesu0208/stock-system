resource "aws_vpc" "this" {
  cidr_block = var.vpc_cidr
  tags = {
    Name = "${var.environment}-vpc"
  }
}

resource "aws_internet_gateway" "igw" {
  vpc_id = aws_vpc.this.id
  tags = {
    Name = "${var.environment}-igw"
  }
}

resource "aws_subnet" "public" {
  for_each = toset(var.public_subnet_cidr)
  vpc_id   = aws_vpc.this.id
  cidr_block = each.key
  map_public_ip_on_launch = true
  availability_zone = "ap-northeast-2a"
  tags = {
    Name = "${var.environment}-public-${each.key}"
  }
}

resource "aws_subnet" "private" {
  for_each = toset(var.private_subnet_cidr)
  vpc_id   = aws_vpc.this.id
  cidr_block = each.key
  availability_zone = "ap-northeast-2a"
  tags = {
    Name = "${var.environment}-private-${each.key}"
  }
}