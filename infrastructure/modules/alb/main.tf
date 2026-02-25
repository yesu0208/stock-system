resource "aws_lb" "this" {
  name               = "${var.environment}-alb"
  internal           = false
  load_balancer_type = "application"
  subnets            = var.public_subnets
}

output "alb_dns" {
  description = "The DNS name of the ALB"
  value       = aws_lb.this.dns_name
}