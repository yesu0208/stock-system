module "network" {
  source = "../../modules/network"

  name               = local.name
  region             = var.aws_region
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  single_nat_gateway = var.single_nat_gateway
}

module "security" {
  source = "../../modules/security"

  name   = local.name
  vpc_id = module.network.vpc_id
  ports  = local.ports
}

module "dns" {
  source = "../../modules/dns"

  providers = {
    aws           = aws
    aws.us_east_1 = aws.us_east_1
  }

  domain_name        = var.domain_name
  create_hosted_zone = var.create_hosted_zone
}

module "ecr" {
  source = "../../modules/ecr"

  name         = var.project
  repositories = ["bff-server", "stock-server", "account-server"]
}

module "secrets" {
  source = "../../modules/secrets"

  name        = local.name
  db_services = ["bff", "stock", "account"]

  # application.yaml 의 ${...} 환경변수 이름과 동일하게 맞춘다.
  app_secret_keys = {
    bff = [
      "SECRET_KEY",
      "ADMIN_PASSWORD",
      "SLACK_WEBHOOK_URL",
      "NAVER_CLIENT_ID",
      "NAVER_CLIENT_SECRET",
    ]
    stock = [
      "APPROVAL_KEY_URL",
      "APP_KEY",
      "APP_SECRET",
      "WS_URL",
      "CHART_API_URL",
      "CHART_API_APPKEY",
      "CHART_API_APPSECRET",
    ]
  }
}

module "rds" {
  source = "../../modules/rds"

  name              = local.name
  subnet_ids        = module.network.private_data_subnet_ids
  security_group_id = module.security.rds_sg_id
  instance_class    = var.db_instance_class
  multi_az          = var.db_multi_az
}

module "redis" {
  source = "../../modules/elasticache"

  name              = local.name
  subnet_ids        = module.network.private_data_subnet_ids
  security_group_id = module.security.redis_sg_id
  node_type         = var.redis_node_type
}

module "alb" {
  source = "../../modules/alb"

  name              = local.name
  vpc_id            = module.network.vpc_id
  subnet_ids        = module.network.public_subnet_ids
  security_group_id = module.security.alb_sg_id
  certificate_arn   = module.dns.api_certificate_arn
  zone_id           = module.dns.zone_id
  api_domain        = module.dns.api_domain
  target_port       = local.ports.bff
}
