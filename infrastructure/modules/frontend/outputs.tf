output "web_bucket_name" {
  description = "프론트 빌드 결과물 업로드 대상"
  value       = aws_s3_bucket.web.bucket
}

output "web_bucket_arn" {
  value = aws_s3_bucket.web.arn
}

output "uploads_bucket_name" {
  description = "프로필 이미지 버킷"
  value       = aws_s3_bucket.uploads.bucket
}

output "uploads_bucket_arn" {
  value = aws_s3_bucket.uploads.arn
}

output "distribution_id" {
  description = "배포 후 캐시 무효화에 사용"
  value       = aws_cloudfront_distribution.this.id
}

output "distribution_arn" {
  value = aws_cloudfront_distribution.this.arn
}

output "distribution_domain_name" {
  value = aws_cloudfront_distribution.this.domain_name
}
