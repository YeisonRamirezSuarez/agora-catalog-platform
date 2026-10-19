# Reglas

Instrucciones que Claude sigue **al editar código**. Cada archivo lleva `paths:` en el frontmatter,
así que se activa solo cuando el trabajo toca esos archivos.

| Archivo | Se activa al tocar | Qué exige |
|---|---|---|
| [`migraciones.md`](migraciones.md) | `db/migration/**` | Política de base de datos y Flyway: migraciones incrementales con cabecera, inmutables una vez publicadas. |
| [`multiempresa.md`](multiempresa.md) | controladores, repositorios y migraciones | Roles por ruta con `@PreAuthorize` y aislamiento por `empresa_id` tomado del token. |
