# GameSpotlight Deployment - Quick Reference & Troubleshooting

## Quick Commands

### View Service Status
```bash
# Check all GameSpotlight services
sudo systemctl list-units --type=service | grep gamespotlight

# Check specific service
sudo systemctl status auth-service
sudo systemctl is-active auth-service  # Returns "active" or "inactive"
```

### View Logs
```bash
# Real-time logs for specific service
sudo journalctl -u auth-service -f

# Last 50 lines
sudo journalctl -u auth-service -n 50

# Since last boot
sudo journalctl -u auth-service -b

# Specific time range
sudo journalctl -u auth-service --since "2 hours ago"

# All services
sudo journalctl -f

# Nginx logs
sudo tail -f /var/log/nginx/gamespotlight_access.log
sudo tail -f /var/log/nginx/gamespotlight_error.log
```

### Restart Services
```bash
# Restart single service
sudo systemctl restart auth-service

# Restart all services
for svc in auth-service game-service purchase-service wishlist-service notification-service storage-service gateway; do
    sudo systemctl restart $svc
done

# Restart Nginx
sudo systemctl restart nginx
```

### Start/Stop Services
```bash
# Start
sudo systemctl start auth-service

# Stop
sudo systemctl stop auth-service

# Disable (don't auto-start on boot)
sudo systemctl disable auth-service
```

### Check Service Ports
```bash
# Check if services are listening
sudo netstat -tlnp | grep java
sudo netstat -tlnp | grep nginx

# Check specific port
sudo netstat -tlnp | grep :8087
```

### Test API Endpoints
```bash
# Health check
curl http://localhost/health
curl http://localhost:8080/actuator/health

# Test auth service
curl http://localhost:8087/actuator/health

# Test with auth header
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" http://localhost/api/auth/session

# Test file upload
curl -X POST -F "file=@/path/to/file" http://localhost/api/storage/upload

# Test with verbose output
curl -v http://localhost/health
```

### Rebuild & Redeploy Service
```bash
# Go to repo
cd /opt/GameSpotlight/Game-SpotLight

# Rebuild specific service
mvn clean package -DskipTests -pl services/game-service

# Stop the service
sudo systemctl stop game-service

# Replace JAR
cp services/game-service/target/game-service-0.0.1-SNAPSHOT.jar services/game-service/target/

# Start service
sudo systemctl start game-service

# Watch logs
sudo journalctl -u game-service -f
```

---

## Troubleshooting Guide

### ❌ Service won't start

**Check logs:**
```bash
sudo journalctl -u auth-service -n 50
```

**Common causes:**
1. **Port already in use**
   ```bash
   sudo netstat -tlnp | grep :8087
   kill -9 <PID>
   ```

2. **Java OutOfMemory**
   - Increase memory in systemd service:
   - Edit `/etc/systemd/system/auth-service.service`
   - Change `-Xmx512m` to `-Xmx1024m`
   - Reload: `sudo systemctl daemon-reload && sudo systemctl restart auth-service`

3. **Missing environment variables**
   ```bash
   # Check if .env file is readable
   cat /opt/GameSpotlight/Game-SpotLight/.env | head
   ```

4. **Database connection failed**
   - Test PostgreSQL connection:
   ```bash
   source .env
   nc -zv gamespotlightdb-gajerautsav08-9ccc.h.aivencloud.com 22498
   ```
   - Verify credentials in `.env`

---

### ❌ Nginx not working

**Check config:**
```bash
sudo nginx -t  # Should say "successful"
```

**Check Nginx status:**
```bash
sudo systemctl status nginx
sudo journalctl -u nginx -f
```

**Port 80 conflicts:**
```bash
# Find what's using port 80
sudo lsof -i :80
sudo netstat -tlnp | grep :80
```

**Fix config:**
```bash
# Edit config
sudo nano /etc/nginx/sites-available/gamespotlight

# Test after edit
sudo nginx -t

# Reload (don't restart entirely)
sudo systemctl reload nginx
```

---

### ❌ Database connection issues

**PostgreSQL:**
```bash
# Test connection with psql
apt install postgresql-client -y
psql -h gamespotlightdb-gajerautsav08-9ccc.h.aivencloud.com -U avnadmin -d auth_db

# From .env
source .env
psql "$POSTGRES_JDBC_URL" -U "$POSTGRES_USER"
```

**MongoDB:**
```bash
# Test with mongosh
apt install mongodb-mongosh -y
mongosh "$MONGO_URI"
```

**Redis:**
```bash
# Test Redis connection
apt install redis-tools -y
redis-cli -h 13.200.236.38 -p 14985 -u default ping
```

---

### ❌ Services slow or timing out

**Check system resources:**
```bash
# CPU & Memory
top -bn1 | head -20

# Disk space
df -h

# Free memory
free -h
```

**If out of resources:**
- Stop unnecessary services: `sudo systemctl stop notification-service`
- Upgrade VM (Oracle Free Tier can't auto-scale)
- Reduce Java heap: `-Xmx256m` in systemd service

**Network issues:**
```bash
# Check internet connectivity
ping -c 1 google.com

# Check if external services reachable
curl -I https://gamespotlightdb-gajerautsav08-9ccc.h.aivencloud.com:22498
```

---

### ❌ Frontend can't reach backend API

**Check CORS settings:**
```bash
# Add to Nginx config:
add_header Access-Control-Allow-Origin "https://gamespotlight.vercel.app" always;

# Test CORS request
curl -H "Origin: https://gamespotlight.vercel.app" -v http://localhost/api/
```

**Check API URL in frontend:**
- `.env` on Vercel should have `VITE_API_BASE_URL=https://yourdomain.com/api`

**Test connectivity:**
```bash
# From frontend, check backend
curl https://yourdomain.com/api/health
```

---

### ❌ File uploads failing

**Check storage service:**
```bash
sudo journalctl -u storage-service -f
```

**Check Supabase credentials:**
```bash
# From VM, test Supabase
curl -X POST \
  -H "Authorization: Bearer $SUPABASE_KEY" \
  -F "file=@test.txt" \
  "$SUPABASE_URL/storage/v1/object/game-files/test.txt"
```

**Increase upload limit:**
```bash
# Edit Nginx config, increase:
client_max_body_size 1000M;

# Reload
sudo systemctl reload nginx
```

---

### ❌ Database migrations failed

**Re-run migrations:**
```bash
cd /opt/GameSpotlight/Game-SpotLight
source .env

mvn -f services/auth-user-service/pom.xml \
  -Dflyway.url="$POSTGRES_JDBC_URL" \
  -Dflyway.user="$POSTGRES_USER" \
  -Dflyway.password="$POSTGRES_PASSWORD" \
  flyway:migrate -X  # -X for debug output
```

**Reset migrations (⚠️ Careful!):**
```bash
mvn -f services/auth-user-service/pom.xml \
  -Dflyway.url="$POSTGRES_JDBC_URL" \
  -Dflyway.user="$POSTGRES_USER" \
  -Dflyway.password="$POSTGRES_PASSWORD" \
  flyway:clean  # Deletes all tables!
```

---

### ❌ Memory leak (service keeps growing memory)

**Monitor memory usage:**
```bash
# Get PID of service
ps aux | grep java | grep game-service

# Monitor specific process
watch -n 5 'ps aux | grep 12345'

# Check for memory leaks with jmap
jmap -heap 12345
```

**Solutions:**
1. Restart service: `sudo systemctl restart game-service`
2. Reduce heap size in systemd
3. Check for open file handles: `lsof -p 12345`

---

### ❌ Kafka connection issues

**Test Kafka connection:**
```bash
# Install kafkacat
apt install kafkacat -y

# Test connection
kafkacat -b gamespotlight-gajerautsav08-9ccc.l.aivencloud.com:22511 \
  -X security.protocol=SASL_SSL \
  -X sasl.mechanism=PLAIN \
  -X sasl.username=avnadmin \
  -X sasl.password=$KAFKA_API_SECRET \
  -L  # List topics
```

**Check Kafka service logs:**
```bash
sudo journalctl -u purchase-service -f | grep -i kafka
```

---

## Performance Tuning

### Increase Java Heap (for large games DB)
```bash
# Edit systemd service
sudo nano /etc/systemd/system/game-service.service

# Change:
# ExecStart=/usr/bin/java -Xmx512m ...
# To:
# ExecStart=/usr/bin/java -Xmx1024m ...

# Reload & restart
sudo systemctl daemon-reload
sudo systemctl restart game-service
```

### Enable Gzip in Nginx
```bash
# Already in nginx.conf, check it's enabled
grep "gzip on" /etc/nginx/sites-available/gamespotlight
```

### Connection Pooling
```bash
# In .env, increase:
SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=20
SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=10
```

### Redis Caching
```bash
# Verify Redis is working
redis-cli -h 13.200.236.38 -p 14985 -u default INFO stats
```

---

## Emergency Recovery

### Rollback to Previous Build
```bash
cd /opt/GameSpotlight/Game-SpotLight

# Keep previous JAR
cp services/game-service/target/*.jar services/game-service/target/backup/

# Rebuild from git (clean)
git clean -fd
git checkout .
mvn clean package -DskipTests -pl services/game-service

# Restart
sudo systemctl restart game-service
```

### Reset Everything
```bash
# CAUTION: This removes all data!
sudo systemctl stop auth-service game-service purchase-service wishlist-service notification-service storage-service gateway

# Remove services
for svc in auth-service game-service purchase-service wishlist-service notification-service storage-service gateway; do
    sudo rm /etc/systemd/system/${svc}.service
done

sudo systemctl daemon-reload

# Then follow deployment instructions again
```

---

## Performance Monitoring Script

```bash
#!/bin/bash
# Save as monitor.sh and run: bash monitor.sh

while true; do
    clear
    echo "🚀 GameSpotlight Service Monitor - $(date)"
    echo "=================================================="
    
    # Service status
    echo ""
    echo "📊 Service Status:"
    for svc in auth-service game-service purchase-service wishlist-service notification-service storage-service gateway; do
        status=$(sudo systemctl is-active $svc)
        if [ "$status" == "active" ]; then
            echo "  ✓ $svc"
        else
            echo "  ✗ $svc - $status"
        fi
    done
    
    # System resources
    echo ""
    echo "💻 System Resources:"
    echo "  CPU: $(top -bn1 | grep "Cpu(s)" | awk '{print $2}')"
    echo "  Memory: $(free -h | awk '/^Mem:/ {print $3 "/" $2}')"
    echo "  Disk: $(df -h / | awk '/^\/dev/ {print $3 "/" $2}')"
    
    # Network
    echo ""
    echo "🌐 Network:"
    echo "  Port 80: $(sudo netstat -tlnp | grep :80 | wc -l)"
    echo "  Port 8080: $(sudo netstat -tlnp | grep :8080 | wc -l)"
    echo "  Connections: $(sudo netstat -an | grep ESTABLISHED | wc -l)"
    
    echo ""
    echo "Press Ctrl+C to exit. Refreshing in 5 seconds..."
    sleep 5
done
```

---

## Useful Links

- Oracle Cloud Console: https://cloud.oracle.com
- Service Health: https://status.oracle.com
- Nginx Docs: https://nginx.org/en/docs/
- Spring Boot Actuator: http://localhost:8080/actuator/
- Systemd Manual: `man systemd.unit`
