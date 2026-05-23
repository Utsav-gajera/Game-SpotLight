#!/bin/sh
set -euo pipefail

# Consolidated nginx entrypoint: performs placeholder substitution from
# services/nginx/nginx.conf.template into /etc/nginx/nginx.conf and starts
# nginx. This is the tested entrypoint previously developed under deployment/.

TEMPLATE=/etc/nginx/nginx.conf.template
OUT=/etc/nginx/nginx.conf

if [ ! -f "$TEMPLATE" ]; then
  echo "Template $TEMPLATE not found" >&2
  exit 1
fi

: > "$OUT"

VARS="GAME_SERVICE_HOST PURCHASE_SERVICE_HOST WISHLIST_SERVICE_HOST NOTIFICATION_SERVICE_HOST AUTH_SERVICE_HOST STORAGE_SERVICE_HOST"

cp "$TEMPLATE" "$OUT"

for var in $VARS; do
  val=""
  # Expand the environment variable named by $var (POSIX-safe)
  eval "val=\$$var" || true
  esc_val=$(printf '%s' "$val" | sed -e 's/[\/&]/\\&/g')
  # Replace placeholder occurrences like @VAR@ in the output file
  sed -i "s/@${var}@/$esc_val/g" "$OUT" || true
done

exec nginx -g 'daemon off;'
