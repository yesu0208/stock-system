variable "name" {
  description = "리소스 이름 접두사"
  type        = string
}

variable "subnet_ids" {
  description = "Redis 서브넷 (private-data)"
  type        = list(string)
}

variable "security_group_id" {
  description = "Redis 보안 그룹"
  type        = string
}

variable "engine_version" {
  description = "Redis 엔진 버전"
  type        = string
  default     = "7.1"
}

variable "parameter_group_name" {
  description = "파라미터 그룹 (cluster mode 비활성화)"
  type        = string
  default     = "default.redis7"
}

variable "node_type" {
  description = "노드 타입"
  type        = string
  default     = "cache.t4g.small"
}

variable "num_cache_clusters" {
  description = "노드 수 (primary 포함, 2 이상이면 자동 장애조치)"
  type        = number
  default     = 2
}

variable "transit_encryption_enabled" {
  description = "전송 구간 암호화(TLS) 사용 여부"
  type        = bool
  default     = false
}

variable "snapshot_retention_days" {
  description = "자동 스냅샷 보관 기간 (일)"
  type        = number
  default     = 3
}
