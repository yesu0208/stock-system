variable "domain_name" {
  description = "구입한 루트 도메인 (예: example.com)"
  type        = string
}

variable "api_subdomain" {
  description = "API(ALB) 서브도메인"
  type        = string
  default     = "api"
}

variable "create_hosted_zone" {
  description = "true 면 Route53 호스팅 영역을 새로 생성 (외부 등록기관 도메인), false 면 기존 영역 사용"
  type        = bool
  default     = true
}
