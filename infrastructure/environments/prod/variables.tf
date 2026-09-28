variable "project" {
  description = "리소스 이름 접두사로 사용할 프로젝트 이름"
  type        = string
  default     = "stock-system"
}

variable "environment" {
  description = "배포 환경 이름"
  type        = string
  default     = "prod"
}

variable "aws_region" {
  description = "주 리전"
  type        = string
  default     = "ap-northeast-2"
}

# 네트워크
variable "vpc_cidr" {
  description = "VPC CIDR"
  type        = string
  default     = "10.0.0.0/16"
}

variable "availability_zones" {
  description = "사용할 AZ 목록"
  type        = list(string)
  default     = ["ap-northeast-2a", "ap-northeast-2c"]
}

variable "single_nat_gateway" {
  description = "NAT Gateway 1개만 사용할지 여부 (운영 권장: false)"
  type        = bool
  default     = false
}

# 도메인
variable "domain_name" {
  description = "구입한 루트 도메인 (예: example.com)"
  type        = string
}

variable "create_hosted_zone" {
  description = "Route53 호스팅 영역을 새로 만들지 여부 (외부 등록기관에서 구입한 도메인이면 true)"
  type        = bool
  default     = true
}

# 데이터
variable "db_instance_class" {
  description = "RDS 인스턴스 타입"
  type        = string
  default     = "db.t4g.medium"
}

variable "db_multi_az" {
  description = "RDS Multi-AZ 사용 여부"
  type        = bool
  default     = true
}

variable "redis_node_type" {
  description = "ElastiCache 노드 타입"
  type        = string
  default     = "cache.t4g.small"
}

# 애플리케이션
variable "image_tag" {
  description = "최초 생성 시 사용할 이미지 태그 (ECR 에 미리 push 되어 있어야 함, 이후 배포는 CI 가 담당)"
  type        = string
}

variable "bff" {
  description = "bff-server 태스크 크기와 오토스케일링 범위"
  type = object({
    cpu          = number
    memory       = number
    min_capacity = number
    max_capacity = number
  })
  default = {
    cpu          = 1024
    memory       = 2048
    min_capacity = 2
    max_capacity = 6
  }
}

variable "account" {
  description = "account-server 태스크 크기와 오토스케일링 범위"
  type = object({
    cpu          = number
    memory       = number
    min_capacity = number
    max_capacity = number
  })
  default = {
    cpu          = 512
    memory       = 1024
    min_capacity = 2
    max_capacity = 4
  }
}

variable "stock" {
  description = "stock-server 태스크 크기 (그룹마다 1개 고정)"
  type = object({
    cpu    = number
    memory = number
  })
  default = {
    cpu    = 1024
    memory = 2048
  }
}

variable "stock_groups" {
  description = "stock-server 그룹 (키 → 서비스 이름 접미사, group → application.yaml 의 server.group)"
  type = map(object({
    group = string
  }))
  default = {
    a = { group = "A" }
  }
}

variable "primary_stock_group_key" {
  description = "bff 가 내역/휴장일 조회에 사용할 stock 그룹 키"
  type        = string
  default     = "a"
}

#  모니터링
variable "alarm_emails" {
  description = "CloudWatch 알람을 받을 이메일 목록"
  type        = list(string)
  default     = []
}
