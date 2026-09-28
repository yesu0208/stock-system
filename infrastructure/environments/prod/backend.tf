terraform {
  backend "s3" {
    bucket       = "stock-system-tfstate"
    key          = "prod/terraform.tfstate"
    region       = "ap-northeast-2"
    encrypt      = true
    use_lockfile = true
  }
}
