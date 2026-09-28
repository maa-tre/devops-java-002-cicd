#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd -- "${script_dir}/../.." && pwd)"
terraform_dir="${repo_root}/infra/terraform"
tfvars_file="${terraform_dir}/terraform.tfvars"

for tool in terraform aws; do
  if ! command -v "$tool" >/dev/null 2>&1; then
    printf 'Required command not found: %s. Run infra/scripts/install-tools.sh first.\n' "$tool" >&2
    exit 1
  fi
done

if [[ ! -f "$tfvars_file" ]]; then
  printf 'Configuration not found: %s\nCopy terraform.tfvars.example and set allowed_ssh_cidrs first.\n' "$tfvars_file" >&2
  exit 1
fi

if ! aws sts get-caller-identity >/dev/null; then
  printf 'AWS credentials are unavailable. Configure them with aws configure or your approved credential provider.\n' >&2
  exit 1
fi

bash "${script_dir}/bootstrap-ssh-key.sh"
terraform -chdir="$terraform_dir" init -input=false
terraform -chdir="$terraform_dir" apply
bash "${script_dir}/configure-server.sh"

app_url="$(terraform -chdir="$terraform_dir" output -raw app_url)"
printf '\nHost setup complete. App URL: %s\n' "$app_url"
