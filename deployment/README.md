# 📦 Deployment Files Summary

This directory contains everything needed to deploy GameSpotlight on Oracle Cloud (free tier) with Nginx + Vercel frontend.

## 📋 Files Included

### 1. **DEPLOYMENT_GUIDE.md** (START HERE)
Complete step-by-step guide covering:
- Oracle Cloud VM setup
- Ubuntu dependencies installation
- Environment variables configuration
- Building all 7 Java services
- Systemd service configuration
- Nginx reverse proxy setup
- Vercel frontend deployment
- SSL certificate (Let's Encrypt)
- Database migrations
- Troubleshooting basics

**How to use:**
- Read sections in order
- Follow each step carefully
- Replace placeholders (YOUR_IP, YOUR_DOMAIN, etc.)

---

### 2. **deploy.sh** (AUTOMATION)
Automated deployment script that:
- Checks all prerequisites
- Builds all services (`mvn clean package`)
- Creates systemd service files
- Enables and starts all services
- Configures Nginx
- Optionally runs database migrations

**How to use:**
```bash
cd /opt/GameSpotlight/Game-SpotLight
bash deployment/deploy.sh
```

**Prerequisites:**
- Java 17, Maven, Nginx installed
- `.env` file populated with credentials
- Running on Ubuntu/Debian

---

### 3. **.env.template** (CREDENTIALS)
Template file with all required environment variables:
- PostgreSQL (Aiven)
- MongoDB (Atlas/Aiven)
- JWT configuration
- Kafka (Aiven)
- Redis (Aiven)
- Supabase (Storage)
- Brevo (Email)
- Service ports and URLs

**How to use:**
```bash
cp deployment/.env.template .env
nano .env  # Fill in your credentials
```

**Critical values to update:**
- `POSTGRES_PASSWORD` - from Aiven
- `MONGO_URI` - from MongoDB Atlas
- `JWT_SECRET` - generate strong key
- `KAFKA_API_SECRET` - from Aiven
- `REDIS_PASSWORD` - from Aiven
- `AUTH_SUPABASE_URL`, `AUTH_SUPABASE_ANON_KEY` - auth Supabase project
- `STORAGE_SUPABASE_URL`, `STORAGE_SUPABASE_KEY` - storage Supabase project
- `BREVO_SMTP_PASSWORD` - from Brevo account
- `MAIL_FROM_ADDRESS` - your email

---

### 4. **systemd-templates.sh** (SERVICE MANAGEMENT)
Script to create systemd service files for all 7 microservices:
- auth-service (Port 8087)
- game-service (Port 8082)
- purchase-service (Port 8083)
- wishlist-service (Port 8084)
- notification-service (Port 8086)
- storage-service (Port 8085)
- gateway (Port 8080)

Each service:
- Starts automatically on boot
- Auto-restarts if it crashes
- Logs to journald (systemd)
- Configurable memory limits

**How to use:**
```bash
bash deployment/systemd-templates.sh
```

**Manual alternative:**
```bash
sudo systemctl enable auth-service
sudo systemctl start auth-service
sudo systemctl status auth-service
```

---

### 5. **nginx.conf** (REVERSE PROXY)
Production-ready Nginx configuration featuring:
- Upstream definitions for all 7 services
- API gateway routing (`/api/*`)
- Direct service routes (debugging)
- Health check endpoint
- CORS headers
- Security headers
- Rate limiting setup
- File upload handling (500MB limit)
- HTTPS/SSL configuration
- Gzip compression

**How to use:**
```bash
# Copy to Nginx
sudo cp deployment/nginx.conf /etc/nginx/sites-available/gamespotlight

# Enable it
sudo ln -s /etc/nginx/sites-available/gamespotlight /etc/nginx/sites-enabled/

# Test config
sudo nginx -t

# Reload
sudo systemctl restart nginx
```

**Customizations needed:**
- Replace `yourdomain.com` with your actual domain/IP
- Uncomment HTTPS section after getting SSL certificate
- Adjust `client_max_body_size` if needed

---

### 6. **TROUBLESHOOTING.md** (QUICK REFERENCE)
Comprehensive troubleshooting guide with:
- Quick command reference
- Common error scenarios
- Database connection testing
- Performance monitoring
- Emergency recovery procedures
- Memory/resource optimization
- Monitoring script

**When to use:**
- Service won't start? See "Service won't start" section
- API slow? See "Performance Tuning"
- Frontend can't reach backend? See "CORS settings"
- Need to check logs? See "View Logs"

---

## 🚀 Quick Start (30 minutes)

### On Your Local Machine

1. **Download files:**
   ```bash
   cd Game-SpotLight
   ls -la deployment/
   ```

2. **Prepare credentials:**
   - Copy credentials from Aiven (PostgreSQL, Kafka, Redis)
   - Copy MongoDB URI from Atlas
   - Copy Supabase URL and keys
   - Generate strong JWT secret

### On Oracle Cloud VM (SSH)

3. **Clone repo & install dependencies:**
   ```bash
   git clone https://github.com/YOUR_USERNAME/GameSpotlight.git
   cd Game-SpotLight
   
   sudo apt update && sudo apt install -y nginx openjdk-17-jdk maven
   ```

4. **Setup credentials:**
   ```bash
   cp deployment/.env.template .env
   nano .env  # Fill in all values
   ```

5. **One-command deploy:**
   ```bash
   bash deployment/deploy.sh
   ```

6. **Deploy frontend:**
   - Go to Vercel.com
   - Import GameSpotlight repo
   - Set `VITE_API_BASE_URL` to your domain
   - Deploy!

7. **Test:**
   ```bash
   curl http://YOUR_IP/health
   # Should return: {"status":"UP"}
   ```

---

## 📊 Architecture Reference

```
┌─────────────────────────────────────────────────────────┐
│                     CLIENT (Vercel)                     │
│            https://gamespotlight.vercel.app             │
└────────────────────────┬────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────┐
│                  NGINX REVERSE PROXY                    │
│                  (Port 80/443)                          │
└────────────────────────┬────────────────────────────────┘
                         │
         ┌───────────────┼────────────────┐
         ▼               ▼                ▼
    ┌─────────┐    ┌──────────┐    ┌──────────┐
    │ Gateway │    │Nginx Logs│    │Load Bal. │
    │ :8080   │    │          │    │          │
    └────┬────┘    └──────────┘    └──────────┘
         │
         ├─────────────────────────────────────────┐
         │                                         │
         ▼                                         ▼
    ┌──────────────┐               ┌──────────────────┐
    │ Auth Service │               │ Game Service     │
    │ :8087        │               │ :8082            │
    └──────────────┘               └──────────────────┘
         │                                 │
         ▼                                 ▼
    ┌──────────────┐               ┌──────────────────┐
    │ PostgreSQL   │               │ MongoDB          │
    │ (Aiven)      │               │ (Atlas)          │
    └──────────────┘               └──────────────────┘

[Similar pattern for Purchase, Wishlist, Notification, Storage services]

         All services connected to:
         • Kafka (Aiven) - Event streaming
         • Redis (Aiven) - Caching
         • Supabase - File storage
         • Brevo SMTP - Email
```

---

## ⚠️ Important Notes

### Free Tier Limitations
- **Oracle Cloud**: 2 always-free VMs (4 OCPUs, 24GB RAM each)
- **Aiven Postgres**: 20GB storage, 10 connections
- **MongoDB Atlas**: 512MB shared cluster (sufficient for testing)
- **Brevo Email**: 300 emails/day free
- **Supabase Storage**: 1GB free

### Before Production
1. ✅ Test all services thoroughly
2. ✅ Run load testing
3. ✅ Configure proper backups (Aiven handles this)
4. ✅ Set up monitoring/alerts
5. ✅ Enable HTTPS with SSL certificate
6. ✅ Implement rate limiting in Nginx
7. ✅ Set up log aggregation (optional)

### Scaling (if needed)
- **Free to paid**: $2-5/month per service on Render/Railway
- **Custom domain**: $1/year (Namecheap)
- **More storage**: Scale up Aiven/MongoDB (paid tiers)

---

## 🔐 Security Checklist

Before going live:
- [ ] All `.env` variables filled in
- [ ] `.env` file never committed to git
- [ ] `.gitignore` includes `.env`
- [ ] JWT secret is cryptographically strong
- [ ] HTTPS enabled (Let's Encrypt certificate)
- [ ] Database backups configured (Aiven auto-backup)
- [ ] Firewall rules restrict access
- [ ] API rate limiting enabled in Nginx
- [ ] CORS properly configured
- [ ] Sensitive endpoints protected (admin, actuator)

---

## 📞 Support Resources

- **Oracle Cloud**: https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm
- **Nginx**: https://nginx.org/en/docs/
- **Systemd**: `man systemd.unit`
- **Spring Boot**: https://spring.io/projects/spring-boot
- **Vercel**: https://vercel.com/docs
- **Your Repo Issues**: Create GitHub issue with deployment tag

---

## ✅ Deployment Checklist

- [ ] Oracle Cloud VM running
- [ ] Public IP assigned
- [ ] Security groups configured (22, 80, 443)
- [ ] SSH key working
- [ ] Packages installed (Java, Maven, Nginx)
- [ ] Repository cloned
- [ ] `.env` file created with all credentials
- [ ] `mvn clean package -DskipTests` successful
- [ ] Systemd services created and running
- [ ] Nginx configured and started
- [ ] All 7 services responding on localhost
- [ ] Database migrations completed
- [ ] Frontend deployed to Vercel
- [ ] API endpoints accessible from frontend
- [ ] SSL certificate installed
- [ ] Domain DNS configured
- [ ] Monitoring/alerts setup
- [ ] Backups configured

---

**Last Updated**: May 16, 2026
**Status**: Production Ready ✅
