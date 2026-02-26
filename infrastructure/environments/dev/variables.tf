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
  default = ["10.0.1.0/24", "10.0.11.0/24"]
}

variable "private_subnet_cidr" {
  type    = list(string)
  default = ["10.0.2.0/24", "10.0.12.0/24"]
}

variable "db_username" {
  type    = string
  default = "admin"
}

variable "db_password" {
  type    = string
  default = "ChangeMe123!"
}

variable "db_name" {
  type    = string
  default = "default_db"
}

variable "availability_zones" {
  type    = list(string)
  default = ["ap-northeast-2a","ap-northeast-2c"]
}

variable "app_key" {
  type = string
  sensitive = true
}

variable "app_secret" {
  type = string
  sensitive = true
}

variable "approval_key_url" {
  type = string
  sensitive = true
}

variable "secret_key" {
  type = string
  sensitive = true
}

variable "ws_url" {
  type = string
  sensitive = true
}

variable "image_tag" {
  type    = string
  default = "latest"
}