output "vpc_id" {
  description = "The ID of the VPC"
  value       = module.vpc.vpc_id
}

output "public_subnets" {
  description = "List of public subnet IDs"
  value       = module.vpc.public_subnets
}

output "private_subnets" {
  description = "List of private subnet IDs"
  value       = module.vpc.private_subnets
}

output "rds_endpoint" {
  description = "RDS endpoint for BE database connection"
  value       = module.rds.endpoint
}

output "redis_endpoint" {
  description = "Redis endpoint for pub/sub"
  value       = module.redis.endpoint
}

output "ecr_repo_url" {
  description = "ECR repository URL for backend Docker image"
  value       = module.ecr.repo_url
}

output "ecs_cluster_arn" {
  description = "ARN of ECS cluster"
  value       = module.ecs.ecs_cluster_arn
}

output "ecs_service_arn" {
  description = "ARN of ECS service"
  value       = module.ecs.ecs_service_arn
}

output "alb_dns" {
  description = "DNS name of ALB for frontend/backend access"
  value       = module.alb.alb_dns
}

output "frontend_s3_bucket" {
  value = module.frontend.frontend_s3_bucket
}

output "frontend_cloudfront_url" {
  value = module.frontend.frontend_cloudfront_url
}