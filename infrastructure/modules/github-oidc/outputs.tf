output "deploy_role_arn" {
  description = "GitHub Actions 배포 워크플로에서 assume 할 역할"
  value       = aws_iam_role.deploy.arn
}

output "terraform_plan_role_arn" {
  description = "GitHub Actions terraform plan 워크플로에서 assume 할 역할"
  value       = aws_iam_role.plan.arn
}
