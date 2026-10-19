#!/usr/bin/env sh
# Respaldo diario de producción (docker-compose.prod.yml): la base Y las fotos. Una sin la otra no
# sirve: la base guarda las URL de fotos que viven en el volumen de uploads.
#
#   sh scripts/respaldo/respaldar.sh             # a mano, desde la raíz del repo
#   crontab -e  ->  30 3 * * * cd /opt/agora-catalog-platform && sh scripts/respaldo/respaldar.sh >> /var/log/agora-respaldo.log 2>&1
#
# Variables (del .env de la raíz o del entorno):
#   RESPALDO_DIR      carpeta local de los respaldos (por defecto /var/backups/agora-catalog)
#   RESPALDO_DIAS     días que se guardan en local (por defecto 14)
#   RESPALDO_REMOTO   destino de rclone para la copia fuera del VPS (ej. r2:agora-respaldos). Sin él,
#                     los respaldos quedan solo en este servidor: si se pierde el VPS, se pierden.
#
# Restaurar: ver README, "Respaldos".
set -eu

cd "$(dirname "$0")/../.."
# Solo las variables de respaldo del .env: el resto (contraseñas) no hace falta acá.
if [ -f .env ]; then
  eval "$(grep -E '^RESPALDO_(DIR|DIAS|REMOTO)=' .env || true)"
fi
DIR="${RESPALDO_DIR:-/var/backups/agora-catalog}"
DIAS="${RESPALDO_DIAS:-14}"
FECHA="$(date +%Y-%m-%d_%H%M)"
mkdir -p "$DIR"

echo "[$(date)] Respaldo $FECHA en $DIR"

# Formato custom (-Fc): comprimido y se restaura con pg_restore, incluso tabla por tabla.
docker compose -f docker-compose.prod.yml exec -T postgres \
  sh -c 'pg_dump -U "$POSTGRES_USER" -Fc "$POSTGRES_DB"' > "$DIR/bd-$FECHA.dump.tmp"
mv "$DIR/bd-$FECHA.dump.tmp" "$DIR/bd-$FECHA.dump"

docker run --rm -v agora-catalog-prod-uploads:/datos:ro alpine \
  tar czf - -C /datos . > "$DIR/fotos-$FECHA.tar.gz.tmp"
mv "$DIR/fotos-$FECHA.tar.gz.tmp" "$DIR/fotos-$FECHA.tar.gz"

find "$DIR" -name 'bd-*.dump' -mtime +"$DIAS" -delete
find "$DIR" -name 'fotos-*.tar.gz' -mtime +"$DIAS" -delete

if [ -n "${RESPALDO_REMOTO:-}" ]; then
  rclone copy "$DIR/bd-$FECHA.dump" "$RESPALDO_REMOTO"
  rclone copy "$DIR/fotos-$FECHA.tar.gz" "$RESPALDO_REMOTO"
  echo "Copia fuera del servidor: $RESPALDO_REMOTO"
else
  echo "AVISO: sin RESPALDO_REMOTO, el respaldo queda solo en este servidor."
fi
ls -lh "$DIR/bd-$FECHA.dump" "$DIR/fotos-$FECHA.tar.gz"
