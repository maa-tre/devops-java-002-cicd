#!/usr/bin/env bash
set -euo pipefail
umask 077

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "${script_dir}/../.." && pwd)"
terraform_dir="${repo_root}/infra/terraform"
playbook="${repo_root}/infra/ansible/site.yml"
key_path="${SSH_PRIVATE_KEY_PATH:-${HOME}/.ssh/devops-java-002}"

for tool in terraform ansible-playbook ssh; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    printf 'Required command not found: %s\n' "$tool" >&2
    exit 1
  fi
done

if [[ ! -f "$key_path" ]]; then
  printf 'SSH private key not found: %s\nRun infra/scripts/bootstrap-ssh-key.sh first.\n' "$key_path" >&2
  exit 1
fi

if ! public_ip="$(terraform -chdir="$terraform_dir" output -raw public_ip)"; then
  printf 'Terraform outputs are unavailable. Provision the infrastructure first.\n' >&2
  exit 1
fi

ssh_access_configured="$(terraform -chdir="$terraform_dir" output -raw ssh_access_configured)"
if [[ "$ssh_access_configured" != "true" ]]; then
  printf 'SSH ingress is closed. Set allowed_ssh_cidrs to a trusted /32 in terraform.tfvars and apply Terraform first.\n' >&2
  exit 1
fi

if [[ -z "$public_ip" ]]; then
  printf 'Terraform returned an empty EC2 public IP.\n' >&2
  exit 1
fi

ssh_ready=false
for attempt in {1..30}; do
  if ssh \
    -o BatchMode=yes \
    -o ConnectTimeout=5 \
    -o StrictHostKeyChecking=accept-new \
    -i "$key_path" \
    "ubuntu@${public_ip}" true 2>/dev/null; then
    ssh_ready=true
    break
  fi
  sleep 10
done

if [[ "$ssh_ready" != "true" ]]; then
  printf 'SSH did not become available at %s. Check the SSH CIDR, keypair, and EC2 status.\n' "$public_ip" >&2
  exit 1
fi

ANSIBLE_SSH_ARGS="-o StrictHostKeyChecking=accept-new" \
  ansible-playbook \
  --inventory "${public_ip}," \
  --user ubuntu \
  --private-key "$key_path" \
  --become \
  "$playbook"
