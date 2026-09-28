output "service_name" {
  value = aws_ecs_service.this.name
}

output "task_definition_family" {
  description = "CI 에서 최신 리비전을 조회할 때 사용"
  value       = aws_ecs_task_definition.this.family
}

output "container_name" {
  value = local.container_name
}

output "execution_role_arn" {
  value = aws_iam_role.execution.arn
}

output "task_role_arn" {
  value = aws_iam_role.task.arn
}

output "log_group_name" {
  value = aws_cloudwatch_log_group.this.name
}
