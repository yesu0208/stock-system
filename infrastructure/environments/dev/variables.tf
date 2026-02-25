variable "environment" {
  type    = string
  default = "dev"
}

variable "vpc_cidr" {
  type    = string
  default = "10.0.0.0/16"
}

variable "public_subnet_cidr" {
  type    = list(string)
  default = ["10.0.1.0/24"]
}

variable "private_subnet_cidr" {
  type    = list(string)
  default = ["10.0.2.0/24"]
}

variable "db_username" {
  type    = string
  default = "admin"
}

variable "db_password" {
  type    = string
  default = "ChangeMe123!"
}

variable "availability_zone" {
  type    = string
  default = "ap-northeast-2a"
}