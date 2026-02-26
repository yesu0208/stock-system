resource "aws_ecs_cluster" "this" {
  name = "${var.environment}-ecs"
}

resource "aws_iam_role" "ecs_task_execution_role" {
  name = "${var.environment}-ecs-task-execution-role"

  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [{
      Action = "sts:AssumeRole"
      Effect = "Allow"
      Principal = {
        Service = "ecs-tasks.amazonaws.com"
      }
    }]
  })
}

resource "aws_iam_role_policy_attachment" "ecs_task_execution_role_policy" {
  role = aws_iam_role.ecs_task_execution_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_cloudwatch_log_group" "ecs" {
  name = "/ecs/${var.environment}"
}

resource "aws_ecs_task_definition" "this" {
  family = "${var.environment}-task"
  requires_compatibilities = ["FARGATE"]
  network_mode = "awsvpc"
  cpu = "256"
  memory = "512"
  execution_role_arn = aws_iam_role.ecs_task_execution_role.arn

  container_definitions = jsonencode([
    {
      name = "backend"
      image = "${var.ecr_repo}:${var.image_tag}"
      essential = true
      portMappings = [{
        containerPort = 8080
        hostPort = 8080
      }]
      environment = [
        {
          name = "REDIS_HOST"
          value = var.redis_endpoint
        },
        {
          name = "REDIS_PORT"
          value = "6379"
        },
        {
          name = "LOCAL_DB_URL"
          value = "jdbc:mysql://${var.db_endpoint}/${var.db_name}"
        },
        {
          name = "LOCAL_DB_USER"
          value = var.db_username
        },
        {
          name = "LOCAL_DB_PW"
          value = var.db_password
        },
        {
          name = "APP_KEY"
          value = var.app_key
        },
        {
          name = "APP_SECRET"
          value = var.app_secret
        },
        {
          name = "APPROVAL_KEY_URL"
          value = var.approval_key_url
        },
        {
          name = "SECRET_KEY"
          value = var.secret_key
        },
        {
          name = "WS_URL"
          value = var.ws_url
        },
        {
          name = "FE_URL"
          value = var.fe_url
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          awslogs-group  = aws_cloudwatch_log_group.ecs.name
          awslogs-region = "ap-northeast-2"
          awslogs-stream-prefix = "ecs"
        }
      }
    }
  ])
}

resource "aws_ecs_service" "this" {
  name = "${var.environment}-service"
  cluster = aws_ecs_cluster.this.id
  task_definition = aws_ecs_task_definition.this.arn
  launch_type = "FARGATE"
  desired_count = 1

  network_configuration {
    subnets = var.private_subnets
    security_groups = [var.ecs_sg_id]
    assign_public_ip = false
  }

  load_balancer {
    target_group_arn = var.target_group_arn
    container_name = "backend"
    container_port = 8080
  }
}

output "ecs_cluster_arn" {
  value = aws_ecs_cluster.this.arn
}