output "alb_arn_suffix" {
  description = "CloudWatch 지표 차원용"
  value       = aws_lb.this.arn_suffix
}

output "alb_dns_name" {
  value = aws_lb.this.dns_name
}

output "bff_target_group_arn" {
  value = aws_lb_target_group.bff.arn
}

output "bff_target_group_arn_suffix" {
  description = "오토스케일링(ALBRequestCountPerTarget) 지표용"
  value       = aws_lb_target_group.bff.arn_suffix
}

output "https_listener_arn" {
  value = aws_lb_listener.https.arn
}
