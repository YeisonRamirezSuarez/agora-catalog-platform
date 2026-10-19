# Agorá Catalog

Plataforma SaaS multiempresa de catálogos digitales. Cada negocio publica su catálogo en un **link
público y permanente**, lo comparte por WhatsApp y el visitante arma su pedido y lo envía por
WhatsApp. El dueño administra productos, precios, ofertas y fotos desde un panel, a mano o desde
Excel.

| | |
|---|---|
| Backend | Java 21 · Spring Boot 4 · Spring Security · JdbcTemplate · PostgreSQL 16+ · Flyway |
| Frontend | Angular 21 (standalone) · Tailwind 3 · Vitest |

Alcance, modelo de datos, API y plan por fases: [`ESPECIFICACION.md`](ESPECIFICACION.md). Sistema
visual: [`DESIGN.md`](DESIGN.md). Prototipo navegable: [`docs/prototipo/index.html`](docs/prototipo/index.html).

## Estructura

```
.
├── agora-catalog-back/     Spring Boot 4 + JdbcTemplate + Flyway   → CLAUDE.md propio, .env.example
├── agora-catalog-front/    Angular 21 standalone + Tailwind + Vitest → CLAUDE.md propio
├── docker/web/             Imagen del front con Caddy (sirve el front y reenvía /api)
├── docs/prototipo/         Prototipo HTML navegable y kit de marca
├── herramientas/           Generadores del prototipo, la marca y sus pruebas
├── scripts/bd/             Utilidades de base de datos
├── .claude/rules/          Reglas de migraciones y multiempresa
├── .githooks/pre-push      Protecciones de migraciones (lo instala el build de ambos lados)
├── .github/workflows/      CI: build + pruebas + migraciones desde cero
└── docker-compose.yml      Todo en Docker, o solo la base
```

## Arranque

Se puede correr de dos formas. Las dos usan los mismos puertos de la aplicación.

| | Nativo | Docker |
|---|---|---|
| Front | http://localhost:4251 | http://localhost:8080 |
| API | http://localhost:9092/api | http://localhost:8080/api |
| Swagger | http://localhost:9092/api/swagger-ui.html | (perfil `dev`, solo dentro de la red de Docker) |
| Base | PostgreSQL de la máquina (5432) | contenedor en el 5433 |

### Opción 1: nativo

Requisitos: JDK 21, Node 22.12+ (24 recomendado), PostgreSQL 16 o superior.

```bash
# 1. Base de datos (una sola vez)
psql -U postgres -c "CREATE DATABASE agora_catalog"

# 2. Backend (aplica las migraciones al arrancar)
cd agora-catalog-back
cp .env.example .env        # completar DB_PASSWORD
bash gradlew bootRun        # http://localhost:9092/api

# 3. Frontend
cd ../agora-catalog-front
npm install
npm start                   # http://localhost:4251 (el proxy manda /api al backend)
```

¿Sin PostgreSQL instalado? `docker compose up -d postgres` levanta solo la base en el 5433;
en el `.env` del backend poner `DB_URL='jdbc:postgresql://localhost:5433/agora_catalog?currentSchema=main'`
y `DB_PASSWORD='postgres'`.

### Opción 2: todo en Docker

Requisitos: Docker.

```bash
docker compose up -d --build   # base + API + front en http://localhost:8080
docker compose logs -f api     # ver el arranque del backend
docker compose down            # detener (los datos quedan en el volumen)
```

Si el 8080 o el 5433 están ocupados: `WEB_PORT=8081 DB_PORT=5434 docker compose up -d --build`.

### Publicar desde esta máquina

`herramientas/servidor/serve_app.py` sirve el front compilado y reenvía `/api` y `/c/...` al backend en
un solo puerto, por HTTP y HTTPS (con certificado). Solo usa la biblioteca estándar de Python.

```bash
cd agora-catalog-front && npm run build
cd ../agora-catalog-back && bash gradlew bootJar
# Escucha solo en 127.0.0.1:9092. FRONTEND_URL es la dirección pública (enlaces de correos y vista previa).
SPRING_PROFILES_ACTIVE=prod TRUSTED_PROXY_COUNT=1 FRONTEND_URL=https://mi-dominio:2020 \
  CORS_ALLOWED_ORIGINS=https://mi-dominio:2020 \
  FRONT_INDEX_PATH=../agora-catalog-front/dist/agora-catalog-front/browser/index.html \
  java -jar build/libs/agora-catalog-back-0.1.0.jar
python herramientas/servidor/serve_app.py agora-catalog-front/dist/agora-catalog-front/browser 2020 cert.crt cert.key
```

### Producción en un VPS (Docker + Caddy con HTTPS)

Requisitos: un VPS con Docker, un dominio con su registro A apuntando al servidor y los puertos 80 y
443 abiertos. Caddy saca y renueva el certificado de Let's Encrypt solo.

```bash
git clone https://github.com/YeisonRamirezSuarez/agora-catalog-platform.git /opt/agora-catalog-platform
cd /opt/agora-catalog-platform
cp .env.prod.example .env       # completar DOMAIN, DB_PASSWORD, JWT_SECRET, AES_KEY y SUPERADMIN_*
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml logs -f api
```

Para actualizar: `git pull && docker compose -f docker-compose.prod.yml up -d --build`. Solo Caddy
publica puertos; la base y la API quedan dentro de la red de Docker. En producción no se cargan los
datos de prueba.

### Vista previa del link en WhatsApp

El link de cada catálogo (`/c/{slug}-{hash}`) lo atiende el backend (`GET /api/public/paginas/...`):
devuelve el `index.html` del front (`FRONT_INDEX_PATH`) con las etiquetas Open Graph del negocio
(nombre, texto de la portada e imagen). La imagen es la portada en una versión liviana de 1200 px
(WhatsApp no muestra imágenes pesadas) o, sin portada, el logo. Si el slug del link no es el actual,
responde 301 al link correcto: los links viejos siguen sirviendo. En desarrollo (`npm start`) `/c/` lo
atiende Angular y no hay vista previa.

## Respaldos

`scripts/respaldo/respaldar.sh` guarda la base (`pg_dump`) **y** la carpeta de fotos: una sin la otra
no sirve. Se programa una vez al día con cron en el VPS:

```bash
crontab -e
30 3 * * * cd /opt/agora-catalog-platform && sh scripts/respaldo/respaldar.sh >> /var/log/agora-respaldo.log 2>&1
```

Guarda 14 días en `/var/backups/agora-catalog` (`RESPALDO_DIR`, `RESPALDO_DIAS`). Para la copia fuera
del VPS, configurar [rclone](https://rclone.org) (`rclone config`, por ejemplo con Cloudflare R2 o
Google Drive) y poner el destino en `RESPALDO_REMOTO` del `.env`: sin ella, si se pierde el servidor
se pierden también los respaldos.

Restaurar (con la API detenida):

```bash
docker compose -f docker-compose.prod.yml stop api
docker compose -f docker-compose.prod.yml exec -T postgres sh -c 'pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists' < bd-AAAA-MM-DD_HHMM.dump
docker run --rm -i -v agora-catalog-prod-uploads:/datos alpine tar xzf - -C /datos < fotos-AAAA-MM-DD_HHMM.tar.gz
docker compose -f docker-compose.prod.yml start api
```

## Usuarios

- **Super admin**: se crea al arrancar con `SUPERADMIN_NAME`, `SUPERADMIN_EMAIL` y `SUPERADMIN_PASSWORD`
  del `.env` (o del entorno en Docker), si ese correo todavía no existe. Al entrar se le pide cambiar
  la contraseña. Desde **Empresas** crea cada negocio con su administrador, que recibe una contraseña
  temporal.
- **Datos de prueba** (solo perfil local y Docker): la empresa Salsamentaria Javier con 25 productos y
  su administrador `javier@agora.local` / `Agora123*`.

## Verificación

```bash
cd agora-catalog-back && bash gradlew build      # compila y corre las pruebas (necesita la base del .env)
cd agora-catalog-front && npm run build && npx ng test --watch=false
```

## Documentación

- [`CLAUDE.md`](CLAUDE.md): reglas que cruzan front y back.
- [`agora-catalog-back/CLAUDE.md`](agora-catalog-back/CLAUDE.md): convenciones del backend.
- [`agora-catalog-front/CLAUDE.md`](agora-catalog-front/CLAUDE.md): convenciones del frontend.
- [`.claude/rules/`](.claude/rules/README.md): migraciones y multiempresa.
