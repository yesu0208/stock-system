variable "environment" {
   type = string
}

variable "vpc_id" {
   type = string
}

variable "private_subnets" {
   type = list(string)
}

variable "db_username" {
   type = string
}

variable "db_password" {
   type = string
}

variable "ecs_security_group_id" {
   type = string
}