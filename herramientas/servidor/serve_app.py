# Sirve Agorá Catalog desde esta máquina: el front compilado (dist) y la API, en un solo puerto,
# por HTTP y, si se le da certificado, también por HTTPS. Mira el primer byte de cada conexión
# (0x16 = inicio de TLS) y solo cifra las que llegan con TLS.
#
#   python serve_app.py <carpeta_dist_browser> <puerto> [cert.pem key.pem] [--api http://127.0.0.1:9092]
#
# - /api/...      se reenvía al backend, con X-Forwarded-For = IP del visitante (el backend debe
#                 correr con TRUSTED_PROXY_COUNT=1 para limitar peticiones por visitante).
# - lo demás      archivos del front; una ruta que no es un archivo devuelve index.html (rutas de Angular).
# Solo biblioteca estándar: corre con el Python de la máquina sin instalar nada.
import functools
import http.client
import http.server
import json
import os
import socket
import ssl
import sys
from urllib.parse import urlsplit

args = [a for a in sys.argv[1:] if not a.startswith('--api')]
api_flag = next((a for a in sys.argv[1:] if a.startswith('--api')), '--api=http://127.0.0.1:9092')
API = urlsplit(api_flag.split('=', 1)[1] if '=' in api_flag else sys.argv[sys.argv.index(api_flag) + 1])
folder, port = os.path.abspath(args[0]), int(args[1])
ctx = None
if len(args) > 3:
    ctx = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    ctx.load_cert_chain(args[2], args[3])

# No se reenvían: son de esta conexión, no del mensaje (RFC 9110 §7.6.1), o los pone este proxy.
HOP = {'connection', 'keep-alive', 'proxy-connection', 'transfer-encoding', 'te', 'trailer', 'upgrade',
       'host', 'x-forwarded-for', 'x-forwarded-proto', 'x-forwarded-host', 'content-length'}
MAX_BODY = 60 * 1024 * 1024  # el ZIP de imágenes más grande que acepta la API (50 MB) con margen


class Handler(http.server.SimpleHTTPRequestHandler):
    protocol_version = 'HTTP/1.1'

    def log_request(self, code='-', size='-'):
        self.log_message('"%s" %s ip=%s ua="%s"', self.requestline, code, self.client_address[0],
                         self.headers.get('User-Agent', '-') if self.headers else '-')

    def end_headers(self):
        self.send_header('X-Content-Type-Options', 'nosniff')
        self.send_header('Referrer-Policy', 'strict-origin-when-cross-origin')
        super().end_headers()

    # ---------- API ----------
    def _proxy(self):
        length = int(self.headers.get('Content-Length') or 0)
        if length > MAX_BODY:
            return self._json(413, 'Solicitud inválida', 'El archivo es demasiado grande.')
        body = self.rfile.read(length) if length else None
        headers = {k: v for k, v in self.headers.items() if k.lower() not in HOP}
        headers['X-Forwarded-For'] = self.client_address[0]
        headers['X-Forwarded-Proto'] = 'https' if isinstance(self.connection, ssl.SSLSocket) else 'http'
        headers['X-Forwarded-Host'] = self.headers.get('Host', '')
        if body is not None:
            headers['Content-Length'] = str(len(body))
        try:
            conn = http.client.HTTPConnection(API.hostname, API.port or 80, timeout=120)
            conn.request(self.command, self.path, body=body, headers=headers)
            resp = conn.getresponse()
            data = resp.read()
        except OSError:
            return self._json(502, 'Servicio no disponible', 'El servidor no responde. Intenta de nuevo en un momento.')
        self.send_response(resp.status, resp.reason)
        for k, v in resp.getheaders():
            if k.lower() not in HOP:
                self.send_header(k, v)
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        if self.command != 'HEAD':
            self.wfile.write(data)
        conn.close()

    def _json(self, status, error, message):
        data = json.dumps({'error': error, 'message': message}, ensure_ascii=False).encode('utf-8')
        self.send_response(status)
        self.send_header('Content-Type', 'application/json;charset=UTF-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _is_api(self):
        return self.path == '/api' or self.path.startswith('/api/')

    # ---------- Front ----------
    def send_head(self):
        path = self.translate_path(self.path)
        # Nada fuera de la carpeta, ni listados de carpetas: lo que no es un archivo es una ruta de Angular.
        if not os.path.isfile(path) or not os.path.realpath(path).startswith(folder):
            self.path = '/index.html'
        return super().send_head()

    def send_response(self, code, message=None):
        super().send_response(code, message)
        if not self._is_api():
            # index.html siempre fresco (apunta a los archivos nuevos tras cada build); el resto lleva hash en el nombre.
            self.send_header('Cache-Control', 'no-cache' if self.path.endswith('index.html') or self.path == '/'
                             else 'public, max-age=31536000, immutable')

    def do_GET(self):
        self._proxy() if self._is_api() else super().do_GET()

    def do_HEAD(self):
        self._proxy() if self._is_api() else super().do_HEAD()

    def _api_only(self):
        self._proxy() if self._is_api() else self._json(405, 'Método no permitido', 'Ruta no disponible.')

    do_POST = do_PUT = do_PATCH = do_DELETE = do_OPTIONS = _api_only


class DualServer(http.server.ThreadingHTTPServer):
    daemon_threads = True

    def finish_request(self, request, client_address):
        request.settimeout(30)  # conexiones abiertas sin pedir nada no dejan hilos colgados
        tls = False
        try:
            if ctx and request.recv(1, socket.MSG_PEEK) == b'\x16':
                request, tls = ctx.wrap_socket(request, server_side=True), True
        except (OSError, ssl.SSLError):
            return
        try:
            self.RequestHandlerClass(request, client_address, self)
        finally:
            if tls:
                request.close()  # el socket original quedó separado; el cifrado se cierra aquí


if not os.path.isfile(os.path.join(folder, 'index.html')):
    sys.exit(f'No hay index.html en {folder}: compila el front (npm run build) y pasa dist/agora-catalog-front/browser')

with DualServer(('0.0.0.0', port), functools.partial(Handler, directory=folder)) as srv:
    print(f'puerto {port}: HTTP' + (' y HTTPS' if ctx else '') + f'; /api -> {API.geturl()}', flush=True)
    srv.serve_forever()
