variable "environment" {
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

variable "rds_sg_id" {
   type = string
}