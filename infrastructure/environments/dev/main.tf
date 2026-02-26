module "vpc" {
  source            = "../../modules/vpc"
  environment       = var.environment
  vpc_cidr          = var.vpc_cidr
  public_subnet_cidr  = var.public_subnet_cidr
  private_subnet_cidr = var.private_subnet_cidr
  availability_zone   = var.availability_zone
}

module "security" {
  source      = "../../modules/security"
  environment = var.environment
  vpc_id      = module.vpc.vpc_id
}

module "rds" {
  source      = "../../modules/rds"
  environment = var.environment
  private_subnets = module.vpc.private_subnets
  db_username = var.db_username
  db_password = var.db_password
  db_name = var.db_name
  rds_sg_id = module.security.rds_sg_id
}

module "redis" {
  source          = "../../modules/redis"
  environment     = var.environment
  subnet_ids      = module.vpc.private_subnets
  redis_sg_id = module.security.redis_sg_id
}

module "ecr" {
  source      = "../../modules/ecr"
  environment = var.environment
}

module "ecs" {
  source          = "../../modules/ecs"
  environment     = var.environment
  private_subnets = module.vpc.private_subnets
  ecr_repo        = module.ecr.repo_url
  redis_endpoint  = module.redis.endpoint
  db_endpoint     = module.rds.endpoint
  target_group_arn = module.alb.target_group_arn
  ecs_sg_id        = module.security.ecs_sg_id
  db_username = module.rds.username
  db_password = module.rds.password
  db_name = module.rds.db_name
  app_key = var.app_key
  app_secret = var.app_secret
  approval_key_url = var.approval_key_url
  secret_key = var.secret_key
  ws_url = var.ws_url
}

module "alb" {
  source          = "../../modules/alb"
  environment     = var.environment
  vpc_id          = module.vpc.vpc_id
  public_subnets  = module.vpc.public_subnets
  alb_sg_id = module.security.alb_sg_id
}

module "frontend" {
  source = "../../modules/frontend"
  environment = var.environment
}