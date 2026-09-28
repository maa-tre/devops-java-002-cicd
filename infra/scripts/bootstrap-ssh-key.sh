#!/usr/bin/env bash
set -euo pipefail

key_path="${SSH_PRIVATE_KEY_PATH:-${HOME}/.ssh/devops-java-002}"
public_key_path="${key_path}.pub"

if ! command -v ssh-keygen >/dev/null 2>&1; then
  printf 'Required command not found: ssh-keygen\n' >&2
  exit 1
fi

mkdir -p "$(dirname "$key_path")"
chmod 700 "$(dirname "$key_path")"

if [[ ! -f "$key_path" ]]; then
  ssh-keygen -t ed25519 -C "devops-java-002" -f "$key_path" -N ""
elif [[ ! -f "$public_key_path" ]]; then
  ssh-keygen -y -f "$key_path" > "$public_key_path"
fi

chmod 600 "$key_path"
chmod 644 "$public_key_path"
printf 'SSH key ready.\nPrivate key: %s\nPublic key:  %s\n' "$key_path" "$public_key_path"
