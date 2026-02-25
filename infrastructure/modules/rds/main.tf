resource "aws_db_subnet_group" "this" {
  name       = "${var.environment}-db-subnet"
  subnet_ids = var.private_subnets
}

resource "aws_db_instance" "this" {
  identifier = "${var.environment}-rds"
  engine     = "mysql"
  instance_class = "db.t3.micro"
  username   = var.db_username
  password   = var.db_password
  allocated_storage = 20
  db_subnet_group_name = aws_db_subnet_group.this.name
  skip_final_snapshot = true
}

# RDS Security Group
resource "aws_security_group" "rds" {
  name   = "${var.environment}-rds-sg"
  vpc_id = var.vpc_id

  ingress {
    from_port       = 3306
    to_port         = 3306
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
  value = aws_db_instance.this.endpoint
}