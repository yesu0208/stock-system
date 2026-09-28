variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "create_oidc_provider" {
  description = "계정에 GitHub OIDC 공급자가 없으면 true"
  type        = bool
  default     = true
}

variable "github_repository" {
  description = "owner/repo 형식의 GitHub 저장소"
  type        = string
}

variable "deploy_branch" {
  description = "배포를 허용할 브랜치"
  type        = string
  default     = "main"
}

variable "ecr_repository_arns" {
  type = list(string)
}

variable "ecs_role_arns" {
  description = "새 태스크 정의 등록 시 전달할 실행/태스크 역할"
  type        = list(string)
}

variable "web_bucket_arn" {
  type = string
}

variable "cloudfront_distribution_arn" {
  type = string
}

variable "state_bucket_name" {
  description = "terraform 상태 버킷"
  type        = string
}

variable "secret_arns" {
  description = "plan 이 refresh 할 Terraform 관리 시크릿"
  type        = list(string)
}
