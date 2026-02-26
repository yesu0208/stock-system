terraform {
  backend "s3" {
    bucket = "stock-system-terraform-state"
    key = "dev/terraform.tfstate"
    region = "ap-northeast-2"
    dynamodb_table = "stock-system-terraform-locks"
    encrypt = true
  }
}