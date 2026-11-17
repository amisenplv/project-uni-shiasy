from http.client import HTTPConnection
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from sys import argv
from time import sleep

root = Path(argv[1]).resolve(strict=True)
backend_port = int(argv[2])
if backend_port == 8765 or not 1024 <= backend_port <= 65535:
    raise SystemExit("choose a separate local backend port")

class Bridge(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        self.forward()

    do_PUT = do_POST = do_GET

    def forward(self):
        # Record fixed routes and result codes only. No request headers or bytes.
        if self.path not in {"/v1/auth/register", "/v1/auth/login", "/v1/auth/logout", "/v1/auth/recover",
                             "/v1/backup/metadata", "/v1/backup"}:
            self.send_error(404)
            return
        mode = (root / "proxy-mode").read_text().strip()
        with (root / "proxy-events").open("a") as stream:
            stream.write(f"request {self.command} {self.path}\n")
        length = int(self.headers.get("content-length", "0"))
        if not 0 <= length <= 4 * 1024 * 1024:
            self.send_error(413)
            return
        body = self.rfile.read(length) if length else None
        if mode == "drop-before-put" and self.command == "PUT":
            self.close_connection = True
            return
        connection = HTTPConnection("127.0.0.1", backend_port, timeout=12)
        try:
            headers = {key: value for key, value in self.headers.items()
                       if key.lower() not in {"connection", "host"}}
            connection.request(self.command, self.path, body=body, headers=headers)
            response = connection.getresponse()
            payload = response.read()
            with (root / "proxy-events").open("a") as stream:
                stream.write(f"result {self.command} {response.status}\n")
            if mode in {"hold-put", "drop-after-put"} and self.command == "PUT" and response.status == 200:
                (root / "put-committed").touch()
                if mode == "hold-put":
                    for _ in range(100):
                        if (root / "proxy-mode").read_text().strip() != "hold-put":
                            break
                        sleep(0.1)
                self.close_connection = True
                return
            snapshot = self.command == "GET" and self.path == "/v1/backup" and response.status == 200
            if snapshot and mode == "hold-get":
                (root / "get-ready").touch()
                for _ in range(220):
                    if (root / "proxy-mode").read_text().strip() != "hold-get":
                        break
                    sleep(0.1)
                self.close_connection = True
                return
            content_length = len(payload)
            if snapshot and mode == "truncate-get":
                payload = payload[:len(payload) // 2]
                self.close_connection = True
            if snapshot and mode == "oversize-get":
                content_length = 4 * 1024 * 1024 + 1
                payload = b""
                self.close_connection = True
            if snapshot and mode == "corrupt-get":
                payload = b"[" + payload[1:]
            self.send_response(response.status)
            for key, value in response.getheaders():
                if key.lower() not in {"content-length", "connection", "transfer-encoding", "server", "date"}:
                    self.send_header(key, value)
            self.send_header("Content-Length", str(content_length))
            self.end_headers()
            self.wfile.write(payload)
        except (BrokenPipeError, ConnectionResetError):
            pass
        finally:
            connection.close()

    def log_message(self, *args):
        pass

ThreadingHTTPServer(("127.0.0.1", 8765), Bridge).serve_forever()
