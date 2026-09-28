#!/usr/bin/env bash
set -euo pipefail

if [[ ! -f /etc/os-release ]]; then
  printf 'Cannot identify this Linux distribution.\n' >&2
  exit 1
fi

# shellcheck disable=SC1091
source /etc/os-release
if [[ "${ID:-}" != "ubuntu" ]]; then
  printf 'This installer supports Ubuntu/WSL Ubuntu only.\n' >&2
  exit 1
fi

sudo_cmd=()
if [[ "$EUID" -ne 0 ]]; then
  if ! command -v sudo >/dev/null 2>&1; then
    printf 'sudo is required to install the automation tools.\n' >&2
    exit 1
  fi
  sudo_cmd=(sudo)
fi

key_file="$(mktemp)"
trap 'rm -f "$key_file"' EXIT

"${sudo_cmd[@]}" apt-get update
"${sudo_cmd[@]}" apt-get install -y ansible awscli ca-certificates curl gnupg openssh-client

curl -fsSL https://apt.releases.hashicorp.com/gpg | gpg --dearmor > "$key_file"
"${sudo_cmd[@]}" install -D -o root -g root -m 0644 "$key_file" /usr/share/keyrings/hashicorp-archive-keyring.gpg

printf 'deb [arch=%s signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com %s main\n' \
  "$(dpkg --print-architecture)" "$VERSION_CODENAME" |
  "${sudo_cmd[@]}" tee /etc/apt/sources.list.d/hashicorp.list >/dev/null

"${sudo_cmd[@]}" apt-get update
"${sudo_cmd[@]}" apt-get install -y terraform

terraform version
ansible-playbook --version
aws --version
