# Agorá Catalog (agora-catalog-platform) — Catálogo digital multiempresa

> Versión 3 (2026-10-16). Incluye las decisiones de la revisión técnica y las del diseño que pidió el cliente (sección 14).
> Prototipo navegable de todas las pantallas: [`docs/prototipo/index.html`](docs/prototipo/index.html).

## 1. Qué es

Plataforma SaaS propia donde cada negocio (empresa) publica su catálogo de productos en un **link público, permanente y sin login**, que comparte por WhatsApp. El dueño administra productos, categorías, precios y promociones desde un **panel**, cargándolos a mano o desde **Excel**.

- **NO** es inventario ni facturación (el cliente ya tiene eso).
- **NO** hay pagos ni cuentas de clientes. El visitante arma un **pedido por WhatsApp**: suma productos con su cantidad, la lista se guarda solo en su navegador y se envía como un mensaje de WhatsApp con cantidades y total. El negocio confirma el total y el domicilio por WhatsApp. El servidor no guarda pedidos.
- Primer cliente: **Salsamentaria Javier** (embutidos, quesos, lácteos, carnes frías). Debe quedar personalizable para futuras empresas.

## 2. Stack

| Capa | Tecnología |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Gradle, Spring Security + JWT (access + refresh), **JdbcTemplate** (sin JPA), Flyway, Bean Validation, springdoc-openapi (Swagger), Lombok |
| Excel | Apache POI (`poi-ooxml`), con un `common/export` propio |
| Imágenes | Thumbnailator. Entrada JPG, PNG o WebP (WebP se lee con `com.twelvemonkeys.imageio:imageio-webp`). **Salida siempre JPG** |
| Base de datos | PostgreSQL 16 o superior, esquema `main` |
| Frontend | Angular 21 standalone, signals, Tailwind 3, Vitest, Node 24. **CSR** (sin SSR, ver §5) |
| Archivos | `StorageService` sobre el disco local del servidor, servido como estático. Una sola implementación (ver §14, decisión 10) |
| Infra local | PostgreSQL nativo o `docker-compose.yml` (PostgreSQL 16, API y front con Caddy) |

### Estructura del monorepo

```
agora-catalog-platform/
├── agora-catalog-back/      Spring Boot 4 + JdbcTemplate + Flyway     → CLAUDE.md propio, .env.example, Dockerfile
├── agora-catalog-front/     Angular 21 standalone + Tailwind + Vitest  → CLAUDE.md propio
├── docker/web/              Caddy con el front compilado (modo Docker)
├── docs/prototipo/          Prototipo HTML navegable (fase 0)
├── herramientas/            Generadores del prototipo y de la marca
├── scripts/bd/              Utilidades de base de datos
├── .claude/rules/           Reglas de migraciones y multiempresa
├── .githooks/pre-push       Protección de migraciones Flyway
├── .github/workflows/       CI: build + pruebas + migraciones desde cero
├── .gitattributes           LF para .sh, .yml, .yaml, gradlew, Caddyfile y Dockerfile
├── .gitignore
├── CLAUDE.md                Reglas que cruzan front y back
├── README.md                Cómo levantar todo en local (nativo o Docker)
├── ESPECIFICACION.md
├── docker-compose.yml       Todo en Docker (base, API y front) o solo la base
└── docker-compose.prod.yml  api + PostgreSQL + Caddy (fase 8)
```

| | Backend | Frontend |
|---|---|---|
| Carpeta | `agora-catalog-back/` | `agora-catalog-front/` |
| Proyecto | `settings.gradle` → `rootProject.name = 'agora-catalog-back'` | `angular.json` y `package.json` → `agora-catalog-front` |
| Artefacto | `build/libs/agora-catalog-back-<version>.jar` | `dist/agora-catalog-front/` |
| Paquete / prefijo | `com.agora.catalog` | `app` |
| Puerto local | `9092`, context-path `/api` | `4251` |

Se corre de dos formas: **nativo** (PostgreSQL de la máquina, `gradlew bootRun`, `npm start`) o **Docker** (`docker compose up -d --build`: base en el 5433, front y API en el 8080). Las dos deben seguir funcionando.

Backend organizado por módulo de negocio (`empresa`, `auth`, `categoria`, `producto`, `importacion`, `catalogopublico`, `storage`, `common`, `config`, `security`). Cada módulo tiene la misma forma: `controller` / `service` / `repository` / `dto/request` / `dto/response` / `model`. El repositorio usa JdbcTemplate y devuelve records: no hay capa `mapper`. Nunca exponer el `model` en los controllers: siempre DTOs.

Frontend: `core/` (guards, interceptors, services), `shared/` (components, layouts), `modules/<módulo>/`.

### Piezas transversales

- Backend: `common/` (`dto` con `ErrorResponse` y `PageResponse`, `exception`, `handler` con `GlobalExceptionHandler`, `net` con `ClientIpResolver`, `secrets` con la carga del `.env`, `time` con la zona horaria, y `export` para Excel en la fase 6), `config/` (`FlywayConfig`, `SwaggerConfig`), `security/` (`SecurityConfig`, CORS y `RateLimitFilter`) y `auth/` (JWT, refresh token y su filtro, en la fase 3).
- Front: `core/interceptors`, `core/guards`, `shared/layouts`, un componente de tabla para el panel y `src/testing/test-providers.ts`.
- Raíz: `.githooks/pre-push`, `.gitattributes`, `.gitignore` y `.claude/rules/`.

Fuera: matriz de permisos (módulos × acciones), MFA, envío de correo, política de contraseñas configurable y geolocalización. El login es corto (~60 líneas) y sin esas dependencias.

## 3. Roles

| Rol | Puede |
|---|---|
| `SUPER_ADMIN` | Crear/desactivar empresas, crear el usuario admin de cada empresa, ver todas |
| `ADMIN_EMPRESA` | Gestionar SOLO los datos de su empresa (productos, categorías, promos, configuración) |
| Visitante (anónimo) | Ver el catálogo público por link. Sin login |

Implementación: columna `usuario.rol` y `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` o `hasRole('SUPER_ADMIN')` en cada endpoint. Sin matriz de permisos.

**Multiempresa**: una sola base de datos, todas las tablas de negocio llevan `empresa_id`. El `empresa_id` del usuario autenticado se toma SIEMPRE del token, nunca del request. Un `ADMIN_EMPRESA` jamás debe poder leer ni modificar datos de otra empresa (cubrir con tests). Si se pide un recurso de otra empresa, responder 404 (no 403, para no revelar que existe).

**Desactivación**:

- Login y refresh rechazan al usuario si `usuario.activo = false` o si su empresa tiene `activa = false`.
- Al desactivar una empresa o un usuario se marcan `revocado = true` todos sus refresh tokens. El access token vigente deja de servir en ≤ 15 minutos.

## 4. Modelo de datos

**empresa**
- `id`, `nombre`, `slug` (editable, único, ej. `salsamentaria-javier`), `public_hash` (8 caracteres aleatorios `[a-z0-9]` con `SecureRandom`, **inmutable**, único)
- `logo_url` (cuadrado 512×512, ya recortado), `portada_url` (3200×1508, ya recortada, nítida en pantallas retina; null → la portada queda en el fondo noche `#15120D` con el titular)
- `color_primario`: color de los botones. Contraste ≥ 4.5:1 con texto blanco.
- `color_secundario`: color de acento (segunda línea del titular, botón de la portada, pestaña activa). Contraste ≥ 4.5:1 con el fondo noche `#15120D`.
- El backend rechaza colores que no cumplan el contraste (el panel ya avisa al elegirlos).
- `logo_original_url`, `logo_recorte`, `portada_original_url`, `portada_recorte`: el archivo que subió el dueño y el encuadre elegido (`{zoom, cx, cy}`), para poder reajustarlo sin volver a subirlo.
- `nombre_corto` (≤ 24; ej. "Javier", va grande en el encabezado y en "Nos vemos en {nombre_corto}."; null → el nombre), `lema` (≤ 32, debajo del nombre en el encabezado)
- `portada_antetitulo` (≤ 40, línea pequeña sobre el titular), `portada_titulo` (≤ 40; null → el nombre del negocio), `portada_titulo_acento` (≤ 40, en cursiva; admite saltos de línea), `portada_subtitulo` (≤ 140), `portada_nota` (≤ 48, nota manuscrita que sale en computador), `portada_sello_texto` (≤ 30, texto del sello redondo)
- `catalogo_titulo` (≤ 40, admite saltos de línea), `catalogo_acento` (≤ 30, final en cursiva), `catalogo_subtitulo` (≤ 140): encabezado de la página de catálogo
- `colecciones_titulo` (≤ 40, default "¿Cuál es el plan?"), `colecciones_texto` (≤ 80), `colecciones_nota` (≤ 40, nota manuscrita al lado del título)
- `visita_nota` (≤ 40, nota manuscrita de la sección de contacto)
- `fachada` (enum `SALSAMENTARIA`, `PANADERIA`, `FRUTERIA`, `CAFETERIA`, `MINIMERCADO`, `FERRETERIA`; null → sin dibujo): dibujo en línea de un local, con el nombre del negocio en el letrero, arriba de la sección de contacto. Los dibujos son SVG del front; la base solo guarda cuál.
- `adornos` (default true): hojas de laurel y perejil recortadas junto a los títulos (solo computador).
- En todos los textos que admiten saltos de línea, el dueño los pone con Enter; se guardan como `\n`.
- `direccion`, `horario`, `info_domicilios`
- `mostrar_precios` (default true)
- `pedidos_whatsapp` (default true): el catálogo permite armar un pedido y enviarlo por WhatsApp. En false, cada producto muestra un botón "Preguntar" que abre WhatsApp con el nombre del producto.
- `mostrar_agotados` (default true → se muestran con etiqueta "Agotado"; false → se ocultan)
- `activa` (default true), `created_at`, `updated_at`

**empresa_sello**: `id`, `empresa_id`, `icono` (enum: `CALIDAD`, `DOMICILIO`, `WHATSAPP`, `PRECIO`, `TRADICION`, `HORARIO`), `texto` (≤ 48), `orden`. Máximo 4 por empresa. Son las frases cortas con ícono debajo de la portada. Van en `GET/PUT /admin/empresa` como lista `sellos`.

**coleccion**: `id`, `empresa_id`, `nombre` (≤ 40), `descripcion` (≤ 80), `imagen_url`, `visible`, `orden`. Máximo 6 por empresa. Son los planes de la sección "¿Cuál es el plan?".

**coleccion_producto**: `coleccion_id`, `producto_id`, `orden`; clave `(coleccion_id, producto_id)`. El producto debe ser de la misma empresa que la colección. Borrar un producto lo quita de sus colecciones.

**empresa_whatsapp**: `id`, `empresa_id`, `numero` (formato internacional, solo dígitos, 10 a 15, ej. `573163218276`), `nombre` (opcional, ej. "Pedidos"), `orden`. Máximo 3 por empresa; único `(empresa_id, numero)`. Un negocio puede atender en varios números. `GET/PUT /admin/empresa` y el catálogo público los envían como lista `whatsapps` ordenada.

**usuario**: `id`, `empresa_id` (null para SUPER_ADMIN), `nombre`, `email` (único), `password_hash` (BCrypt), `rol`, `activo`, `created_at`

**refresh_token**: `id`, `usuario_id`, `token_hash`, `expira_en`, `revocado`

**categoria**: `id`, `empresa_id`, `nombre`, `orden`, `activa`, `imagen_url`; único `(empresa_id, nombre)`
- Franja del inicio: `banner_activo` (default false), `banner_url` (3200×1200), `banner_titulo` (≤ 40, admite saltos de línea), `banner_frase` (≤ 80), `banner_nota` (≤ 32, etiqueta de papel manuscrita).
- Solo una categoría por empresa tiene la franja: índice único parcial `(empresa_id) WHERE banner_activo`. Activarla en una la desactiva en la otra, en la misma transacción.

**producto**
- `id`, `empresa_id`, `categoria_id`, `codigo` — único `(empresa_id, codigo)`; clave para Excel e imágenes
- `nombre`, `descripcion`, `presentacion` (ej. "Tajado x 250 g")
- `unidad_venta` (enum: `UNIDAD`, `LIBRA`, `KILO`, `GRAMOS`, `PAQUETE`, `CAJA`)
- `precio numeric(12,0)`, `precio_oferta numeric(12,0) null`, `oferta_desde date null`, `oferta_hasta date null`
- `imagen_url`, `imagen_thumb_url`, `disponible`, `destacado`, `orden`
- `created_at`, `updated_at`

Reglas:
- La categoría de un producto debe pertenecer a la misma empresa.
- Una oferta está **vigente** si `precio_oferta` no es null y la fecha actual en `America/Bogota` está entre `oferta_desde` y `oferta_hasta` (extremo null = abierto). Usar un `Clock` inyectable para poder testearlo.
- `precio_oferta` debe ser menor que `precio`.
- Si existen las dos fechas, `oferta_desde` debe ser igual o anterior a `oferta_hasta`.
- Un solo validador de producto, usado por el formulario y por la importación Excel.
- Precios en COP sin decimales; en la vista: `$12.500`.
- Un producto es **nuevo** durante 30 días desde `created_at` (con el mismo `Clock`). No hay columna aparte.
- Etiqueta de la tarjeta, una sola, en este orden: "Agotado" > "Promo -X%" (oferta vigente y precios visibles) > "Más vendido" (`destacado`) > "Nuevo".

## 5. Link público

- URL: `/c/{slug}-{public_hash}` → ej. `/c/salsamentaria-javier-x7k2m9ab`
- El backend resuelve la empresa **solo por `public_hash`** (lo que va después del último `-`). Si cambian el slug, los links viejos siguen funcionando.
- **La ruta `/c/...` la atiende el backend** (Caddy la reenvía a `GET /api/public/paginas/{slugHash}`):
  - Si el slug del link no es el actual, responde 301 a la URL correcta.
  - Si no, devuelve el `index.html` del front con las etiquetas Open Graph (`og:title`, `og:description`, `og:image` = portada, o el logo si no hay portada, `og:url`) ya escritas. El `index.html` lleva un marcador `<!--og-->` en el `<head>`; el backend lo lee de `FRONT_INDEX_PATH` y reemplaza el marcador con los valores **escapados para HTML**.
  - Angular arranca en el navegador (CSR) y pide los datos a la API. El crawler de WhatsApp no ejecuta JavaScript: solo necesita esas etiquetas.
- Nunca expira. Empresa inactiva o inexistente → página "Catálogo no disponible" (y 404 en la API).
- Nadie puede adivinar catálogos de otras empresas (por eso el hash).

## 6. API (prefijo `/api`)

**Pública (sin auth)**
- `GET /public/catalogos/{publicHash}` → datos públicos de la empresa + categorías activas ordenadas + productos ordenados con `ofertaVigente` y `precioFinal` ya calculados.
  - Si `mostrar_precios = false` → NO enviar `precio`, `precioOferta`, `precioFinal` ni `ofertaVigente` (no basta con ocultarlos en el front). El front no muestra la sección "Ofertas".
  - Si `mostrar_agotados = false` → excluir los no disponibles.
  - Incluye nombre corto y lema, portada (antetítulo, titular, acento, subtítulo, nota, texto del sello), encabezado de la página de catálogo, notas manuscritas, `sellos`, la categoría con franja (si está activa y visible), las colecciones visibles con los ids de sus productos (solo las que tienen al menos un producto publicado) y `nuevo` por producto.
  - Cabecera `Cache-Control` corta (ej. 60 s).
- El pedido no tiene endpoint: se arma en el navegador (`localStorage` por negocio) y sale como enlace `https://wa.me/{numero}?text=`.
- `GET /public/paginas/{slugHash}` → HTML con Open Graph o 301 (ver §5).
- **Rate limiting**: `RateLimitFilter` propio (en memoria, una sola instancia; sin Bucket4j). Dos cupos por IP:
  - `/auth/login` y `/auth/refresh`: estricto (ej. 10 por minuto).
  - `/public/`: amplio (ej. 120 por minuto), porque los operadores móviles comparten una IP entre muchos clientes (CGNAT).
  - La IP real sale de `ClientIpResolver` con `TRUSTED_PROXY_COUNT=1` (detrás de Caddy). Sin esto, todos los visitantes comparten un solo cupo.
  - `// shortcut: contador en memoria por instancia; con dos instancias o balanceador, pasar a un almacén compartido.`

**Auth**
- `POST /auth/login` → access token (corto, ~15 min) + refresh token (~7 días). Rechaza usuarios o empresas inactivos.
- `POST /auth/refresh` (revisa de nuevo `activo` del usuario y `activa` de la empresa), `POST /auth/logout`
- `PUT /auth/password` (cambiar contraseña propia)

**Panel empresa (`ADMIN_EMPRESA`)**
- `GET/PUT /admin/empresa` (configuración, colores, toggles; no puede cambiar `public_hash` ni `activa`)
- `POST /admin/empresa/logo` y `POST /admin/empresa/portada`: multipart con `archivo` (opcional: si falta, se recorta de nuevo el original guardado) y `recorte` = rectángulo en píxeles del original (`x`, `y`, `ancho`, `alto`). Mismas reglas de archivo que la imagen de producto (5 MB, 8000 px por lado, dimensiones leídas antes de decodificar). El backend valida el rectángulo y genera el JPG final: logo 512×512, portada 3200×1508. En el logo el rectángulo puede salirse de la imagen (logo alejado para que entre completo) y lo que sobra se rellena de blanco. En la portada debe quedar dentro.
- `DELETE /admin/empresa/logo` y `DELETE /admin/empresa/portada` los quitan.
- CRUD `/admin/categorias` + `PUT /admin/categorias/orden`
- `PUT /admin/categorias/{id}/franja` (multipart: `titulo`, `frase`, `nota`, `archivo` opcional si ya tiene foto) y `DELETE /admin/categorias/{id}/franja`. Misma regla de imagen que el producto; salida JPG 3200×1200.
- CRUD `/admin/colecciones` (máx. 6; `productos` = lista de ids de la empresa) + `PUT /admin/colecciones/orden` + `POST /admin/colecciones/{id}/imagen`
- CRUD `/admin/productos` con paginación (`PageResponse`), búsqueda y filtro por categoría/disponibilidad
- `PATCH /admin/productos/{id}/disponible`
- `PATCH /admin/productos/precios` (edición masiva)
- `POST /admin/productos/{id}/imagen`:
  - Máximo 5 MB y **8000 px por lado**; jpg, png o webp.
  - Leer ancho y alto de la cabecera (`ImageReader.getWidth(0)` / `getHeight(0)`) **antes** de decodificar: un PNG de pocos MB puede ocupar varios GB de RAM al decodificarse.
  - Generar imagen máx. 1200 px + miniatura 400 px, ambas en JPG.
- `GET /admin/dashboard` (contadores)

**Importación (`ADMIN_EMPRESA`)**
- `GET /admin/importacion/plantilla` → descarga `.xlsx`
- `POST /admin/importacion/previsualizar` → sube Excel; devuelve filas válidas, filas con error (número de fila + motivo) y resumen (nuevos / a actualizar / categorías nuevas). **No guarda nada**: ni el archivo ni el resultado.
- `POST /admin/importacion/confirmar` → el front reenvía **el mismo archivo**. El backend lo vuelve a validar y:
  - sin errores: aplica todo en **una transacción**;
  - con errores: responde 422 con el mismo reporte de `previsualizar` y no aplica nada.
- `GET /admin/importacion/exportar` → descarga los productos de la empresa en el formato de la plantilla.
- `POST /admin/importacion/imagenes` → sube `.zip`; cada imagen se asigna al producto cuyo `codigo` = nombre del archivo sin extensión. Devuelve asignadas / sin producto / con error. Protecciones:
  - Límite de tamaño del ZIP (ej. 50 MB), de número de archivos (ej. 500) y de tamaño descomprimido por entrada.
  - Ignorar rutas con `..` o absolutas.
  - Cada imagen pasa el mismo control de 5 MB y 8000 px que la subida individual.

**Super admin (`SUPER_ADMIN`)**
- CRUD `/superadmin/empresas`, `PATCH /superadmin/empresas/{id}/activa` (al desactivar, revoca los refresh tokens de sus usuarios)
- `POST /superadmin/empresas/{id}/usuarios` (crear admin de la empresa con contraseña temporal)

## 7. Importación desde Excel

Plantilla con hoja `Productos`:

| codigo* | categoria* | nombre* | descripcion | presentacion | unidad_venta* | precio* | precio_oferta | oferta_desde | oferta_hasta | disponible | destacado |
|---|---|---|---|---|---|---|---|---|---|---|---|

- `*` obligatorio. `disponible`/`destacado` aceptan SI/NO (default SI / NO). Fechas `dd/mm/aaaa`.
- **Upsert por `codigo`**; categorías inexistentes se crean (la previsualización las lista como "categorías nuevas").
- Validaciones con mensaje claro por fila: precio no numérico, unidad inválida, código duplicado en el archivo, oferta mayor o igual al precio, fecha de fin anterior a la de inicio, etc.
- **Solo se confirma un archivo sin errores**: el usuario corrige el Excel y lo vuelve a subir.
- La plantilla incluye hoja `Instrucciones` y listas desplegables en `unidad_venta`, `disponible`, `destacado`.
- Opción "Exportar mis productos" en el mismo formato, para editar y volver a subir.
- Las imágenes NO van en el Excel: se suben aparte (ZIP o una por una).

## 8. Frontend

### 8.1 Catálogo público (`/c/:publicId`) — prioridad máxima, mobile-first

Diseño pedido por el cliente: portada fotográfica oscura con titular serif grande y un "mostrador" crema debajo. Es un solo diseño para todos los negocios; cada uno lo personaliza desde Configuración. El prototipo es la referencia visual (`DESIGN.md` documenta el sistema).

**Inicio** (`/c/:publicId`)
- Encabezado fijo sobre la portada: logo redondo y el nombre en tres líneas (nombre en pequeño, `nombre_corto` grande, `lema`); en computador, menú (Inicio, Catálogo, Promociones, la categoría con franja, Contáctanos); buscar; "Mi pedido" con contador y total. En celular, el menú va en una hoja y el carrito queda en el encabezado. Al bajar, el encabezado se vuelve sólido.
- "Contáctanos" es un enlace a `/c/:publicId/contacto`: abre el inicio y baja a la sección de contacto, antes del pie. No abre ventanas.
- Portada: foto a sangre con degradado noche, antetítulo en mayúsculas espaciadas, `portada_titulo` en blanco y `portada_titulo_acento` en cursiva con `color_secundario`, subtítulo, botón "Explorar el catálogo" (computador) o buscador (celular), nota manuscrita y sello circular con `portada_sello_texto` e inicial (solo computador).
- Sellos debajo de la portada (0 a 4), cada uno en una sola línea (en 960-1199 px, dos por fila).
- Celular: círculos de categorías con la foto de un producto.
- "¿Cuál es el plan?": tarjetas de las colecciones visibles con `colecciones_nota` manuscrita al lado (computador); cada una abre el catálogo filtrado por esa colección.
- "Nuestros favoritos" (título manuscrito en computador): pestañas Todos, Ofertas, Más pedidos y una por categoría (solo computador); carrusel de hasta 12 tarjetas con flechas en computador y "Ver todos" al catálogo con el mismo filtro.
- Franja grande de la categoría con `banner_activo` (ej. "Combos de la casa"), con borde ondulado, etiqueta de papel con `banner_nota` y botón a esa categoría. También sale al final de la página de catálogo.
- Sección de contacto (`id="contacto"`), centrada y sin foto: el dibujo de `fachada`, `visita_nota` manuscrita, "Nos vemos en {nombre_corto}." (el nombre en cursiva), una línea de invitación, un botón por número de WhatsApp (nombre o "WhatsApp" + número, abre `wa.me` directo) y debajo tres columnas sin tarjeta: Dirección (con "Cómo llegar" a Google Maps), Horario (una frase por línea) y Domicilios. En celular todo va apilado.
- Pie oscuro: logo y nombre del negocio con su subtítulo, menú "Explora" (las mismas secciones del encabezado), "Comparte el catálogo" (Web Share API, fallback copiar link) y una línea final con © año + aviso de precios y "Catálogo hecho con Agorá Catalog". No repite los teléfonos de la sección de contacto.
- Los cortes entre la portada, la franja y el contacto llevan un borde ondulado crema; el crema tiene una textura de papel muy suave.

**Catálogo** (`/c/:publicId/catalogo`)
- Encabezado con foto: antetítulo "Nuestro catálogo", `catalogo_titulo` + `catalogo_acento` en cursiva, `catalogo_subtitulo` y migas (Inicio / Catálogo).
- Búsqueda (sin tildes ni mayúsculas, en nombre, presentación, descripción y categoría), orden (relevancia, menor y mayor precio, A-Z) y vista cuadrícula o lista (computador).
- Filtros: categorías (varias, con conteo), rango de precio, solo promociones, "En stock" y "Mis favoritos" (solo si el visitante guardó alguno). En computador van en una tarjeta a la izquierda con otra tarjeta "¿No encuentras algo en específico?" que abre WhatsApp; en celular, en una hoja "Filtros" con "Ver N resultados".
- Los filtros activos se muestran como chips que se quitan con un toque y viven en la URL (`?cat=1,3&promo=1&q=queso&min=&max=&orden=&vista=&col=&fav=1`), para compartir una búsqueda.
- Si la última fila de la cuadrícula queda incompleta, una tarjeta oscura "Pide en tres pasos" (elige, revisa en «Mi pedido», envía por WhatsApp; el negocio confirma total y domicilio) ocupa las celdas libres, con el botón "Ver mi pedido". Se recalcula al cambiar el ancho; no sale en vista de lista, sin pedido por WhatsApp ni cuando la fila está completa.

**Tarjeta de producto**: foto 4:3 (placeholder si no tiene), etiqueta (§4), corazón de favoritos, nombre, descripción corta, precio en `#B4182D` + unidad ("$18.000 / libra"), precio tachado si hay oferta, presentación, selector de cantidad (− 1 +) y botón oscuro de agregar. En computador la tarjeta se eleva un poco al pasar el mouse. El nombre se corta a 3 líneas (completo en la ficha). Agotado: foto en gris y "Agotado por ahora", sin agregar.

**Favoritos del visitante**: el corazón guarda el producto en `localStorage` por negocio (`ec-favoritos-{public_hash}`), sin cuenta ni servidor. Sin almacenamiento, duran mientras la página esté abierta.

**Producto abierto**: hoja con foto grande, precio, descripción, cantidad, "Agregar al pedido" y "Preguntar por WhatsApp" (o "Preguntar cuándo llega" si está agotado).

**Pedido por WhatsApp** (`pedidos_whatsapp = true`)
- Al agregar, el contador y el total del encabezado se actualizan con una animación corta y un aviso breve. No hay barra inferior: el carrito del encabezado abre el pedido en celular y computador.
- "Mi pedido": hoja (celular) o panel lateral (computador) con cada línea, cantidad editable (el − con una unidad quita la línea), total, nombre y dirección opcionales, "Enviar pedido por WhatsApp" y "Vaciar pedido".
- El mensaje: `Hola, quiero hacer este pedido:` + una línea por producto (`• 2 × Queso campesino (Bloque): $24.000`) + `Total:` + nombre y entrega si se escribieron. Sin precios visibles, sin montos.
- La lista vive en `localStorage` por negocio (`ec-pedido-{public_hash}`); los productos que dejaron de estar disponibles se quitan al cargar. Sin almacenamiento (modo privado), el pedido dura mientras la página esté abierta.
- Con un solo número, los botones abren WhatsApp directo. Con varios, abren primero una hoja "¿A qué número le escribes?" con un botón por número (nombre opcional + número); el mensaje se conserva. Sin JavaScript, el enlace va al primer número.
- Con `pedidos_whatsapp = false`, cada tarjeta tiene "Preguntar" (WhatsApp con "Hola, vi en su catálogo: {nombre} ({presentacion}). ¿Lo tiene disponible?") y el carrito del encabezado no aparece.

**General**
- Tipografías: Playfair Display (titulares, con cursiva), Ms Madi (notas manuscritas), Onest (todo lo funcional). Elegidas comparándolas con el boceto del cliente.
- Imágenes lazy, miniaturas en listados.
- **Vista previa al compartir**: la resuelve el backend (§5). Toda la app es CSR.
- `color_primario` / `color_secundario` aplicados como CSS variables (`--brand`, `--accent`).

### 8.2 Panel admin (`/admin`, protegido con guard) — usable desde celular

- Login; interceptor HTTP que agrega el token y refresca automáticamente al expirar.
- Dashboard: # productos, # sin imagen, # agotados, ofertas vigentes, **link público con botón copiar y código QR descargable** (librería `qrcode`).
- Productos: listado (tabla en escritorio, cards en móvil) con búsqueda, filtros, paginación, toggle de disponible en línea, edición rápida de precio, formulario crear/editar con subida de imagen.
- Categorías: CRUD + reordenar con drag & drop (`@angular/cdk/drag-drop`). Botón de foto por categoría para la franja del inicio (encender/apagar, título, frase, etiqueta manuscrita, foto); avisa si se la quita a otra categoría.
- Colecciones: título, texto y nota manuscrita de la sección, lista con visible/oculta y editar; formulario con nombre, descripción, productos (lista con búsqueda y casillas), foto y eliminar. Máximo 6.
- Importar: descargar plantilla → subir Excel → previsualización con errores → corregir y volver a subir si hay errores → confirmar; exportar productos; subida de ZIP de imágenes con reporte.
- Configuración: datos del negocio, números de WhatsApp (agregar, quitar, nombre opcional; máx. 3), nombre corto y lema, portada del catálogo (antetítulo, titular, línea en cursiva, texto, texto del sello redondo), página de catálogo (titular, final en cursiva, texto), notas escritas a mano (portada y contacto), 4 sellos (ícono + texto), dibujo de «Nos vemos en…» (6 fachadas con vista previa o ninguno), toggles (adornos de hojas, precios, pedido por WhatsApp, agotados), "Color de botones" y "Color de acento" (avisan y no guardan si no hay contraste) y una vista previa de la portada que muestra el titular mientras se escribe. La portada y el logo se cambian tocándolos en esa vista previa (sin botones aparte). Sin imagen, el toque abre el selector de archivo. Con imagen, abre un editor de recorte:
  - Marco fijo: portada 1600:754, con dos líneas verticales que marcan la franja que se ve en el celular detrás del texto (en computador se ve completa); logo cuadrado.
  - Arrastrar para encuadrar, zoom con barra, rueda o `+`/`-`, flechas para mover.
  - El logo se puede alejar hasta que entre completo con fondo blanco.
  - Botones "Elegir otra foto", "Quitar" y "Guardar".
  - El logo se muestra siempre en la misma caja cuadrada de esquinas redondeadas: nunca cambia de tamaño ni empuja el nombre.
- El menú lateral muestra arriba el logo y el nombre del negocio; la marca Agorá Catalog va abajo, encima de "Ver mi catálogo".
- Botón "Ver mi catálogo".

### 8.3 Super admin (`/superadmin`)
- Listado de empresas, crear empresa + usuario admin, activar/desactivar.

## 9. Datos semilla (perfil `dev`)

- Usuario SUPER_ADMIN y usuario ADMIN_EMPRESA de prueba (credenciales en el README, solo local).
- Empresa **Salsamentaria Javier** (dueño Javier Barón; colores del diseño que pidió: botones verde oscuro `#24331F`, acento dorado `#DDB06C`; WhatsApp `573163218276` y `573015303994`; logo: el emblema completo de la vaca, original en `docs/prototipo/img/cliente/logo-javier.webp` y recortado a cuadrado en `logo-javier-cuadrado.webp`).
- Nombre corto "Javier", lema "Tradición y buen sabor".
- Portada: antetítulo "El sabor de reunirnos", "Se ve rico." / "Sabe a\nun buen plan.", subtítulo "Embutidos, quesos y salsamentaria de la mejor calidad para tus momentos especiales.", nota "Productos\nque hacen\nmejores momentos", sello "Tradición · desde siempre"; 4 sellos (calidad, domicilio, WhatsApp, precio).
- Página de catálogo: "Tradición\nen cada" + "bocado.", "Productos seleccionados de la mejor calidad para tus momentos especiales."
- Notas: "Siempre\nqueda bien." (colecciones), "El buen sabor\nde siempre" (contacto), "Listos\npara disfrutar" (franja).
- Categorías: Embutidos, Carnes frías, Quesos, Lácteos, Abarrotes y Combos (con la franja "Combos de la casa").
- 25 productos realistas (salchichón cervecero, mortadela, jamón de cerdo tajado, chorizo santarrosano, queso campesino, queso doble crema, kumis, mantequilla, 3 combos, etc.) con distintas unidades de venta, 4 con oferta vigente, 1 agotado y 3 nuevos.
- Colecciones: Una buena picada, Desayunar sin afán, Me dejo antojar.

## 10. Calidad

- Migraciones Flyway incrementales, con la cabecera obligatoria (`QUÉ HACE`, `DESTRUCTIVO`, `TIPOS`, `POR QUÉ`, `MEDIDO EN`) e inmutables una vez pusheadas. No hay JPA, así que no hay `ddl-auto`.
- Manejo global de errores con `GlobalExceptionHandler`: formato consistente y mensajes en español.
- Tests (JUnit 5 + Testcontainers con PostgreSQL):
  - aislamiento entre empresas;
  - resolución del link público (incluido el 301 por slug viejo);
  - ocultamiento de precios, incluidas las ofertas;
  - lógica de oferta vigente y fechas invertidas;
  - importación Excel (válidos y con errores);
  - desactivar empresa o usuario corta login y refresh;
  - imagen de más de 8000 px rechazada sin decodificarla.
- Tests unitarios en Angular (Vitest) para servicios y el interceptor de auth.
- CORS configurable con `CORS_ALLOWED_ORIGINS`.
- Configuración por variables de entorno en `agora-catalog-back/.env` (lo carga solo `SecretsEnvironmentPostProcessor`): `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, `STORAGE_PATH`, `FRONT_INDEX_PATH`, `PUBLIC_BASE_URL`, `CORS_ALLOWED_ORIGINS`, `TRUSTED_PROXY_COUNT`, `APP_TIME_ZONE` (`America/Bogota`). Sin secretos en el repo; `.env.example` documentado.
- `README.md` con cómo levantar todo en local.

## 11. Despliegue

- Dockerfile para `agora-catalog-back`. El front es estático (`dist/`) y lo sirve Caddy (`docker/web/`).
- `docker-compose.prod.yml`: api + PostgreSQL + **Caddy** (HTTPS automático con Let's Encrypt). Caddy:
  - sirve el front estático con fallback a `index.html`;
  - sirve `/uploads/*` desde `STORAGE_PATH`;
  - reenvía `/api/*` al backend y `/c/*` a `/api/public/paginas/*`.
- Objetivo: un VPS pequeño con dominio propio.
- Backups diarios: `pg_dump` de PostgreSQL **y** copia de la carpeta de imágenes (`STORAGE_PATH`), con una copia fuera del VPS.

## 12. Fuera de alcance (no construir ahora)

Pagos en línea, pedidos guardados en el servidor (el pedido solo vive en el navegador y sale por WhatsApp), estado de pedidos, inventario/stock numérico, facturación, login de clientes finales, favoritos guardados en el servidor (hoy viven en el navegador), app móvil nativa, exportación a PDF. El modelo debe permitir agregarlos después sin rehacerlo.

También se pospone:
- S3 / Cloudflare R2 para imágenes: cuando el disco o el ancho de banda del VPS no alcancen.
- SSR: solo si algún día se necesita posicionamiento en Google.

## 13. Plan de trabajo (en este orden, una fase a la vez)

0. Prototipo HTML navegable de todas las pantallas (`docs/prototipo/index.html`), aprobado antes de escribir en el stack. La marca (logo, símbolo, favicon, ícono de app) vive en `docs/prototipo/marca/` en SVG; el front la copia a sus assets.
1. Monorepo, docker-compose, back y front arrancando, nativo y en Docker.
2. Modelo de datos + migraciones Flyway + datos semilla.
3. Auth JWT + roles + aislamiento por empresa + desactivación (con tests).
4. API pública del catálogo + **catálogo público en Angular** (para mostrárselo al cliente cuanto antes).
5. Panel: productos, categorías, configuración, imágenes.
6. Importación/exportación Excel + ZIP de imágenes.
7. Dashboard con link y QR; super admin.
8. Open Graph desde el backend, pulido mobile, Dockerfile del back, Caddy, README y despliegue.

Al terminar cada fase: compilar, correr tests y resumir qué quedó hecho antes de seguir.

## 14. Decisiones de la revisión (2026-10-16)

| # | Decisión | Por qué |
|---|---|---|
| 1 | Monorepo con back y front separados y convenciones comunes | Una sola convención para los dos lados |
| 2 | Límite de 8000 px por lado, revisado antes de decodificar | Un PNG de 5 MB puede ocupar GB de RAM y tumbar la API de todas las empresas |
| 3 | Login y refresh revisan `activo`; desactivar revoca los tokens | Desactivar debe cortar el acceso en ≤ 15 min, no en 7 días |
| 4 | IP real con `ClientIpResolver` detrás del proxy | Sin eso, todos los visitantes comparten un solo cupo |
| 5 | Salida JPG; WebP solo de entrada, con TwelveMonkeys | El JDK no lee ni escribe WebP |
| 6 | JdbcTemplate, sin JPA ni mappers | Un solo estilo de acceso a datos |
| 7 | Sin SSR: el backend escribe las etiquetas Open Graph | WhatsApp no ejecuta JavaScript; evita un runtime Node en producción |
| 8 | Importación sin estado temporal: se reenvía el archivo | Sin tabla temporal ni expiración; sobrevive a un deploy entre los dos pasos |
| 9 | `RateLimitFilter` propio en memoria, sin Bucket4j; cupo público aparte | Sin dependencia nueva; los operadores móviles comparten IP |
| 10 | Imágenes en disco local + backup fuera del VPS | Un VPS y un cliente; S3/R2 cuando haga falta |
| 11 | Precios ocultos ocultan también las ofertas | No revelar descuentos ni mostrar una sección rota |
| 12 | `oferta_desde <= oferta_hasta` | Evita ofertas que nunca aparecen |
| 13 | Solo las piezas transversales listadas en §2 | Evita miles de líneas de RBAC, MFA y correo sin uso |
| 14 | QR con `qrcode`, drag & drop con CDK, Excel con `common/export` | Librerías pequeñas y conocidas |
| 15 | Pedido por WhatsApp guardado solo en el navegador | Lo pidió el cliente; sin pagos, cuentas ni tabla de pedidos que mantener |
| 16 | Un solo diseño de catálogo, configurable por negocio | Javier se ve como su boceto y los demás negocios cambian portada, frase, sellos, colecciones y colores sin código |
| 17 | Secciones del boceto: portada con frase, "¿Cuál es el plan?", franja de combos, filtros en computador | Elegidas por el usuario el 2026-10-16 |
| 18 | El catálogo sigue el boceto del cliente sin cambios: antetítulos, notas manuscritas, corazones de favoritos (en el navegador), "Contáctanos" que baja a la sección de contacto, sin barra inferior | Lo pidió el usuario el 2026-10-16 ("preservar el diseño que te mandé sin cambiar nada") |
| 19 | Contraste obligatorio en los dos colores del negocio | Botones con texto blanco y acento sobre la portada oscura siempre legibles |
| 20 | Caddy en lugar de Nginx + certbot | HTTPS y renovación automáticos, sin tareas aparte |
| 21 | Contacto sin foto grande, con fachada dibujada elegible por tipo de negocio | El usuario rechazó la foto y el logo repetido; la plataforma no es solo para salsamentarias |
| 22 | Adornos solo de hojas, apagables | El queso recortado no gustó; las hojas sirven para casi cualquier negocio de comida |
| 23 | Portada 3200×1508 y franja 3200×1200 | A 1600 px la portada se veía pixelada en pantallas grandes |
