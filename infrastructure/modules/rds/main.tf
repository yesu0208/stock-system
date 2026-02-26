resource "aws_db_subnet_group" "this" {
  name = "${var.environment}-db-subnet"
  subnet_ids = var.private_subnets
}

resource "aws_db_instance" "this" {
  identifier = "${var.environment}-rds"
  engine = "mysql"
  engine_version = "8.0"
  instance_class = "db.t3.micro"
  username = var.db_username
  password = var.db_password
  allocated_storage = 20
  db_subnet_group_name = aws_db_subnet_group.this.name
  vpc_security_group_ids = [var.rds_sg_id]
  skip_final_snapshot = true
}

output "endpoint" {
  value = aws_db_instance.this.endpoint
}