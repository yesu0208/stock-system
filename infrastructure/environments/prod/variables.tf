variable "project" {
  description = "리소스 이름 접두사로 사용할 프로젝트 이름"
  type        = string
  default     = "stock-system"
}

variable "environment" {
  description = "배포 환경 이름"
  type        = string
  default     = "prod"
}

variable "aws_region" {
  description = "주 리전"
  type        = string
  default     = "ap-northeast-2"
}
