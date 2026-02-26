variable "environment" {
   type = string
}

variable "vpc_id" {
   type = string
}

variable "public_subnets" {
   type = list(string)
}

variable "alb_sg_id" {
   type = string
}

variable "alb_acm_certificate_arn" {
   type = string
}