output "instance_id" {
  description = "EC2 instance ID."
  value       = aws_instance.demo.id
}

output "public_ip" {
  description = "Public IPv4 address used by Ansible and the deployment pipeline."
  value       = aws_instance.demo.public_ip
}

output "app_url" {
  description = "Public URL for the Java application after it is deployed."
  value       = "http://${aws_instance.demo.public_ip}:${var.app_port}"
}

output "rollback_console_url" {
  description = "Public demo rollback console URL."
  value       = "http://${aws_instance.demo.public_ip}:${var.rollback_console_port}/deploy"
}

output "ssh_access_configured" {
  description = "Whether at least one restricted SSH source CIDR was configured."
  value       = length(var.allowed_ssh_cidrs) > 0
}
