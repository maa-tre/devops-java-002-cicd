variable "aws_region" {
  description = "AWS region for the demo environment."
  type        = string
  default     = "us-east-1"
}

variable "project_name" {
  description = "Prefix used to name and tag AWS resources."
  type        = string
  default     = "space-hyper-cicd"
}

variable "instance_type" {
  description = "EC2 instance type. t3.micro is intended for low-traffic demonstration use."
  type        = string
  default     = "t3.micro"
}

variable "app_port" {
  description = "Public TCP port for the Java application."
  type        = number
  default     = 8080

  validation {
    condition     = var.app_port >= 1 && var.app_port <= 65535
    error_message = "app_port must be between 1 and 65535."
  }
}

variable "rollback_console_port" {
  description = "Public testing port for the rollback console."
  type        = number
  default     = 8081

  validation {
    condition     = var.rollback_console_port >= 1 && var.rollback_console_port <= 65535 && var.rollback_console_port != var.app_port
    error_message = "rollback_console_port must be a valid TCP port different from app_port."
  }
}

variable "allowed_ssh_cidrs" {
  description = "Trusted IPv4 CIDRs allowed to SSH to the instance."
  type        = list(string)

  validation {
    condition = length(var.allowed_ssh_cidrs) > 0 && alltrue([
      for cidr in var.allowed_ssh_cidrs :
      can(cidrnetmask(cidr)) && cidr != "0.0.0.0/0"
    ])
    error_message = "Provide at least one trusted IPv4 SSH CIDR; 0.0.0.0/0 is not allowed."
  }
}

variable "public_key_path" {
  description = "Path to the locally generated SSH public key registered with EC2."
  type        = string
  default     = "~/.ssh/devops-java-002.pub"
}
