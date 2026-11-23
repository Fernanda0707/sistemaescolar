"""
Microservicio de monitoreo HTTP para Sistema Escolar.
Puerto 8090; independiente de Java 8081. No requiere paquetes externos.
Ejecutar: python monitor_service.py
"""
from __future__ import annotations

import json
import os
import threading
import time
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

HOST = "127.0.0.1"
PORT = int(os.environ.get("HEALTH_PORT", "8090"))
SCHOOL = os.environ.get("SCHOOL_BASE_URL", "http://127.0.0.1:8081").rstrip("/")
INTERVAL_SECONDS = 10
TIMEOUT_SECONDS = 4
STARTED_AT = time.time()

# Cuatro endpoints distintos de la aplicación. No hacen cambios ni emiten facturas.
TARGETS = (
    ("escolar", "Sistema Escolar", "/", "Interfaz web principal"),
    ("academico", "Servicio Académico", "/api/academic/assignments", "Asignaciones de docentes"),
    ("clima", "Servicio de Clima", "/api/weather/current", "OpenWeatherMap mediante Java"),
    ("nominas", "Servicio de Nóminas", "/api/nominas/docente?query=salud", "Dependencia externa, puerto 8000"),
)

state_lock = threading.Lock()
refresh_lock = threading.Lock()
last_refresh = 0.0
records = {
    key: {
        "id": key, "name": name, "description": description,
        "status": "UNKNOWN", "latency_ms": None,
        "checks": 0, "successful_checks": 0, "failed_checks": 0,
        "last_check": None, "error": None, "history": [],
    }
    for key, name, _, description in TARGETS
}


def probe(target):
    key, name, path, description = target
    started = time.perf_counter()
    status_code = None
    failure = None
    try:
        req = Request(SCHOOL + path, headers={"Accept": "application/json,text/html"})
        with urlopen(req, timeout=TIMEOUT_SECONDS) as response:
            status_code = response.status
            # No es necesario descargar toda la respuesta; sólo comprobar HTTP.
            response.read(4096)
    except HTTPError as exc:
        status_code = exc.code
        failure = "HTTP " + str(status_code)
    except (URLError, TimeoutError, OSError) as exc:
        failure = type(exc).__name__ + ": " + str(exc.reason if isinstance(exc, URLError) else exc)[:130]
    except Exception as exc:
        failure = type(exc).__name__
    elapsed_ms = round((time.perf_counter() - started) * 1000, 1)
    ok = status_code is not None and 200 <= status_code < 300
    if not ok and not failure:
        failure = "HTTP " + str(status_code or "sin respuesta")
    return key, ok, elapsed_ms, failure


def fetch_real_metrics():
    try:
        with urlopen(Request(SCHOOL + "/api/health/metrics"), timeout=TIMEOUT_SECONDS) as response:
            if response.status != 200:
                return None
            obj = json.load(response)
            if isinstance(obj, dict):
                return obj.get("services")
    except (URLError, TimeoutError, OSError, ValueError, TypeError):
        pass
    return None


def refresh():
    global last_refresh
    with refresh_lock:
        if time.monotonic() - last_refresh < INTERVAL_SECONDS - 1:
            return
        with ThreadPoolExecutor(max_workers=4) as executor:
            results = list(executor.map(probe, TARGETS))
        now = datetime.now(timezone.utc).isoformat(timespec="seconds")
        with state_lock:
            for key, ok, latency, error in results:
                r = records[key]
                r["checks"] += 1
                r["successful_checks" if ok else "failed_checks"] += 1
                r["status"] = "UP" if ok else "DOWN"
                r["latency_ms"] = latency
                r["last_check"] = now
                r["error"] = error
                r["history"].append({"at": now, "latency_ms": latency, "up": ok})
                r["history"] = r["history"][-40:]
            last_refresh = time.monotonic()


def snapshot():
    refresh()
    real = fetch_real_metrics()
    with state_lock:
        services = []
        for key, _, _, _ in TARGETS:
            r = dict(records[key])
            r["history"] = list(records[key]["history"])
            r["uptime_percent"] = round(100 * r["successful_checks"] / r["checks"], 1) if r["checks"] else 0
            metrics = real.get(key) if isinstance(real, dict) else None
            r["real_requests"] = metrics.get("requests") if isinstance(metrics, dict) else None
            r["real_failures"] = metrics.get("failures") if isinstance(metrics, dict) else None
            r["avg_server_ms"] = metrics.get("avg_ms") if isinstance(metrics, dict) else None
            services.append(r)
    return {
        "monitor": {"name": "Health Status Monitor", "uptime_seconds": int(time.time()-STARTED_AT),
                    "poll_interval_seconds": INTERVAL_SECONDS, "updated_at": datetime.now(timezone.utc).isoformat(timespec="seconds"),
                    "metrics_available": real is not None},
        "services": services,
    }


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = self.path.split("?", 1)[0]
        if path == "/":
            filename = Path(__file__).resolve().with_name("health_dashboard.html")
            try:
                body = filename.read_bytes()
            except OSError:
                self.send_body(500, "text/plain; charset=utf-8", b"No se encontro health_dashboard.html")
                return
            self.send_body(200, "text/html; charset=utf-8", body)
        elif path == "/api/status":
            body = json.dumps(snapshot(), ensure_ascii=False).encode("utf-8")
            self.send_body(200, "application/json; charset=utf-8", body)
        elif path == "/health":
            self.send_body(200, "application/json", b'{"status":"UP","service":"health-monitor"}')
        else:
            self.send_body(404, "application/json", b'{"error":"Not found"}')

    def send_body(self, status, content_type, body):
        self.send_response(status)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Cache-Control", "no-store")
        self.send_header("X-Content-Type-Options", "nosniff")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, format, *args):
        return


if __name__ == "__main__":
    server = ThreadingHTTPServer((HOST, PORT), Handler)
    print(f"Health Status independiente: http://{HOST}:{PORT}/")
    print(f"Monitoreando 4 endpoints de {SCHOOL} (cada {INTERVAL_SECONDS} s)")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nMonitor detenido.")
    finally:
        server.server_close()
