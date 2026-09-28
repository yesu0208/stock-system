variable "name" {
  description = "리소스 이름 접두사 (버킷 이름에 사용, 전역 유일해야 함)"
  type        = string
}

variable "aliases" {
  description = "CloudFront 에 연결할 도메인 (예: example.com, www.example.com)"
  type        = list(string)
}

variable "certificate_arn" {
  description = "us-east-1 ACM 인증서 ARN"
  type        = string
}

variable "zone_id" {
  description = "Route53 호스팅 영역"
  type        = string
}

variable "price_class" {
  description = "CloudFront 가격 등급 (PriceClass_200 에 한국 포함)"
  type        = string
  default     = "PriceClass_200"
}

variable "force_destroy" {
  description = "객체가 남아 있어도 버킷 삭제 허용 (테스트 환경 정리용)"
  type        = bool
  default     = false
}
