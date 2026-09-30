#!/usr/bin/env python3
import base64
import hashlib
import secrets
import sys

pin = sys.stdin.readline().rstrip("\r\n")
if len(pin) < 8 or not pin.isdigit():
    raise SystemExit("Rollback PIN must contain at least 8 digits.")

salt = secrets.token_bytes(16)
iterations = 310_000
digest = hashlib.pbkdf2_hmac("sha256", pin.encode("utf-8"), salt, iterations)
print(
    "pbkdf2_sha256${}${}${}".format(
        iterations,
        base64.b64encode(salt).decode("ascii"),
        base64.b64encode(digest).decode("ascii"),
    )
)
