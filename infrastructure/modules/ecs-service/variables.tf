variable "name" {
  description = "리소스 이름 접두사 (예: stock-system-prod)"
  type        = string
}

variable "service_name" {
  description = "ECS 서비스/컨테이너 이름 (예: bff-server, stock-server-a)"
  type        = string
}

# 클러스터
variable "cluster_id" {
  type = string
}

variable "cluster_name" {
  type = string
}

variable "namespace_arn" {
  description = "Service Connect 네임스페이스"
  type        = string
}

variable "discovery_name" {
  description = "다른 서비스가 호출할 이름 (null 이면 client 전용)"
  type        = string
  default     = null
}

# 컨테이너
variable "image" {
  description = "ECR 저장소 URL (태그 제외)"
  type        = string
}

variable "image_tag" {
  description = "최초 생성 시 사용할 이미지 태그 (이후 배포는 CI 가 담당)"
  type        = string
}

variable "container_port" {
  type = number
}

variable "cpu" {
  description = "태스크 vCPU 단위 (1024 = 1 vCPU)"
  type        = number
}

variable "memory" {
  description = "태스크 메모리 (MiB)"
  type        = number
}

variable "cpu_architecture" {
  description = "X86_64 또는 ARM64 (이미지 빌드 플랫폼과 일치해야 함)"
  type        = string
  default     = "X86_64"
}

variable "environment" {
  description = "평문 환경변수"
  type        = map(string)
  default     = {}
}

variable "secrets" {
  description = "환경변수 이름 → Secrets Manager valueFrom (arn 또는 arn:키::)"
  type        = map(string)
  default     = {}
}

variable "secret_arns" {
  description = "실행 역할이 읽을 수 있는 시크릿 ARN 목록"
  type        = list(string)
  default     = []
}

variable "task_role_policy_json" {
  description = "애플리케이션에 추가로 부여할 IAM 정책 (JSON)"
  type        = string
  default     = null
}

variable "stop_timeout" {
  description = "SIGTERM 후 강제 종료까지 대기 시간 (초, 최대 120)"
  type        = number
  default     = 60
}

variable "log_retention_days" {
  type    = number
  default = 30
}

# 서비스
variable "desired_count" {
  description = "최초 태스크 수 (오토스케일링 사용 시 min_capacity 와 맞춤)"
  type        = number
}

variable "subnet_ids" {
  description = "태스크 서브넷 (private-app)"
  type        = list(string)
}

variable "security_group_ids" {
  type = list(string)
}

variable "target_group_arn" {
  description = "ALB 대상 그룹 (외부 노출 서비스만)"
  type        = string
  default     = null
}

variable "health_check_grace_period" {
  description = "ALB 헬스 체크 유예 시간 (Spring 기동 시간 고려)"
  type        = number
  default     = 120
}

variable "deployment_minimum_healthy_percent" {
  description = "배포 중 유지할 최소 정상 태스크 비율"
  type        = number
  default     = 100
}

variable "deployment_maximum_percent" {
  description = "배포 중 허용할 최대 태스크 비율"
  type        = number
  default     = 200
}

# 오토스케일링
variable "autoscaling" {
  description = "null 이면 오토스케일링 사용 안 함 (stock-server)"
  type = object({
    min_capacity        = number
    max_capacity        = number
    cpu_target          = number
    requests_per_target = optional(number, 1000)
  })
  default = null
}

variable "request_count_resource_label" {
  description = "ALBRequestCountPerTarget 지표 라벨 (<alb arn suffix>/<target group arn suffix>)"
  type        = string
  default     = null
}
