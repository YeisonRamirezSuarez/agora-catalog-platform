# Front compilado servido por Caddy, que además reenvía /api al backend.
# Se construye desde la raíz del monorepo (ver docker-compose.yml).

FROM node:24-alpine AS build
WORKDIR /app
COPY agora-catalog-front/package.json agora-catalog-front/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY agora-catalog-front/ ./
RUN npm run build

FROM caddy:2-alpine
COPY docker/web/Caddyfile /etc/caddy/Caddyfile
COPY --from=build /app/dist/agora-catalog-front/browser /srv
