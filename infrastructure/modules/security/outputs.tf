output "alb_sg_id" {
  value = aws_security_group.alb.id
}

output "bff_sg_id" {
  value = aws_security_group.bff.id
}

output "stock_sg_id" {
  value = aws_security_group.stock.id
}

output "account_sg_id" {
  value = aws_security_group.account.id
}

output "rds_sg_id" {
  value = aws_security_group.rds.id
}

output "redis_sg_id" {
  value = aws_security_group.redis.id
}
