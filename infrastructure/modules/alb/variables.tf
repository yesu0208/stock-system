variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "vpc_id" {
  type = string
}

variable "subnet_ids" {
  description = "ALB 서브넷 (public)"
  type        = list(string)
}

variable "security_group_id" {
  description = "ALB 보안 그룹"
  type        = string
}

variable "certificate_arn" {
  description = "api 도메인 ACM 인증서 (ap-northeast-2)"
  type        = string
}

variable "zone_id" {
  description = "Route53 호스팅 영역"
  type        = string
}

variable "api_domain" {
  description = "API 도메인 (예: api.example.com)"
  type        = string
}

variable "target_port" {
  description = "bff 컨테이너 포트"
  type        = number
}

variable "health_check_path" {
  description = "대상 그룹 헬스 체크 경로"
  type        = string
  default     = "/actuator/health"
}

variable "idle_timeout" {
  description = "연결 유휴 시간 (초) - WebSocket 을 고려해 기본값보다 길게"
  type        = number
  default     = 300
}

variable "deletion_protection" {
  description = "ALB 삭제 방지"
  type        = bool
  default     = true
}
