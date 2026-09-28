variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "region" {
  description = "VPC endpoint 서비스 이름에 사용할 리전"
  type        = string
}

variable "vpc_cidr" {
  description = "VPC CIDR (/16 권장, 서브넷은 /24 로 분할)"
  type        = string
}

variable "availability_zones" {
  description = "사용할 AZ 목록 (2개 이상)"
  type        = list(string)

  validation {
    condition     = length(var.availability_zones) >= 2
    error_message = "고가용성을 위해 AZ 는 2개 이상이어야 합니다."
  }
}

variable "single_nat_gateway" {
  description = "true 면 NAT Gateway 1개만 사용 (비용 절감), false 면 AZ 마다 1개 (운영 권장)"
  type        = bool
  default     = false
}

variable "enable_interface_endpoints" {
  description = "ECR/Logs/Secrets Manager Interface VPC endpoint 생성 여부"
  type        = bool
  default     = true
}
