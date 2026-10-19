# Pruebas de pantalla (Playwright)

El catálogo público y el panel en un Chrome de verdad: clics, selector de archivos, celular (390 px) y
computador (1280 px). Cubren lo que las pruebas unitarias no ven: que "Elegir foto" abra el selector,
que la hoja de números de WhatsApp no se salga, que mover un filtro no suba la página, que el editor de
recorte quepa en un portátil, el pedido por WhatsApp, el link de producto y las estadísticas.

En CI las corre el job **Pantallas** en cada push. Para correrlas en local hacen falta una base de
prueba con la semilla, la API y la app compilada servida en el 4300:

```bash
# 1. Base de prueba vacía (Flyway la arma y carga la semilla al arrancar la API)
psql -U postgres -c "CREATE DATABASE agora_catalog_e2e"

# 2. Front compilado
cd agora-catalog-front && npm run build

# 3. API en el 9093 contra esa base (perfil local = con semilla), sin límite de peticiones
cd agora-catalog-back
SPRING_PROFILES_ACTIVE=local SERVER_PORT=9093 \
  DB_URL='jdbc:postgresql://localhost:5432/agora_catalog_e2e?currentSchema=main' \
  SETTINGS_SECURITY_RATE_LIMIT_ENABLED=false TRUSTED_PROXY_COUNT=1 \
  FRONTEND_URL=http://127.0.0.1:4300 CORS_ALLOWED_ORIGINS=http://127.0.0.1:4300 \
  FRONT_INDEX_PATH=../agora-catalog-front/dist/agora-catalog-front/browser/index.html \
  java -jar build/libs/agora-catalog-back-0.1.0.jar

# 4. La app en el 4300, con /api y /c hacia esa API
python herramientas/servidor/serve_app.py agora-catalog-front/dist/agora-catalog-front/browser 4300 --api http://127.0.0.1:9093

# 5. Las pruebas (usan el Chrome instalado)
cd agora-catalog-front && npx playwright test
```

Las pruebas cambian datos (franja, logo, productos nuevos): usar siempre una base de prueba, nunca la
real. La prueba del super admin solo corre con `E2E_SUPER_EMAIL` y `E2E_SUPER_PASSWORD` (una cuenta de
prueba creada con `SUPERADMIN_*` al arrancar la API) y le cambia la contraseña.
