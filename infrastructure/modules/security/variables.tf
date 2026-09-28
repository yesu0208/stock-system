variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "vpc_id" {
  description = "보안 그룹을 만들 VPC"
  type        = string
}

variable "ports" {
  description = "서비스별 컨테이너 포트"
  type = object({
    bff     = number
    stock   = number
    account = number
  })
}
