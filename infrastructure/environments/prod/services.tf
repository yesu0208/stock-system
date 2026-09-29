# 애플리케이션 서비스 구성
# bff-server: ALB 뒤, 오토스케일링 (WebSocket/REST 진입점)
# account-server: 내부 전용, 오토스케일링 (Redis Streams 컨슈머 그룹으로 분산 처리)
# stock-server-<그룹>: 내부 전용, 그룹마다 1개 고정 (담당 종목 체결/증권 WebSocket)

locals {
  jdbc_options = "serverTimezone=Asia/Seoul&characterEncoding=UTF-8"

  # 모든 서비스 공통 환경변수
  common_env = {
    SPRING_PROFILES_ACTIVE = "prod"
    TZ                     = "Asia/Seoul"
    JAVA_TOOL_OPTIONS      = "-XX:MaxRAMPercentage=75.0 -Duser.timezone=Asia/Seoul"
    REDIS_HOST             = module.redis.primary_endpoint
    REDIS_PORT             = tostring(module.redis.port)
  }

  db_url = { for s in ["bff", "stock", "account"] :
    s => "jdbc:mysql://${module.rds.address}:${module.rds.port}/${s}_db?${local.jdbc_options}"
  }

  # DB 계정은 서비스별 시크릿의 username/password 키에서 주입
  db_secrets = { for s in ["bff", "stock", "account"] : s => {
    LOCAL_DB_USER = "${module.secrets.db_secret_arns[s]}:username::"
    LOCAL_DB_PW   = "${module.secrets.db_secret_arns[s]}:password::"
  } }

  # bff 가 내역/휴장일 조회에 사용하는 대표 stock 서버 (모든 그룹이 같은 stock_db 를 사용)
  primary_stock_service = "stock-server-${var.primary_stock_group_key}"

  account_api_url = "http://account-server:${local.ports.account}"
  stock_api_url   = "http://${local.primary_stock_service}:${local.ports.stock}"
}

# bff-server
data "aws_iam_policy_document" "bff_task" {
  # 프로필 이미지 저장 (S3 저장소 구현체 적용 후 사용)
  statement {
    actions   = ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"]
    resources = ["${module.frontend.uploads_bucket_arn}/profile/*"]
  }
}

module "bff_server" {
  source = "../../modules/ecs-service"

  name          = local.name
  service_name  = "bff-server"
  cluster_id    = module.ecs_cluster.cluster_id
  cluster_name  = module.ecs_cluster.cluster_name
  namespace_arn = module.ecs_cluster.namespace_arn

  image          = module.ecr.repository_urls["bff-server"]
  image_tag      = var.image_tag
  container_port = local.ports.bff
  cpu            = var.bff.cpu
  memory         = var.bff.memory

  environment = merge(local.common_env, {
    LOCAL_DB_URL    = local.db_url["bff"]
    ACCOUNT_API_URL = local.account_api_url
    STOCK_API_URL   = local.stock_api_url
    FE_URL          = "https://${module.dns.frontend_domain}"
    ADMIN_USERNAME  = "admin"
    ADMIN_NICKNAME  = "관리자"

    # S3 프로필 이미지 저장소 구현체에서 사용할 값
    PROFILE_IMAGE_BUCKET   = module.frontend.uploads_bucket_name
    PROFILE_IMAGE_BASE_URL = "https://${module.dns.frontend_domain}"
  })

  secrets = merge(local.db_secrets["bff"], {
    for k in ["SECRET_KEY", "ADMIN_PASSWORD", "SLACK_WEBHOOK_URL", "NAVER_CLIENT_ID", "NAVER_CLIENT_SECRET"] :
    k => "${module.secrets.app_secret_arns["bff"]}:${k}::"
  })

  secret_arns = [
    module.secrets.db_secret_arns["bff"],
    module.secrets.app_secret_arns["bff"],
  ]

  task_role_policy_json = data.aws_iam_policy_document.bff_task.json

  desired_count      = var.bff.min_capacity
  subnet_ids         = module.network.private_app_subnet_ids
  security_group_ids = [module.security.bff_sg_id]
  target_group_arn   = module.alb.bff_target_group_arn

  autoscaling = {
    min_capacity        = var.bff.min_capacity
    max_capacity        = var.bff.max_capacity
    cpu_target          = 60
    requests_per_target = 1000
  }
  request_count_resource_label = "${module.alb.alb_arn_suffix}/${module.alb.bff_target_group_arn_suffix}"
}

# account-server
module "account_server" {
  source = "../../modules/ecs-service"

  name           = local.name
  service_name   = "account-server"
  discovery_name = "account-server"
  cluster_id     = module.ecs_cluster.cluster_id
  cluster_name   = module.ecs_cluster.cluster_name
  namespace_arn  = module.ecs_cluster.namespace_arn

  image          = module.ecr.repository_urls["account-server"]
  image_tag      = var.image_tag
  container_port = local.ports.account
  cpu            = var.account.cpu
  memory         = var.account.memory

  environment = merge(local.common_env, {
    LOCAL_DB_URL = local.db_url["account"]
  })

  secrets     = local.db_secrets["account"]
  secret_arns = [module.secrets.db_secret_arns["account"]]

  desired_count      = var.account.min_capacity
  subnet_ids         = module.network.private_app_subnet_ids
  security_group_ids = [module.security.account_sg_id]

  autoscaling = {
    min_capacity = var.account.min_capacity
    max_capacity = var.account.max_capacity
    cpu_target   = 60
  }
}

# stock-server (그룹별 1개)
module "stock_server" {
  source   = "../../modules/ecs-service"
  for_each = var.stock_groups

  name           = local.name
  service_name   = "stock-server-${each.key}"
  discovery_name = "stock-server-${each.key}"
  cluster_id     = module.ecs_cluster.cluster_id
  cluster_name   = module.ecs_cluster.cluster_name
  namespace_arn  = module.ecs_cluster.namespace_arn

  image          = module.ecr.repository_urls["stock-server"]
  image_tag      = var.image_tag
  container_port = local.ports.stock
  cpu            = var.stock.cpu
  memory         = var.stock.memory

  environment = merge(local.common_env, {
    LOCAL_DB_URL    = local.db_url["stock"] # 모든 그룹이 stock_db 공유
    ACCOUNT_API_URL = local.account_api_url
    SERVER_GROUP    = each.value.group # application.yaml 의 server.group
  })

  secrets = merge(local.db_secrets["stock"], {
    for k in local.stock_app_secret_keys :
    k => "${module.secrets.app_secret_arns["stock-${each.key}"]}:${k}::"
  })

  secret_arns = [
    module.secrets.db_secret_arns["stock"],
    module.secrets.app_secret_arns["stock-${each.key}"],
  ]

  desired_count      = 1
  subnet_ids         = module.network.private_app_subnet_ids
  security_group_ids = [module.security.stock_sg_id]

  # 같은 그룹 태스크가 동시에 2개 뜨면 체결·외부 WebSocket·스케줄이 중복되므로
  # 이전 태스크를 먼저 내린 뒤 새 태스크를 띄운다. (배포는 장 마감 후 권장)
  deployment_minimum_healthy_percent = 0
  deployment_maximum_percent         = 100

  autoscaling = null
}
