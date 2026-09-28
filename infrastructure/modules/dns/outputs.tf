output "zone_id" {
  value = local.zone_id
}

output "name_servers" {
  description = "도메인 등록기관에 설정할 네임서버 (호스팅 영역을 새로 만든 경우)"
  value       = var.create_hosted_zone ? aws_route53_zone.this[0].name_servers : []
}

output "api_domain" {
  value = local.api_domain
}

output "frontend_domain" {
  value = local.frontend_domain
}

output "frontend_aliases" {
  value = concat([local.frontend_domain], local.frontend_alt)
}

output "api_certificate_arn" {
  description = "검증이 끝난 ALB 인증서 ARN"
  value       = aws_acm_certificate_validation.api.certificate_arn
}

output "frontend_certificate_arn" {
  description = "검증이 끝난 CloudFront 인증서 ARN (us-east-1)"
  value       = aws_acm_certificate_validation.frontend.certificate_arn
}
