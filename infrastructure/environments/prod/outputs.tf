# 도메인
output "name_servers" {
  description = "도메인 등록기관에 설정할 네임서버"
  value       = module.dns.name_servers
}

output "frontend_url" {
  value = "https://${module.dns.frontend_domain}"
}

output "api_url" {
  value = "https://${module.dns.api_domain}"
}

# 배포(CI/CD)에서 사용하는 값
output "ecr_repository_urls" {
  value = module.ecr.repository_urls
}

output "ecs_cluster_name" {
  value = module.ecs_cluster.cluster_name
}

output "ecs_services" {
  description = "서비스 이름 → 태스크 정의 family / 컨테이너 이름"
  value = {
    for s in concat([module.bff_server, module.account_server], values(module.stock_server)) :
    s.service_name => {
      task_definition_family = s.task_definition_family
      container_name         = s.container_name
    }
  }
}

output "frontend_bucket" {
  value = module.frontend.web_bucket_name
}

output "cloudfront_distribution_id" {
  value = module.frontend.distribution_id
}

output "github_deploy_role_arn" {
  value = module.github_oidc.deploy_role_arn
}

output "github_terraform_plan_role_arn" {
  value = module.github_oidc.terraform_plan_role_arn
}

# 데이터
output "rds_endpoint" {
  value = module.rds.endpoint
}

output "rds_master_user_secret_arn" {
  description = "init-databases.sql 실행 시 사용할 마스터 계정 시크릿"
  value       = module.rds.master_user_secret_arn
}

output "redis_endpoint" {
  value = module.redis.primary_endpoint
}

output "db_secret_arns" {
  description = "서비스별 DB 계정 시크릿"
  value       = module.secrets.db_secret_arns
}

output "app_secret_arns" {
  description = "값을 직접 입력해야 하는 애플리케이션 시크릿"
  value       = module.secrets.app_secret_arns
}
