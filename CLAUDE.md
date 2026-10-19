# CLAUDE.md

Guía para Claude Code (y para el equipo) al trabajar en este repositorio.

Agorá Catalog: catálogos públicos multiempresa que terminan en un pedido por WhatsApp, con un panel
que cada negocio maneja desde el celular. El qué está en `ESPECIFICACION.md` (fases en §13), el
cómo se ve en `DESIGN.md` y `PRODUCT.md`.

## Estructura

Monorepo con dos proyectos independientes que forman un solo producto:

```
.
├── agora-catalog-back/    Spring Boot 4 + JdbcTemplate + PostgreSQL + Flyway   → CLAUDE.md propio
├── agora-catalog-front/   Angular 21 standalone + Tailwind + Vitest            → CLAUDE.md propio
├── docker/web/            Caddy con el front compilado (modo Docker)
├── scripts/bd/            Utilidades de base de datos (chequeo de cambio de tipo)
├── scripts/respaldo/      Respaldo diario de la base y las fotos (producción)
├── .claude/rules/         Reglas que se activan solas según el archivo que se edite
├── .githooks/pre-push     Protecciones de migraciones (instalado por el build de ambos lados)
├── docker-compose.yml     Base + API + front en Docker, o solo la base
├── docker-compose.prod.yml  Producción en un VPS: base + API + Caddy con HTTPS
└── .github/workflows/     CI: build + pruebas + migraciones desde cero
```

**Cada subproyecto tiene su `CLAUDE.md` con las convenciones detalladas: leer el del lado que se
toca antes de escribir código.** Este archivo cubre solo lo que cruza los dos.

No hay herramienta de build raíz ni tipos compartidos: los contratos entre front y back (rutas,
nombres de campos JSON) se mantienen a mano. Por eso **un cambio que cruza los dos lados va en un
solo commit** (endpoint + pantalla). Correr `git` desde la raíz; las rutas en commits y docs,
relativas a la raíz.

### Nombres

| | Backend | Frontend |
|---|---|---|
| Carpeta | `agora-catalog-back/` | `agora-catalog-front/` |
| Proyecto | `settings.gradle` → `rootProject.name` | `angular.json` (clave + `outputPath` + `buildTarget`) y `package.json` |
| Artefacto | `build/libs/agora-catalog-back-<version>.jar` | `dist/agora-catalog-front/` |
| Paquete / prefijo | `com.agora.catalog` | `app` |
| Puerto local | `9092`, context-path `/api` | `4251` |

## Comandos

```bash
cd agora-catalog-back
cp .env.example .env                    # completar DB_*
bash gradlew bootRun                    # http://localhost:9092/api (aplica Flyway)
bash gradlew build                      # compila + pruebas; ojo: migra la base del .env

cd agora-catalog-front
npm install
npm start                               # http://localhost:4251
npm run build                           # build de producción: la verificación del front
npx ng test --watch=false               # Vitest

docker compose up -d --build            # todo en Docker: http://localhost:8080
docker compose up -d postgres           # solo la base (5433)
```

Las dos formas de correr (nativo y Docker) tienen que seguir funcionando: un cambio de puertos,
variables o rutas se refleja en `.env.example`, `docker-compose.yml`, `docker-compose.prod.yml`,
`docker/web/Caddyfile*`, `herramientas/servidor/serve_app.py` y el README a la vez.

Que existan las suites no reemplaza la verificación real: levantar el servicio, abrir la pantalla y
probarla en un celular (o a 390 px) además de en computador.

## Reglas que muerden

- **Multiempresa**: el `empresa_id` sale siempre del token; un recurso de otra empresa responde
  404. Toda ruta de `/admin` y `/superadmin` lleva `@PreAuthorize` con su rol
  (`.claude/rules/multiempresa.md`).
- **El catálogo público no filtra datos ocultos en el front**: si el negocio oculta precios, el
  backend no los envía.
- **Migraciones**: solo incrementales, con cabecera, inmutables una vez pusheadas
  (`.claude/rules/migraciones.md`). Se desarrolla contra la base local.
- **Secretos**: solo en `agora-catalog-back/.env` (ignorado por git) o en variables de entorno.
  Nunca en YAML ni en `environment.ts`.
- **`environment.ts` vs `environment.development.ts`**: nunca commitear una URL de `localhost` en el
  archivo que se despliega.
- **Idioma**: identificadores en inglés salvo los nombres de dominio del negocio que fija la
  especificación (`empresa`, `producto`, `categoria`...); texto para el usuario en español neutro
  con tuteo.
- **Listados del panel**: paginación y filtros en el backend (`PageResponse`).
