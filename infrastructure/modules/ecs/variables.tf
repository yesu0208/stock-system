variable "environment" {
   type = string
}

variable "vpc_id" {
   type = string
}

variable "private_subnets" {
   type = list(string)
}

variable "ecr_repo" {
   type = string
}

variable "redis_endpoint" {
   type = string
}

variable "db_endpoint" {
   type = string
}