# CLAUDE.md — backend

Spring Boot 4 · Java 21 · Spring Security · JdbcTemplate (sin JPA) · Flyway · PostgreSQL.
Las reglas que cruzan front y back están en el `CLAUDE.md` de la raíz.

## Estructura

Un paquete por módulo de negocio bajo `com.agora.catalog`, todos con la misma forma:

```
<modulo>/
├── controller/       @RestController; solo recibe, valida (@Valid) y delega
├── service/          una clase por caso de uso (CreateProductoService, ListProductosService...)
├── repository/       JdbcTemplate + SQL explícito; devuelve records o modelos
├── dto/request/      entrada del controller (records con Bean Validation)
├── dto/response/     salida del controller
└── model/            filas de la base; nunca salen por un controller
```

Módulos previstos: `empresa`, `auth`, `categoria`, `producto`, `coleccion`, `importacion`,
`catalogopublico`, `storage`. Transversales:

| Paquete | Qué tiene |
|---|---|
| `common/dto` | `ErrorResponse` (todo error de la API) y `PageResponse` (listados paginados, snake_case) |
| `common/exception` | `ResourceNotFoundException` (404), `DuplicateResourceException` y `ResourceInUseException` (409) |
| `common/handler` | `GlobalExceptionHandler`: convierte excepciones en `ErrorResponse` con mensajes en español |
| `common/net` | `ClientIpResolver`: IP real detrás de Caddy (`TRUSTED_PROXY_COUNT`) |
| `common/secrets` | Carga del `.env` antes de arrancar Spring |
| `common/time` | Zona horaria de la JVM (`APP_TIME_ZONE`, `America/Bogota`) |
| `config` | `FlywayConfig` (esquema `main`), `SwaggerConfig` |
| `security` | `SecurityConfig` (rutas abiertas: `/auth/**`, `/public/**`), CORS y `RateLimitFilter` |

## Convenciones

- **Sin JPA ni mappers**: el repositorio arma el objeto desde el `ResultSet`. SQL con parámetros
  (`?`), nunca concatenando valores del usuario.
- **Nunca exponer el `model`** en un controller: siempre un DTO de `dto/response`.
- Errores de negocio con las excepciones de `common/exception`; el mensaje lo ve el usuario tal cual,
  en español neutro con tuteo.
- Transacciones con `@Transactional` en el servicio, no en el repositorio.
- Fechas de negocio con un `Clock` inyectable (oferta vigente, producto nuevo) para poder probarlas.
- Precios en `numeric(12,0)` y `long`/`BigDecimal` en Java: pesos colombianos sin decimales.

## Seguridad

- Rutas abiertas solo en `SecurityConfig.PUBLIC_PATHS`. Todo lo demás exige token.
- Toda ruta de `/admin` o `/superadmin` lleva `@PreAuthorize` con el rol
  (`.claude/rules/multiempresa.md`).
- `RateLimitFilter`: cupo estricto para `/auth/login` y `/auth/refresh`, amplio para `/public/`.

## Configuración

`application.yaml` (común) + `application-{local,dev,prod}.yaml`. Los valores por ambiente salen de
variables de entorno o del `.env` (`.env.example` documenta cada una). `prod` escucha solo en
loopback y apaga Swagger.

## Pruebas

`bash gradlew build` corre todo. `AgoraCatalogApplicationTests` arranca la aplicación contra la base
del `.env`, así que **aplica las migraciones pendientes a esa base**. Las pruebas unitarias no
necesitan base.
