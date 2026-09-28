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

# 네트워크
variable "vpc_cidr" {
  description = "VPC CIDR"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "사용할 AZ 목록"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "single_nat_gateway" {
  description = "NAT Gateway 1개만 사용할지 여부 (운영 권장: false)"
  type        = bool
  default     = false
}
