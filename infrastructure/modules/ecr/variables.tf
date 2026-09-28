variable "name" {
  description = "저장소 이름 접두사"
  type        = string
}

variable "repositories" {
  description = "생성할 저장소 목록 (서비스 이름)"
  type        = list(string)
}

variable "image_tag_mutability" {
  description = "이미지 태그 덮어쓰기 허용 여부 (커밋 SHA 태그 사용 시 IMMUTABLE 권장)"
  type        = string
  default     = "IMMUTABLE"
}

variable "keep_image_count" {
  description = "저장소별로 유지할 최근 이미지 수"
  type        = number
  default     = 30
}
