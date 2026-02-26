variable "environment" {
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

variable "db_name" {
   type = string
}

variable "db_username" {
   type = string
}

variable "db_password" {
   type = string
}

variable "target_group_arn" {
   type = string
}

variable "ecs_sg_id" {
   type = string
}

variable "image_tag" {
   type        = string
   description = "Docker image tag for ECS task"
}

variable "fe_url" {
   type = string
}

variable "app_key" {
   type = string
}

variable "app_secret" {
   type = string
}

variable "approval_key_url" {
   type = string
}

variable "secret_key" {
   type = string
}

variable "ws_url" {
   type = string
}