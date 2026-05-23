#!/bin/sh
set -eu

# Minimal services runner for folded Fly.io deployment.
# Starts backend JARs (assumes Java services) and then starts nginx in foreground.
# Logs from services are redirected to stdout/stderr so Fly collects them.

JAVA_CMD=${JAVA_CMD:-java}

_start() {
  echo "[services-runner] starting services"

  # Start game-service on 8082 if jar exists
  if [ -f /srv/services/game-service.jar ]; then
    echo "[services-runner] launching game-service"
    $JAVA_CMD -jar /srv/services/game-service.jar --server.port=8082 > /proc/1/fd/1 2>/proc/1/fd/2 &
    PID_GAME=$!
  else
    PID_GAME=0
  fi

  if [ -f /srv/services/purchase-service.jar ]; then
    echo "[services-runner] launching purchase-service"
    $JAVA_CMD -jar /srv/services/purchase-service.jar --server.port=8083 > /proc/1/fd/1 2>/proc/1/fd/2 &
    PID_PURCHASE=$!
  else
    PID_PURCHASE=0
  fi

  if [ -f /srv/services/wishlist-service.jar ]; then
    echo "[services-runner] launching wishlist-service"
    $JAVA_CMD -jar /srv/services/wishlist-service.jar --server.port=8084 > /proc/1/fd/1 2>/proc/1/fd/2 &
    PID_WISHLIST=$!
  else
    PID_WISHLIST=0
  fi

  if [ -f /srv/services/notification-service.jar ]; then
    echo "[services-runner] launching notification-service"
    $JAVA_CMD -jar /srv/services/notification-service.jar --server.port=8085 > /proc/1/fd/1 2>/proc/1/fd/2 &
    PID_NOTIFICATION=$!
  else
    PID_NOTIFICATION=0
  fi

  if [ -f /srv/services/auth-service.jar ]; then
    echo "[services-runner] launching auth-service"
    $JAVA_CMD -jar /srv/services/auth-service.jar --server.port=8087 > /proc/1/fd/1 2>/proc/1/fd/2 &
    PID_AUTH=$!
  else
    PID_AUTH=0
  fi

  if [ -f /srv/services/storage-service.jar ]; then
    echo "[services-runner] launching storage-service"
    $JAVA_CMD -jar /srv/services/storage-service.jar --server.port=8086 > /proc/1/fd/1 2>/proc/1/fd/2 &
    PID_STORAGE=$!
  else
    PID_STORAGE=0
  fi

  # Trap SIGTERM and SIGINT to shut down children
  _term() {
    echo "[services-runner] received stop signal, terminating children"
    [ "$PID_GAME" -ne 0 ] 2>/dev/null && kill -TERM "$PID_GAME" 2>/dev/null || true
    [ "$PID_PURCHASE" -ne 0 ] 2>/dev/null && kill -TERM "$PID_PURCHASE" 2>/dev/null || true
    [ "$PID_WISHLIST" -ne 0 ] 2>/dev/null && kill -TERM "$PID_WISHLIST" 2>/dev/null || true
    [ "$PID_NOTIFICATION" -ne 0 ] 2>/dev/null && kill -TERM "$PID_NOTIFICATION" 2>/dev/null || true
    [ "$PID_AUTH" -ne 0 ] 2>/dev/null && kill -TERM "$PID_AUTH" 2>/dev/null || true
    [ "$PID_STORAGE" -ne 0 ] 2>/dev/null && kill -TERM "$PID_STORAGE" 2>/dev/null || true
    # give children time to exit
    sleep 2
    exit 0
  }
  trap _term SIGTERM SIGINT

  echo "[services-runner] starting nginx"
  exec nginx -g 'daemon off;'
}

_start
