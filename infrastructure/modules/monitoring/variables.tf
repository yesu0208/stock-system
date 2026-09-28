variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "alarm_emails" {
  description = "알람을 받을 이메일 (구독 확인 메일 승인 필요)"
  type        = list(string)
  default     = []
}

variable "cluster_name" {
  type = string
}

variable "singleton_service_names" {
  description = "태스크가 1개 고정인 서비스 (stock-server 그룹)"
  type        = list(string)
}

variable "alb_arn_suffix" {
  type = string
}

variable "target_group_arn_suffix" {
  type = string
}

variable "alb_5xx_threshold" {
  description = "5분 동안 허용할 대상 5xx 응답 수"
  type        = number
  default     = 20
}

variable "db_instance_identifier" {
  type = string
}
