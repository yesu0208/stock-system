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
  availability_zone = var.availability_zone
  tags = {
    Name = "${var.environment}-public-${each.key}"
  }
}

resource "aws_subnet" "private" {
  for_each = toset(var.private_subnet_cidr)
  vpc_id   = aws_vpc.this.id
  cidr_block = each.key
  availability_zone = var.availability_zone
  tags = {
    Name = "${var.environment}-private-${each.key}"
  }
}

# NAT Elastic IP
resource "aws_eip" "nat" {
  domain = "vpc"
}

# NAT GateWay
resource "aws_nat_gateway" "this" {
  allocation_id = aws_eip.nat.id
  subnet_id     = values(aws_subnet.public)[0].id

  tags = {
    Name = "${var.environment}-nat"
  }

  depends_on = [aws_internet_gateway.igw]
}

# Public Route Table
resource "aws_route_table" "public" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.igw.id
  }

  tags = {
    Name = "${var.environment}-public-rt"
  }
}

# Private Route Table
resource "aws_route_table" "private" {
  vpc_id = aws_vpc.this.id

  route {
    cidr_block     = "0.0.0.0/0"
    nat_gateway_id = aws_nat_gateway.this.id
  }

  tags = {
    Name = "${var.environment}-private-rt"
  }
}

# Route Table Association(public)
resource "aws_route_table_association" "public" {
  for_each = aws_subnet.public

  subnet_id      = each.value.id
  route_table_id = aws_route_table.public.id
}

# Route Table Association(private)
resource "aws_route_table_association" "private" {
  for_each = aws_subnet.private

  subnet_id      = each.value.id
  route_table_id = aws_route_table.private.id
}