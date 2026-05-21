#!/bin/sh
# Provide defaults if not set and replace env vars in template then start nginx
: ${GAME_SERVICE_HOST:=game-service:8082}
: ${STORAGE_SERVICE_HOST:=storage-service:8085}
: ${AUTH_SERVICE_HOST:=auth-user-service:8087}
envsubst '$GAME_SERVICE_HOST $STORAGE_SERVICE_HOST $AUTH_SERVICE_HOST' \
  < /etc/nginx/templates/nginx.conf.template > /etc/nginx/nginx.conf
exec nginx -g 'daemon off;'
