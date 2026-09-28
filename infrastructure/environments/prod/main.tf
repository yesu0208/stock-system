module "network" {
  source = "../../modules/network"

  name               = local.name
  region             = var.aws_region
  vpc_cidr           = var.vpc_cidr
  availability_zones = var.availability_zones
  single_nat_gateway = var.single_nat_gateway
}
