variable "environment" {
   type = string
}

variable "subnet_ids" {
   type = list(string)
}

variable "vpc_id" {
   type = string
}

variable "ecs_security_group_id" {
   type = string
}