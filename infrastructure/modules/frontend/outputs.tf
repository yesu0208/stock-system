output "frontend_s3_bucket" {
  value = aws_s3_bucket.frontend.bucket
}

output "frontend_cloudfront_url" {
  value = aws_cloudfront_distribution.frontend.domain_name
}

output "frontend_cloudfront_distribution_id" {
  value = aws_cloudfront_distribution.frontend.id
}