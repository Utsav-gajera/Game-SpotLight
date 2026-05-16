# Complete Free Deployment Guide - GameSpotlight

## Architecture Overview
```
Internet
  ↓
Vercel (Frontend - FREE)
  ↓
Oracle Cloud VM (Backend - FREE)
  ├── Nginx (Reverse Proxy)
  ├── Gateway (Port 8080)
  ├── Auth Service (Port 8087)
  ├── Game Service (Port 8082)
  ├── Purchase Service (Port 8083)
  ├── Wishlist Service (Port 8084)
  ├── Notification Service (Port 8086)
  └── Storage Service (Port 8085)
  ↓
External Managed Services (Already configured)
  ├── Aiven PostgreSQL (Auth, Purchase DBs)
  ├── MongoDB Atlas (Game, Wishlist, Notification DBs)
  ├── Aiven Kafka (Event streaming)
  ├── Aiven Redis (Caching)
  └── Supabase (File storage)
```

---

## PART 1: ORACLE CLOUD SETUP (Completely Free)

### Step 1: Create Oracle Cloud Account
1. Go to: https://www.oracle.com/cloud/free/
2. Click "Start for Free"
3. Sign up (credit card required for verification, won't charge)
4. Verify email and phone
5. Login to Oracle Cloud Console

### Step 2: Create a Virtual Machine (Always Free)
1. Navigate: **Compute** → **Instances**
2. Click **Create Instance**
3. Configure:
   - **Name**: gamespotlight-server
   - **Image**: Ubuntu 22.04 (Always Free eligible)
   - **Shape**: Ampere (Always Free) - 4 OCPUs, 24 GB RAM
   - **VCN**: Create new or use default
   - **Subnet**: Public subnet (so it gets public IP)
   - **SSH Key**: Generate, download and save `gamespotlight-key.key`
4. Click **Create**
5. Wait for instance to be "Running" (5-10 minutes)
6. Copy the **Public IP Address** (e.g., `140.238.123.45`)

### Step 3: Configure Firewall (Oracle Cloud)
1. Click on your instance
2. Go to **Attached VNCs** → Your VNC name
3. **Security Lists** → **Default Security List**
4. Click **Add Ingress Rule** for each:
   - **Port 22** (SSH): Source `0.0.0.0/0` (your IP is better)
   - **Port 80** (HTTP): Source `0.0.0.0/0`
   - **Port 443** (HTTPS): Source `0.0.0.0/0`
5. Click **Add Ingress Rule**

---

## PART 2: SERVER SETUP (Ubuntu VM)

### Step 4: SSH Into Your Server
```powershell
# On Windows PowerShell, save key as gamespotlight-key.key in a folder
# Change permissions and connect
cd C:\path\to\key\folder
icacls gamespotlight-key.key /inheritance:r /grant:r "$env:USERNAME`:(F)"
ssh -i gamespotlight-key.key ubuntu@YOUR_PUBLIC_IP
```

### Step 5: Update System & Install Dependencies
```bash
sudo apt update && sudo apt upgrade -y
sudo apt install -y nginx openjdk-17-jdk maven git curl wget
```

### Step 6: Install Docker (Optional, for easier service management)
```bash
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker ubuntu
# Logout and login again
exit
ssh -i gamespotlight-key.key ubuntu@YOUR_PUBLIC_IP
```

---

## PART 3: CLONE & BUILD SERVICES

### Step 7: Clone Repository
```bash
cd /opt
sudo git clone https://github.com/YOUR_USERNAME/GameSpotlight.git
sudo chown -R ubuntu:ubuntu /opt/GameSpotlight
cd /opt/GameSpotlight/Game-SpotLight
```

### Step 8: Set Up Environment Variables
Create `.env` file in `/opt/GameSpotlight/Game-SpotLight`:
```bash
cat > /opt/GameSpotlight/Game-SpotLight/.env << 'EOF'
# PostgreSQL (Aiven)
POSTGRES_JDBC_URL=jdbc:postgresql://gamespotlightdb-gajerautsav08-9ccc.h.aivencloud.com:22498/auth_db?sslmode=require
POSTGRES_USER=avnadmin
POSTGRES_PASSWORD=YOUR_POSTGRES_PASSWORD

# MongoDB (Atlas)
MONGO_URI=mongodb+srv://utsav:PASSWORD@cluster0.yslwcbv.mongodb.net/?retryWrites=true&w=majority

# JWT
JWT_SECRET=YOUR_JWT_SECRET_KEY
JWT_EXPIRATION_SECONDS=604800

# Kafka (Aiven)
KAFKA_BOOTSTRAP_SERVERS=gamespotlight-gajerautsav08-9ccc.l.aivencloud.com:22511
KAFKA_SECURITY_PROTOCOL=SASL_SSL
KAFKA_SASL_MECHANISM=PLAIN
KAFKA_API_KEY=avnadmin
KAFKA_API_SECRET=AVNS_m-PAMXOKEvHGT-pFb_c
KAFKA_TRUSTSTORE_LOCATION=/opt/GameSpotlight/Game-SpotLight/aiven-broker-cert.pem

# Redis (Aiven)
REDIS_HOST=13.200.236.38
REDIS_PORT=14985
REDIS_USERNAME=default
REDIS_PASSWORD=YOUR_REDIS_PASSWORD
REDIS_SSL=false

# Supabase (Storage)
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_KEY=YOUR_SERVICE_ROLE_KEY
SUPABASE_ANON_KEY=YOUR_ANON_KEY
SUPABASE_BUCKET_GAME_FILES=game-files
SUPABASE_BUCKET_GAME_IMAGES=game-images

# Brevo SMTP (Free email)
BREVO_SMTP_USERNAME=aaf372001@smtp-brevo.com
BREVO_SMTP_PASSWORD=REPLACE_WITH_BREVO_PASSWORD
BREVO_SMTP_HOST=smtp-relay.brevo.com
BREVO_SMTP_PORT=587
MAIL_FROM_ADDRESS=your-email@gmail.com

# Service Ports
AUTH_PORT=8087
GAME_PORT=8082
PURCHASE_PORT=8083
WISHLIST_PORT=8084
NOTIFICATION_PORT=8086
STORAGE_PORT=8085
GATEWAY_PORT=8080

# Service URLs
AUTH_USER_SERVICE_URL=http://localhost:8087
GAME_SERVICE_URL=http://localhost:8082
PURCHASE_SERVICE_URL=http://localhost:8083
WISHLIST_SERVICE_URL=http://localhost:8084
NOTIFICATION_SERVICE_URL=http://localhost:8086
STORAGE_SERVICE_URL=http://localhost:8085
EOF
```

### Step 9: Download Kafka Certificate
```bash
cd /opt/GameSpotlight/Game-SpotLight
# Get from Aiven console or:
curl -o aiven-broker-cert.pem https://api.aiven.io/path/to/cert
# Or add to services/.env files directly
```

### Step 10: Build All Services
```bash
cd /opt/GameSpotlight/Game-SpotLight
mvn clean package -DskipTests

# Takes ~15-20 minutes. Output JARs will be in:
# services/*/target/*-SNAPSHOT.jar
```

---

## PART 4: RUN SERVICES (Systemd Services)

### Step 11: Create Systemd Service Files

**Auth Service** (`/etc/systemd/system/auth-service.service`):
```bash
sudo tee /etc/systemd/system/auth-service.service > /dev/null << 'EOF'
[Unit]
Description=GameSpotlight Auth Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/GameSpotlight/Game-SpotLight
EnvironmentFile=/opt/GameSpotlight/Game-SpotLight/.env
ExecStart=/usr/bin/java -jar services/auth-user-service/target/auth-user-service-0.0.1-SNAPSHOT.jar \
  --server.port=${AUTH_PORT} \
  --spring.datasource.url=${POSTGRES_JDBC_URL} \
  --spring.datasource.username=${POSTGRES_USER} \
  --spring.datasource.password=${POSTGRES_PASSWORD}
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

**Game Service**:
```bash
sudo tee /etc/systemd/system/game-service.service > /dev/null << 'EOF'
[Unit]
Description=GameSpotlight Game Service
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/GameSpotlight/Game-SpotLight
EnvironmentFile=/opt/GameSpotlight/Game-SpotLight/.env
ExecStart=/usr/bin/java -jar services/game-service/target/game-service-0.0.1-SNAPSHOT.jar \
  --server.port=${GAME_PORT}
Restart=always
RestartSec=5

[Install]
WantedBy=multi-user.target
EOF
```

Repeat for: **Purchase**, **Wishlist**, **Notification**, **Storage**, **Gateway**

### Step 12: Enable & Start Services
```bash
sudo systemctl daemon-reload
sudo systemctl enable auth-service game-service purchase-service wishlist-service notification-service storage-service gateway
sudo systemctl start auth-service game-service purchase-service wishlist-service notification-service storage-service gateway

# Check status
sudo systemctl status auth-service
sudo journalctl -u auth-service -f  # View logs
```

---

## PART 5: NGINX CONFIGURATION (Reverse Proxy)

### Step 13: Configure Nginx
```bash
sudo tee /etc/nginx/sites-available/gamespotlight > /dev/null << 'EOF'
upstream backend_gateway {
    server localhost:8080;
}

upstream auth_service {
    server localhost:8087;
}

upstream game_service {
    server localhost:8082;
}

upstream purchase_service {
    server localhost:8083;
}

upstream wishlist_service {
    server localhost:8084;
}

upstream notification_service {
    server localhost:8086;
}

upstream storage_service {
    server localhost:8085;
}

server {
    listen 80;
    server_name YOUR_DOMAIN_OR_IP;

    # For Let's Encrypt (optional later)
    location /.well-known/acme-challenge/ {
        root /var/www/certbot;
    }

    # Main API Gateway
    location /api/ {
        proxy_pass http://backend_gateway/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 30s;
        proxy_connect_timeout 30s;
    }

    # Direct service endpoints (optional)
    location /auth/ {
        proxy_pass http://auth_service/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /games/ {
        proxy_pass http://game_service/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # Health checks (optional)
    location /health {
        proxy_pass http://backend_gateway/actuator/health;
    }

    # Deny access to sensitive paths
    location ~ /admin/ {
        deny all;
    }
}
EOF
```

### Step 14: Enable Nginx
```bash
sudo ln -s /etc/nginx/sites-available/gamespotlight /etc/nginx/sites-enabled/
sudo nginx -t  # Test config
sudo systemctl restart nginx
sudo systemctl enable nginx
```

---

## PART 6: FRONTEND DEPLOYMENT (Vercel - FREE)

### Step 15: Deploy Frontend to Vercel
1. Go to: https://vercel.com
2. Sign up with GitHub
3. Click **Import Project**
4. Select your GameSpotlight repository
5. Configure:
   - **Framework**: Vite
   - **Root Directory**: `Game-SpotLight/client`
   - **Environment Variables**:
     ```
     VITE_SUPABASE_URL=https://YOUR_PROJECT.supabase.co
     VITE_SUPABASE_ANON_KEY=YOUR_ANON_KEY
     VITE_API_BASE_URL=https://YOUR_DOMAIN/api
     ```
6. Click **Deploy**
7. Vercel gives you: `https://gamespotlight.vercel.app` (or custom domain)

---

## PART 7: CONFIGURE DNS (Optional)

### Step 16: Point Domain to Oracle VM
If you have a domain (Namecheap, GoDaddy, etc):
1. Go to DNS settings
2. Create **A Record**:
   ```
   @ (or www)  A  YOUR_ORACLE_PUBLIC_IP  (e.g., 140.238.123.45)
   ```
3. Wait 5-15 minutes for propagation
4. Update Nginx config:
   ```bash
   server_name yourdomain.com www.yourdomain.com;
   ```
5. Restart Nginx: `sudo systemctl restart nginx`

---

## PART 8: SSL CERTIFICATE (FREE with Let's Encrypt)

### Step 17: Install Certbot
```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot certonly --nginx -d yourdomain.com -d www.yourdomain.com
# OR for IP-only (no SSL):
# Skip this step, use HTTP only
```

### Step 18: Update Nginx for HTTPS (if you got certificate)
```bash
sudo tee /etc/nginx/sites-available/gamespotlight > /dev/null << 'EOF'
server {
    listen 80;
    server_name yourdomain.com www.yourdomain.com;
    return 301 https://$server_name$request_uri;
}

server {
    listen 443 ssl;
    server_name yourdomain.com www.yourdomain.com;

    ssl_certificate /etc/letsencrypt/live/yourdomain.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/yourdomain.com/privkey.pem;

    # ... rest of config same as before
}
EOF
```

---

## PART 9: DATABASE MIGRATIONS

### Step 19: Run Flyway Migrations
```bash
cd /opt/GameSpotlight/Game-SpotLight

# Auth service migration
mvn -f services/auth-user-service/pom.xml \
  -Dflyway.url="jdbc:postgresql://gamespotlightdb-gajerautsav08-9ccc.h.aivencloud.com:22498/auth_db?sslmode=require" \
  -Dflyway.user="avnadmin" \
  -Dflyway.password="YOUR_PASSWORD" \
  flyway:migrate

# Purchase service migration
mvn -f services/purchase-service/pom.xml \
  -Dflyway.url="jdbc:postgresql://gamespotlightdb-gajerautsav08-9ccc.h.aivencloud.com:22498/purchase_db?sslmode=require" \
  -Dflyway.user="avnadmin" \
  -Dflyway.password="YOUR_PASSWORD" \
  flyway:migrate
```

---

## PART 10: MONITORING & LOGS

### Step 20: View Service Logs
```bash
# Auth service
sudo journalctl -u auth-service -f

# All services
sudo journalctl -f

# Nginx access logs
tail -f /var/log/nginx/access.log
tail -f /var/log/nginx/error.log
```

### Step 21: Restart Services (if needed)
```bash
sudo systemctl restart auth-service
sudo systemctl restart all  # Or individual services
```

---

## PART 11: TESTING

### Step 22: Test Endpoints
```bash
# Health check
curl http://YOUR_IP/health

# Test API gateway
curl http://YOUR_IP/api/auth/session

# Frontend
Open browser: https://yourdomain.com or http://YOUR_IP (if Vercel deployed)
```

---

## TROUBLESHOOTING

### Service Won't Start
```bash
# Check logs
sudo journalctl -u auth-service -n 50
```

### Nginx Not Working
```bash
# Test config
sudo nginx -t
# Check if port 80 is blocked
sudo netstat -tlnp | grep 80
```

### Database Connection Issues
```bash
# Verify env variables loaded
cat /proc/$(pgrep -f auth-service)/environ | tr '\0' '\n' | grep POSTGRES
```

### Services Too Slow (Free Tier)
- Oracle Free Tier: 4 OCPUs, 24 GB RAM (enough for ~3-4 services)
- Upgrade to paid if needed (still cheap: $2-5/month per service)
- Or use Railway.app ($5/month credit) or Render.com

---

## FREE TIER SUMMARY

| Component | Service | Cost | Limits |
|-----------|---------|------|--------|
| **Backend VMs** | Oracle Cloud | FREE | 2 Always-Free VMs |
| **Database (Postgres)** | Aiven | FREE | 20GB storage, 10 connections |
| **Database (MongoDB)** | MongoDB Atlas | FREE | 512MB shared cluster |
| **Cache (Redis)** | Aiven | FREE | 100MB |
| **Message Queue** | Kafka Aiven | FREE | 100MB |
| **File Storage** | Supabase | FREE | 1GB |
| **Frontend** | Vercel | FREE | Unlimited |
| **Email** | Brevo | FREE | 300/day |
| **Domain** | Namecheap | $0.88 | First year |
| **SSL** | Let's Encrypt | FREE | Valid 3 months |
| **TOTAL** | — | ~$1/year | Full app! |

---

## DEPLOYMENT CHECKLIST

- [ ] Oracle Cloud account created
- [ ] VM instance running with public IP
- [ ] Security groups configured (ports 22, 80, 443)
- [ ] SSH access working
- [ ] Dependencies installed (Java, Maven, Nginx)
- [ ] Repository cloned to `/opt`
- [ ] `.env` file created with all credentials
- [ ] Services built with `mvn clean package`
- [ ] Systemd services created & enabled
- [ ] All services running (`systemctl status`)
- [ ] Nginx configured & running
- [ ] Database migrations completed
- [ ] Frontend deployed to Vercel
- [ ] API endpoints responding
- [ ] Logs checking for errors
- [ ] Domain DNS configured (optional)
- [ ] SSL certificate installed (optional)

---

## QUICK START SUMMARY

```bash
# 1. SSH to server
ssh -i gamespotlight-key.key ubuntu@YOUR_IP

# 2. Install & clone
sudo apt update && sudo apt install -y nginx openjdk-17-jdk maven git
cd /opt && sudo git clone YOUR_REPO
sudo chown -R ubuntu:ubuntu /opt/GameSpotlight

# 3. Setup env & build
cd /opt/GameSpotlight/Game-SpotLight
nano .env  # Add all variables
mvn clean package -DskipTests

# 4. Configure services
sudo systemctl create/start auth-service game-service ... (see Step 11-12)

# 5. Configure Nginx
sudo nano /etc/nginx/sites-available/gamespotlight
sudo systemctl restart nginx

# 6. Deploy frontend to Vercel (Step 15)

# 7. Test
curl http://YOUR_IP/health
```

Done! Your app is live on free tier! 🚀
