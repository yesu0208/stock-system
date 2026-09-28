variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "subnet_ids" {
  description = "DB 서브넷 (private-data)"
  type        = list(string)
}

variable "security_group_id" {
  description = "RDS 보안 그룹"
  type        = string
}

variable "engine_version" {
  description = "MySQL 엔진 버전"
  type        = string
  default     = "8.0"
}

variable "instance_class" {
  description = "인스턴스 타입"
  type        = string
  default     = "db.t4g.medium"
}

variable "allocated_storage" {
  description = "초기 스토리지 (GB)"
  type        = number
  default     = 50
}

variable "max_allocated_storage" {
  description = "스토리지 자동 확장 상한 (GB)"
  type        = number
  default     = 200
}

variable "master_username" {
  description = "마스터 계정 이름 (서비스는 전용 계정 사용)"
  type        = string
  default     = "admin"
}

variable "multi_az" {
  description = "Multi-AZ 대기 인스턴스 사용 여부"
  type        = bool
  default     = true
}

variable "backup_retention_days" {
  description = "자동 백업 보관 기간 (일)"
  type        = number
  default     = 7
}

variable "performance_insights_enabled" {
  description = "Performance Insights 사용 여부"
  type        = bool
  default     = false
}

variable "deletion_protection" {
  description = "삭제 방지"
  type        = bool
  default     = true
}

variable "skip_final_snapshot" {
  description = "삭제 시 최종 스냅샷 생략 여부 (테스트 환경 정리용)"
  type        = bool
  default     = false
}
