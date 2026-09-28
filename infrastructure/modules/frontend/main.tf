# 프론트엔드 정적 호스팅 (S3 + CloudFront)
# - web 버킷: React 빌드 결과물 (SPA)
# - uploads 버킷: 프로필 이미지 (/profile/* 경로로 같은 도메인에서 제공)
# 두 버킷 모두 퍼블릭 접근을 막고 CloudFront OAC 로만 읽음.

locals {
  # AWS 관리형 정책 ID
  cache_policy_caching_optimized = "658327ea-f89d-4fab-a63d-7e88639e58f6"
  response_headers_security      = "67f7725c-6f97-4210-82d7-5512b31e9d03"
  origin_request_policy_cors_s3  = "88a5eaf4-2fd4-4709-b370-b4c650ea3fcf"
}

# Buckets
resource "aws_s3_bucket" "web" {
  bucket        = "${var.name}-web"
  force_destroy = var.force_destroy
}

resource "aws_s3_bucket" "uploads" {
  bucket        = "${var.name}-uploads"
  force_destroy = var.force_destroy
}

resource "aws_s3_bucket_public_access_block" "this" {
  for_each = { web = aws_s3_bucket.web.id, uploads = aws_s3_bucket.uploads.id }

  bucket                  = each.value
  block_public_acls       = true
  block_public_policy     = true
  ignore_public_acls      = true
  restrict_public_buckets = true
}

resource "aws_s3_bucket_server_side_encryption_configuration" "this" {
  for_each = { web = aws_s3_bucket.web.id, uploads = aws_s3_bucket.uploads.id }

  bucket = each.value

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_versioning" "uploads" {
  bucket = aws_s3_bucket.uploads.id

  versioning_configuration {
    status = "Enabled"
  }
}

# CloudFront
resource "aws_cloudfront_origin_access_control" "this" {
  name                              = "${var.name}-s3-oac"
  origin_access_control_origin_type = "s3"
  signing_behavior                  = "always"
  signing_protocol                  = "sigv4"
}

resource "aws_cloudfront_distribution" "this" {
  enabled             = true
  is_ipv6_enabled     = true
  http_version        = "http2and3"
  comment             = "${var.name} frontend"
  default_root_object = "index.html"
  aliases             = var.aliases
  price_class         = var.price_class

  origin {
    origin_id                = "web"
    domain_name              = aws_s3_bucket.web.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.this.id
  }

  origin {
    origin_id                = "uploads"
    domain_name              = aws_s3_bucket.uploads.bucket_regional_domain_name
    origin_access_control_id = aws_cloudfront_origin_access_control.this.id
  }

  default_cache_behavior {
    target_origin_id           = "web"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    cache_policy_id            = local.cache_policy_caching_optimized
    response_headers_policy_id = local.response_headers_security
  }

  # 프로필 이미지 (bff 가 uploads 버킷의 profile/ 경로에 저장)
  ordered_cache_behavior {
    path_pattern               = "/profile/*"
    target_origin_id           = "uploads"
    viewer_protocol_policy     = "redirect-to-https"
    allowed_methods            = ["GET", "HEAD", "OPTIONS"]
    cached_methods             = ["GET", "HEAD"]
    compress                   = true
    cache_policy_id            = local.cache_policy_caching_optimized
    origin_request_policy_id   = local.origin_request_policy_cors_s3
    response_headers_policy_id = local.response_headers_security
  }

  # SPA: 존재하지 않는 경로는 index.html 로 응답
  custom_error_response {
    error_code            = 403
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  custom_error_response {
    error_code            = 404
    response_code         = 200
    response_page_path    = "/index.html"
    error_caching_min_ttl = 10
  }

  restrictions {
    geo_restriction {
      restriction_type = "none"
    }
  }

  viewer_certificate {
    acm_certificate_arn      = var.certificate_arn
    ssl_support_method       = "sni-only"
    minimum_protocol_version = "TLSv1.2_2021"
  }
}

# CloudFront 배포에서만 버킷을 읽을 수 있도록 제한
data "aws_iam_policy_document" "bucket" {
  for_each = { web = aws_s3_bucket.web.arn, uploads = aws_s3_bucket.uploads.arn }

  statement {
    sid       = "AllowCloudFrontRead"
    actions   = ["s3:GetObject"]
    resources = ["${each.value}/*"]

    principals {
      type        = "Service"
      identifiers = ["cloudfront.amazonaws.com"]
    }

    condition {
      test     = "StringEquals"
      variable = "AWS:SourceArn"
      values   = [aws_cloudfront_distribution.this.arn]
    }
  }
}

resource "aws_s3_bucket_policy" "this" {
  for_each = { web = aws_s3_bucket.web.id, uploads = aws_s3_bucket.uploads.id }

  bucket = each.value
  policy = data.aws_iam_policy_document.bucket[each.key].json

  depends_on = [aws_s3_bucket_public_access_block.this]
}

# DNS
resource "aws_route53_record" "alias" {
  for_each = {
    for pair in setproduct(var.aliases, ["A", "AAAA"]) : "${pair[0]}-${pair[1]}" => {
      name = pair[0]
      type = pair[1]
    }
  }

  zone_id = var.zone_id
  name    = each.value.name
  type    = each.value.type

  alias {
    name                   = aws_cloudfront_distribution.this.domain_name
    zone_id                = aws_cloudfront_distribution.this.hosted_zone_id
    evaluate_target_health = false
  }
}
