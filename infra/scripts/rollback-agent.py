#!/usr/bin/env python3
import hmac
import json
import os
import re
import subprocess
import threading
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

IMAGE_PATTERN = re.compile(r"^java-app:ci-([0-9]+)$")
STATE_DIR = Path(os.environ.get("ROLLBACK_AGENT_STATE_DIR", "/var/lib/devops-java-002/rollback-requests"))
TOKEN = os.environ["ROLLBACK_AGENT_TOKEN"]
PORT = int(os.environ.get("ROLLBACK_AGENT_PORT", "8790"))
HOST = os.environ.get("ROLLBACK_AGENT_BIND", "127.0.0.1")
operation_lock = threading.Lock()


def inspect_current_image():
    result = subprocess.run(
        ["docker", "inspect", "--format={{.Config.Image}}", "java-app"],
        check=False,
        capture_output=True,
        text=True,
    )
    return result.stdout.strip() if result.returncode == 0 else None


def available_images():
    result = subprocess.run(
        ["docker", "image", "ls", "--format={{.Repository}}:{{.Tag}}"],
        check=True,
        capture_output=True,
        text=True,
    )
    found = {}
    for value in result.stdout.splitlines():
        match = IMAGE_PATTERN.fullmatch(value.strip())
        if match:
            found[value] = int(match.group(1))
    return sorted(found, key=lambda value: found[value], reverse=True)


def write_state(request_id, state):
    STATE_DIR.mkdir(parents=True, exist_ok=True, mode=0o700)
    state.setdefault("updatedAt", datetime.now(timezone.utc).isoformat())
    temporary = STATE_DIR / (request_id + ".tmp")
    destination = STATE_DIR / (request_id + ".json")
    temporary.write_text(json.dumps(state), encoding="utf-8")
    temporary.chmod(0o600)
    temporary.replace(destination)


def read_state(request_id):
    if not re.fullmatch(r"[0-9a-f-]{36}", request_id):
        return None
    try:
        return json.loads((STATE_DIR / (request_id + ".json")).read_text(encoding="utf-8"))
    except FileNotFoundError:
        return None


def read_request_payload(handler):
    transfer_encoding = handler.headers.get("Transfer-Encoding", "").strip().lower()
    content_length = handler.headers.get("Content-Length")
    if transfer_encoding:
        if transfer_encoding != "chunked" or content_length is not None:
            raise ValueError("Unsupported request transfer encoding")
        body = bytearray()
        while True:
            line = handler.rfile.readline(128)
            if not line.endswith(b"\r\n"):
                raise ValueError("Invalid chunk header")
            try:
                chunk_size = int(line[:-2].split(b";", 1)[0], 16)
            except ValueError as ex:
                raise ValueError("Invalid chunk size") from ex
            if chunk_size < 0:
                raise ValueError("Invalid chunk size")
            if chunk_size == 0:
                while True:
                    trailer = handler.rfile.readline(2048)
                    if trailer in (b"\r\n", b"\n"):
                        break
                    if not trailer:
                        raise ValueError("Incomplete chunk trailer")
                break
            if len(body) + chunk_size > 2048:
                raise ValueError("Request body is too large")
            chunk = handler.rfile.read(chunk_size)
            if len(chunk) != chunk_size or handler.rfile.read(2) != b"\r\n":
                raise ValueError("Incomplete request chunk")
            body.extend(chunk)
    else:
        try:
            length = int(content_length or "0")
        except ValueError as ex:
            raise ValueError("Invalid content length") from ex
        if length < 1 or length > 2048:
            raise ValueError("Invalid content length")
        body = handler.rfile.read(length)
        if len(body) != length:
            raise ValueError("Incomplete request body")
    return json.loads(body)


def execute_rollback(request_id, image):
    try:
        write_state(request_id, {
            "id": request_id,
            "image": image,
            "status": "running",
            "phase": "starting",
            "startedAt": datetime.now(timezone.utc).isoformat(),
            "message": "Starting the selected image. The health check can take up to 60 seconds.",
        })
        result = subprocess.run(
            ["/opt/devops-java-002/deploy-app.sh", image, "8080"],
            check=False,
            capture_output=True,
            text=True,
            timeout=100,
        )
        lines = (result.stdout + "\n" + result.stderr).splitlines()
        message = next(
            (line.strip() for line in reversed(lines) if line.strip()),
            "Rollback command finished.",
        )
        write_state(request_id, {
            "id": request_id,
            "image": image,
            "status": "succeeded" if result.returncode == 0 else "failed",
            "phase": "complete" if result.returncode == 0 else "restored",
            "completedAt": datetime.now(timezone.utc).isoformat(),
            "message": message[:500],
        })
    except subprocess.TimeoutExpired:
        write_state(request_id, {
            "id": request_id,
            "image": image,
            "status": "failed",
            "phase": "restored",
            "completedAt": datetime.now(timezone.utc).isoformat(),
            "message": "Rollback timed out; inspect EC2 deployment logs and current health.",
        })
    except Exception:
        write_state(request_id, {
            "id": request_id,
            "image": image,
            "status": "failed",
            "phase": "error",
            "completedAt": datetime.now(timezone.utc).isoformat(),
            "message": "Rollback helper failed; inspect the EC2 system journal.",
        })
    finally:
        operation_lock.release()


class Handler(BaseHTTPRequestHandler):
    server_version = "RollbackAgent/1.0"

    def log_message(self, fmt, *args):
        return

    def respond(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def authorized(self):
        supplied = self.headers.get("Authorization", "")
        expected = "Bearer " + TOKEN
        return hmac.compare_digest(supplied.encode("utf-8"), expected.encode("utf-8"))

    def do_GET(self):
        if not self.authorized():
            return self.respond(401, {"error": "Unauthorized"})
        path = urlparse(self.path).path
        if path == "/v1/images":
            current = inspect_current_image()
            return self.respond(200, {
                "current": current,
                "images": [image for image in available_images() if image != current],
            })
        if path.startswith("/v1/requests/"):
            state = read_state(path.rsplit("/", 1)[-1])
            return self.respond(200, state) if state else self.respond(404, {"error": "Request not found"})
        return self.respond(404, {"error": "Not found"})

    def do_POST(self):
        if not self.authorized():
            return self.respond(401, {"error": "Unauthorized"})
        if urlparse(self.path).path != "/v1/rollback":
            return self.respond(404, {"error": "Not found"})
        try:
            payload = read_request_payload(self)
            if not isinstance(payload, dict):
                return self.respond(400, {"error": "Invalid request"})
            image = payload.get("image", "")
            request_id = payload.get("id", "")
            expected_current = payload.get("expectedCurrent", "")
            if not isinstance(image, str) or not IMAGE_PATTERN.fullmatch(image):
                return self.respond(400, {"error": "Invalid image reference"})
            if not isinstance(request_id, str) or not re.fullmatch(r"[0-9a-f-]{36}", request_id):
                return self.respond(400, {"error": "Invalid request id"})
            current = inspect_current_image()
            if not current or expected_current != current:
                return self.respond(409, {"error": "The running image changed; refresh rollback options"})
            if image == current:
                return self.respond(409, {"error": "Selected image is already running"})
            if image not in available_images():
                return self.respond(409, {"error": "Selected image is no longer available on EC2"})
            if not operation_lock.acquire(blocking=False):
                return self.respond(409, {"error": "Another rollback is already running"})
            try:
                write_state(request_id, {
                    "id": request_id,
                    "image": image,
                    "status": "queued",
                    "phase": "validating",
                    "message": "Image is present on EC2; preparing the health-checked rollback.",
                })
                threading.Thread(
                    target=execute_rollback,
                    args=(request_id, image),
                    name="rollback-" + request_id,
                    daemon=True,
                ).start()
            except Exception:
                operation_lock.release()
                raise
            return self.respond(202, {"id": request_id})
        except (ValueError, TypeError, json.JSONDecodeError):
            return self.respond(400, {"error": "Invalid request"})
        except Exception:
            return self.respond(500, {"error": "Rollback request failed"})


if __name__ == "__main__":
    STATE_DIR.mkdir(parents=True, exist_ok=True, mode=0o700)
    ThreadingHTTPServer((HOST, PORT), Handler).serve_forever()
