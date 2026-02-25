resource "aws_ecs_cluster" "this" {
  name = "${var.environment}-ecs"
}

output "service_arn" {
  value = aws_ecs_cluster.this.arn
}