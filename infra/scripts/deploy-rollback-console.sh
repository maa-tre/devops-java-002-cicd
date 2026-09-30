#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 1 ]]; then
  printf 'Usage: deploy-rollback-console.sh IMAGE_REF\n' >&2
  exit 2
fi

image_ref="$1"
container_name="java-app-rollback-console"
host_port=8081
internal_port=8080
app_env_file="/opt/devops-java-002/app.env"

if [[ ! "$image_ref" =~ ^java-app:ci-[0-9]+$ ]]; then
  printf 'Unexpected rollback console image reference: %s\n' "$image_ref" >&2
  exit 2
fi

if ! docker image inspect "$image_ref" >/dev/null 2>&1; then
  printf 'Rollback console image is not present locally: %s\n' "$image_ref" >&2
  exit 1
fi

if [[ ! -f "$app_env_file" || ! -r "$app_env_file" ]]; then
  printf 'Required application environment file is missing or unreadable: %s\n' "$app_env_file" >&2
  exit 1
fi

if [[ "$(stat -c '%u:%a' "$app_env_file")" != "0:600" ]]; then
  printf 'Application environment file must be owned by root with mode 0600: %s\n' "$app_env_file" >&2
  exit 1
fi

previous_image=""
if docker container inspect "$container_name" >/dev/null 2>&1; then
  previous_image="$(docker container inspect --format '{{.Config.Image}}' "$container_name")"
fi

if ! docker rm --force "$container_name" >/dev/null 2>&1; then
  if docker container inspect "$container_name" >/dev/null 2>&1; then
    printf 'Could not remove the existing rollback console container.\n' >&2
    exit 1
  fi
fi

start_console() {
  docker run --pull=never --detach \
    --name "$container_name" \
    --restart unless-stopped \
    --network private-net \
    --add-host host.docker.internal:host-gateway \
    --env-file "$app_env_file" \
    --publish "${host_port}:${internal_port}" \
    "$1" >/dev/null
}

wait_for_console() {
  local attempt

  for attempt in {1..45}; do
    if curl --fail --silent "http://127.0.0.1:${host_port}/deploy" | grep -q 'Choose a version to restore'; then
      return 0
    fi
    sleep 2
  done

  return 1
}

if ! start_console "$image_ref"; then
  printf 'Could not start the rollback console image %s.\n' "$image_ref" >&2
  if [[ -n "$previous_image" ]]; then
    start_console "$previous_image" || true
  fi
  exit 1
fi

if wait_for_console; then
  printf 'Rollback console healthy: %s is serving on port %s.\n' "$image_ref" "$host_port"
  exit 0
fi

printf 'Rollback console did not become healthy. Recent logs follow:\n' >&2
docker logs --tail 50 "$container_name" >&2 || true
docker rm --force "$container_name" >/dev/null
if [[ -n "$previous_image" ]]; then
  printf 'Restoring previous rollback console image %s.\n' "$previous_image" >&2
  start_console "$previous_image" || true
fi
exit 1
