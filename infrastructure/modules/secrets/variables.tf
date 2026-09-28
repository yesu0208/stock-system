variable "name" {
  description = "시크릿 이름 접두사"
  type        = string
}

variable "db_services" {
  description = "전용 DB 계정을 만들 서비스 목록 (예: bff, stock, account)"
  type        = list(string)
}

variable "app_secret_keys" {
  description = "시크릿 이름 → 포함할 키 목록 (값은 CHANGE_ME 로 생성 후 수동 입력)"
  type        = map(list(string))
}

variable "recovery_window_in_days" {
  description = "삭제 후 복구 가능 기간 (0 이면 즉시 삭제)"
  type        = number
  default     = 7
}
