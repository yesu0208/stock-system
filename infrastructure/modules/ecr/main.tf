resource "aws_ecr_repository" "this" {
  name = "${var.environment}-repo"
}

output "repo_url" {
  value = aws_ecr_repository.this.repository_url
}