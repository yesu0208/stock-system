variable "environment" {
  type = string
  description = "Environment name (dev, prod)"
}

variable "acm_certificate_arn" {
  type = string
}

variable "alb_dns" {
  type = string
  description = "ALB DNS name for backend API"
}