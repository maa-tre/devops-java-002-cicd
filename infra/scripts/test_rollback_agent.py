import os
import runpy
import tempfile
import unittest
from email.message import Message
from io import BytesIO
from pathlib import Path
from unittest.mock import patch

with patch.dict(os.environ, {"ROLLBACK_AGENT_TOKEN": "unit-test-token"}):
    namespace = runpy.run_path(str(Path(__file__).with_name("rollback-agent.py")))
    read_request_payload = namespace["read_request_payload"]
    read_progress = namespace["read_progress"]


class FakeHandler:
    def __init__(self, headers, body):
        self.headers = Message()
        for name, value in headers.items():
            self.headers[name] = value
        self.rfile = BytesIO(body)


class ReadRequestPayloadTests(unittest.TestCase):
    def test_reads_content_length_json(self):
        body = b'{"image":"java-app:ci-1"}'
        handler = FakeHandler({"Content-Length": str(len(body))}, body)

        self.assertEqual({"image": "java-app:ci-1"}, read_request_payload(handler))

    def test_reads_chunked_json(self):
        first = b'{"image":'
        second = b'"java-app:ci-1"}'
        body = (
            f"{len(first):X};part=one\r\n".encode()
            + first
            + b"\r\n"
            + f"{len(second):X}\r\n".encode()
            + second
            + b"\r\n0\r\n\r\n"
        )
        handler = FakeHandler({"Transfer-Encoding": "chunked"}, body)

        self.assertEqual({"image": "java-app:ci-1"}, read_request_payload(handler))

    def test_rejects_oversized_chunk(self):
        handler = FakeHandler({"Transfer-Encoding": "chunked"}, b"801\r\n")

        with self.assertRaises(ValueError):
            read_request_payload(handler)

    def test_rejects_transfer_encoding_with_content_length(self):
        handler = FakeHandler(
            {"Transfer-Encoding": "chunked", "Content-Length": "1"},
            b"0\r\n\r\n",
        )

        with self.assertRaises(ValueError):
            read_request_payload(handler)

    def test_rejects_negative_chunk_size(self):
        handler = FakeHandler({"Transfer-Encoding": "chunked"}, b"-1\r\n")

        with self.assertRaises(ValueError):
            read_request_payload(handler)

    def test_reads_live_deployment_phase(self):
        progress_file = Path(self.enterContext(tempfile.TemporaryDirectory())) / "phase"
        progress_file.write_text("health_check\tChecking the application endpoint.\n", encoding="utf-8")

        self.assertEqual(
            ("health_check", "Checking the application endpoint."),
            read_progress(progress_file),
        )

    def test_ignores_incomplete_deployment_phase(self):
        progress_file = Path(self.enterContext(tempfile.TemporaryDirectory())) / "phase"
        progress_file.write_text("health_check\n", encoding="utf-8")

        self.assertIsNone(read_progress(progress_file))


if __name__ == "__main__":
    unittest.main()
