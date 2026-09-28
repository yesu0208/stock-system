# Public ALB → bff-server
resource "aws_lb" "this" {
  name               = "${var.name}-alb"
  load_balancer_type = "application"
  internal           = false
  subnets            = var.subnet_ids
  security_groups    = [var.security_group_id]

  idle_timeout               = var.idle_timeout
  drop_invalid_header_fields = true
  enable_deletion_protection = var.deletion_protection

  tags = { Name = "${var.name}-alb" }
}

resource "aws_lb_target_group" "bff" {
  name        = "${var.name}-bff-tg"
  port        = var.target_port
  protocol    = "HTTP"
  target_type = "ip" # Fargate awsvpc
  vpc_id      = var.vpc_id

  deregistration_delay = 30

  health_check {
    path                = var.health_check_path
    matcher             = "200"
    interval            = 15
    timeout             = 5
    healthy_threshold   = 2
    unhealthy_threshold = 3
  }

  stickiness {
    type            = "lb_cookie"
    cookie_duration = 86400
    enabled         = true
  }

  tags = { Name = "${var.name}-bff-tg" }
}

resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}

resource "aws_lb_listener" "https" {
  load_balancer_arn = aws_lb.this.arn
  port              = 443
  protocol          = "HTTPS"
  ssl_policy        = "ELBSecurityPolicy-TLS13-1-2-2021-06"
  certificate_arn   = var.certificate_arn

  # 호스트가 맞지 않는 요청(IP 직접 접근 등)은 차단
  default_action {
    type = "fixed-response"

    fixed_response {
      content_type = "text/plain"
      message_body = "Not Found"
      status_code  = "404"
    }
  }
}

resource "aws_lb_listener_rule" "api" {
  listener_arn = aws_lb_listener.https.arn
  priority     = 10

  condition {
    host_header {
      values = [var.api_domain]
    }
  }

  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.bff.arn
  }
}

resource "aws_route53_record" "api" {
  for_each = toset(["A", "AAAA"])

  zone_id = var.zone_id
  name    = var.api_domain
  type    = each.value

  alias {
    name                   = aws_lb.this.dns_name
    zone_id                = aws_lb.this.zone_id
    evaluate_target_health = true
  }
}
