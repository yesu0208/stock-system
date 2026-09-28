# GitHub Actions -> AWS (OIDC, 장기 액세스 키 없음)

data "aws_caller_identity" "current" {}

locals {
  oidc_provider_arn = var.create_oidc_provider ? aws_iam_openid_connect_provider.github[0].arn : "arn:aws:iam::${data.aws_caller_identity.current.account_id}:oidc-provider/token.actions.githubusercontent.com"
}

resource "aws_iam_openid_connect_provider" "github" {
  count = var.create_oidc_provider ? 1 : 0

  url             = "https://token.actions.githubusercontent.com"
  client_id_list  = ["sts.amazonaws.com"]
  thumbprint_list = ["6938fd4d98bab03faadb97b34396831e3780aea1"]
}

data "aws_iam_policy_document" "assume" {
  for_each = {
    deploy = ["repo:${var.github_repository}:ref:refs/heads/${var.deploy_branch}"]
    # plan 은 시크릿 값을 읽으므로 PR/다른 브랜치에는 허용하지 않는다
    plan = ["repo:${var.github_repository}:ref:refs/heads/${var.deploy_branch}"]
  }

  statement {
    actions = ["sts:AssumeRoleWithWebIdentity"]

    principals {
      type        = "Federated"
      identifiers = [local.oidc_provider_arn]
    }

    condition {
      test     = "StringEquals"
      variable = "token.actions.githubusercontent.com:aud"
      values   = ["sts.amazonaws.com"]
    }

    condition {
      test     = "StringLike"
      variable = "token.actions.githubusercontent.com:sub"
      values   = each.value
    }
  }
}

# deploy 역할
resource "aws_iam_role" "deploy" {
  name               = "${var.name}-github-deploy"
  assume_role_policy = data.aws_iam_policy_document.assume["deploy"].json
}

data "aws_iam_policy_document" "deploy" {
  statement {
    sid       = "EcrAuth"
    actions   = ["ecr:GetAuthorizationToken"]
    resources = ["*"]
  }

  statement {
    sid = "EcrPush"
    actions = [
      "ecr:BatchCheckLayerAvailability",
      "ecr:BatchGetImage",
      "ecr:CompleteLayerUpload",
      "ecr:InitiateLayerUpload",
      "ecr:PutImage",
      "ecr:UploadLayerPart",
    ]
    resources = var.ecr_repository_arns
  }

  statement {
    sid = "EcsDeploy"
    actions = [
      "ecs:DescribeTaskDefinition",
      "ecs:RegisterTaskDefinition",
      "ecs:DescribeServices",
      "ecs:UpdateService",
    ]
    resources = ["*"]
  }

  statement {
    sid       = "PassTaskRoles"
    actions   = ["iam:PassRole"]
    resources = var.ecs_role_arns

    condition {
      test     = "StringEquals"
      variable = "iam:PassedToService"
      values   = ["ecs-tasks.amazonaws.com"]
    }
  }

  statement {
    sid       = "FrontendUpload"
    actions   = ["s3:ListBucket"]
    resources = [var.web_bucket_arn]
  }

  statement {
    sid       = "FrontendObjects"
    actions   = ["s3:PutObject", "s3:DeleteObject", "s3:GetObject"]
    resources = ["${var.web_bucket_arn}/*"]
  }

  statement {
    sid       = "CloudFrontInvalidation"
    actions   = ["cloudfront:CreateInvalidation"]
    resources = [var.cloudfront_distribution_arn]
  }
}

resource "aws_iam_role_policy" "deploy" {
  name   = "deploy"
  role   = aws_iam_role.deploy.id
  policy = data.aws_iam_policy_document.deploy.json
}

# terraform-plan 역할
resource "aws_iam_role" "plan" {
  name               = "${var.name}-github-terraform-plan"
  assume_role_policy = data.aws_iam_policy_document.assume["plan"].json
}

resource "aws_iam_role_policy_attachment" "plan_readonly" {
  role       = aws_iam_role.plan.name
  policy_arn = "arn:aws:iam::aws:policy/ReadOnlyAccess"
}

data "aws_iam_policy_document" "plan_state" {
  statement {
    actions   = ["s3:ListBucket"]
    resources = ["arn:aws:s3:::${var.state_bucket_name}"]
  }

  # use_lockfile 사용 시 plan 도 잠금 파일(.tflock)을 쓰고 지운다
  statement {
    actions   = ["s3:GetObject", "s3:PutObject", "s3:DeleteObject"]
    resources = ["arn:aws:s3:::${var.state_bucket_name}/*"]
  }
}

resource "aws_iam_role_policy" "plan_state" {
  name   = "terraform-state"
  role   = aws_iam_role.plan.id
  policy = data.aws_iam_policy_document.plan_state.json
}

# ReadOnlyAccess 에는 시크릿 값 읽기가 없어 plan 의 secret_version refresh 가 실패하므로 추가
data "aws_iam_policy_document" "plan_secrets" {
  statement {
    actions   = ["secretsmanager:GetSecretValue"]
    resources = var.secret_arns
  }
}

resource "aws_iam_role_policy" "plan_secrets" {
  name   = "read-secrets"
  role   = aws_iam_role.plan.id
  policy = data.aws_iam_policy_document.plan_secrets.json
}
