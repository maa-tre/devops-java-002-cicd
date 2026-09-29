#!/usr/bin/env bash
set -euo pipefail

if [[ $# -ne 2 ]]; then
  printf 'Usage: deploy-app.sh IMAGE_REF HOST_PORT\n' >&2
  exit 2
fi

image_ref="$1"
host_port="$2"
container_name="java-app"
internal_port=8080
app_env_file="/opt/devops-java-002/app.env"

wait_for_health() {
  local attempt

  for attempt in {1..30}; do
    if curl --fail --silent "http://127.0.0.1:${host_port}/" >/dev/null 2>&1; then
      return 0
    fi
    sleep 2
  done

  return 1
}

if [[ ! "$image_ref" =~ ^java-app:ci-[0-9]+$ ]]; then
  printf 'Unexpected application image reference: %s\n' "$image_ref" >&2
  exit 2
fi

if [[ ! "$host_port" =~ ^[0-9]+$ ]] || (( host_port < 1 || host_port > 65535 )); then
  printf 'Invalid host port: %s\n' "$host_port" >&2
  exit 2
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
    printf 'Could not remove the existing %s container.\n' "$container_name" >&2
    exit 1
  fi
fi

restore_previous() {
  if [[ -z "$previous_image" ]]; then
    printf 'No previous container image is available for rollback.\n' >&2
    return 1
  fi

  printf 'Restoring previous image %s.\n' "$previous_image" >&2
  docker run --pull=never --detach \
    --name "$container_name" \
    --restart unless-stopped \
    --network private-net \
    --env-file "$app_env_file" \
    --publish "${host_port}:${internal_port}" \
    "$previous_image" >/dev/null
  if ! wait_for_health; then
    printf 'The previous image did not recover successfully.\n' >&2
    docker logs --tail 50 "$container_name" >&2 || true
    return 1
  fi
}

if ! docker run --pull=never --detach \
  --name "$container_name" \
  --restart unless-stopped \
  --network private-net \
  --env-file "$app_env_file" \
  --publish "${host_port}:${internal_port}" \
  "$image_ref"; then
  if ! restore_previous; then
    printf 'Rollback failed after the new image could not start.\n' >&2
  fi
  exit 1
fi

if wait_for_health; then
  printf 'Deployment healthy: %s is serving on port %s.\n' "$image_ref" "$host_port"
  exit 0
fi

printf 'New container did not become healthy. Recent logs follow:\n' >&2
docker logs --tail 50 "$container_name" >&2 || true
docker rm --force "$container_name" >/dev/null
restore_previous
exit 1
