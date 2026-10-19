---
description: Roles y aislamiento entre empresas. Toda ruta nueva del backend declara quién la llama y nunca lee el empresa_id del request.
paths:
  - "agora-catalog-back/src/main/java/**/controller/**"
  - "agora-catalog-back/src/main/java/**/repository/**"
  - "agora-catalog-back/src/main/resources/db/migration/**"
---

# Roles y aislamiento entre empresas

## 1. Roles

| Rol | Rutas | Puede |
|---|---|---|
| `SUPER_ADMIN` | `/superadmin/**` | Crear y desactivar empresas, crear el admin de cada una |
| `ADMIN_EMPRESA` | `/admin/**` | Solo los datos de su empresa |
| Visitante (sin login) | `/public/**` | Leer el catálogo público por link |

- El rol vive en `usuario.rol`. No hay matriz de permisos.
- **Toda ruta de `/admin` o `/superadmin` lleva `@PreAuthorize`** con el rol:
  `@PreAuthorize("hasRole('ADMIN_EMPRESA')")` o `@PreAuthorize("hasRole('SUPER_ADMIN')")`.
- `/public/**` y `/auth/**` son las únicas rutas abiertas (`SecurityConfig.PUBLIC_PATHS`). Nada
  privado va bajo esos prefijos.

## 2. Una sola base, todas las empresas

- Toda tabla de negocio lleva `empresa_id` con su clave foránea y un índice que empieza por él.
- El `empresa_id` del usuario autenticado sale **siempre del token**, nunca del body, del query
  string ni de la ruta.
- Toda consulta de un repositorio de negocio filtra por `empresa_id`, también los `UPDATE` y
  `DELETE`: `WHERE id = ? AND empresa_id = ?`.
- Un recurso de otra empresa responde **404, no 403**: no se revela que existe
  (`ResourceNotFoundException`).
- Una relación entre dos registros (producto → categoría, colección → producto) valida que los dos
  sean de la misma empresa antes de guardar.

## 3. Pruebas obligatorias

Cada módulo nuevo de `/admin` lleva una prueba con dos empresas: el admin de la empresa A no lee,
no edita y no borra un registro de la empresa B (recibe 404), y el listado de A no trae filas de B.
Probar también con un usuario `SUPER_ADMIN` contra `/admin` y con un `ADMIN_EMPRESA` contra
`/superadmin`: los dos reciben 403.
