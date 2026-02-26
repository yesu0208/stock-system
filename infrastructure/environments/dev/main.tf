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
  vpc_id      = module.vpc.vpc_id
  private_subnets = module.vpc.private_subnets
  db_username = var.db_username
  db_password = var.db_password
  rds_sg_id = module.security.rds_sg_id
}

module "redis" {
  source          = "../../modules/redis"
  environment     = var.environment
  subnet_ids      = module.vpc.private_subnets
  vpc_id          = module.vpc.vpc_id
  redis_sg_id = module.security.redis_sg_id
}

module "ecr" {
  source      = "../../modules/ecr"
  environment = var.environment
}

module "ecs" {
  source          = "../../modules/ecs"
  environment     = var.environment
  vpc_id          = module.vpc.vpc_id
  private_subnets = module.vpc.private_subnets
  ecr_repo        = module.ecr.repo_url
  redis_endpoint  = module.redis.endpoint
  db_endpoint     = module.rds.endpoint
  target_group_arn = module.alb.target_group_arn
  ecs_sg_id        = module.security.ecs_sg_id
}

module "alb" {
  source          = "../../modules/alb"
  environment     = var.environment
  vpc_id          = module.vpc.vpc_id
  public_subnets  = module.vpc.public_subnets
  alb_sg_id = module.security.alb_sg_id
}