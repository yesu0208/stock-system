variable "environment" {
  type = string
  description = "Environment name (dev, prod)"
}

variable "acm_certificate_arn" {
  type = string
}