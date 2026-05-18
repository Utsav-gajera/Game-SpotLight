#!/bin/bash
# Quick Deployment Script for Oracle Cloud VM
# Run this AFTER cloning the repo and filling in .env file
# Usage: bash deploy.sh

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

echo "🚀 GameSpotlight Deployment Script"
echo "===================================="
echo ""

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check prerequisites
echo "📋 Checking prerequisites..."

if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java not installed${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}❌ Maven not installed${NC}"
    exit 1
fi

if ! command -v nginx &> /dev/null; then
    echo -e "${RED}❌ Nginx not installed${NC}"
    exit 1
fi

echo -e "${GREEN}✓ All prerequisites installed${NC}"

# Check if .env file exists
if [ ! -f "$ROOT_DIR/.env" ]; then
    echo -e "${RED}❌ .env file not found!${NC}"
    echo "Copy deployment/.env.template to .env and fill in your credentials"
    exit 1
fi

echo -e "${GREEN}✓ .env file exists${NC}"
echo ""

# Build all services
echo "🔨 Building services..."

SERVICES=(
    "auth-user-service"
    "game-service"
    "purchase-service"
    "wishlist-service"
    "notification-service"
    "storage-service"
    "gateway"
)

for service_dir in "${SERVICES[@]}"; do
    echo "  • Building ${service_dir}"
    (cd "$ROOT_DIR/services/$service_dir" && mvn clean package -DskipTests -q)
done

echo -e "${GREEN}✓ Build successful${NC}"
echo ""

# Create systemd services
echo "📝 Creating systemd service files..."

SYSTEMD_SERVICES=(
    "auth-service:8087:auth-user-service"
    "game-service:8082:game-service"
    "purchase-service:8083:purchase-service"
    "wishlist-service:8084:wishlist-service"
    "notification-service:8086:notification-service"
    "storage-service:8085:storage-service"
    "gateway:8080:gateway"
)

for service_config in "${SYSTEMD_SERVICES[@]}"; do
    IFS=':' read -r service_name port jar_name <<< "$service_config"
    
    sudo tee /etc/systemd/system/${service_name}.service > /dev/null << EOF
[Unit]
Description=GameSpotlight $service_name
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=$ROOT_DIR
EnvironmentFile=$ROOT_DIR/.env
ExecStart=/usr/bin/java -Xmx512m -jar services/${jar_name}/target/${jar_name}-0.0.1-SNAPSHOT.jar --server.port=\${${service_name^^}_PORT:-${port}}
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${service_name}

[Install]
WantedBy=multi-user.target
EOF
    
    echo "  ✓ ${service_name}"
done

echo -e "${GREEN}✓ Systemd services created${NC}"
echo ""

# Reload systemd
echo "🔄 Reloading systemd..."
sudo systemctl daemon-reload
echo -e "${GREEN}✓ Systemd reloaded${NC}"
echo ""

# Enable services
echo "⚙️  Enabling services..."
for service_config in "${SYSTEMD_SERVICES[@]}"; do
    IFS=':' read -r service_name _ _ <<< "$service_config"
    sudo systemctl enable ${service_name}.service
done
echo -e "${GREEN}✓ Services enabled${NC}"
echo ""

# Start services
echo "▶️  Starting services..."
for service_config in "${SYSTEMD_SERVICES[@]}"; do
    IFS=':' read -r service_name _ _ <<< "$service_config"
    sudo systemctl start ${service_name}.service
    sleep 2
    if sudo systemctl is-active --quiet ${service_name}.service; then
        echo -e "  ${GREEN}✓${NC} ${service_name}"
    else
        echo -e "  ${RED}✗${NC} ${service_name} (failed to start)"
    fi
done
echo ""

# Configure Nginx
echo "🌐 Configuring Nginx..."
sudo cp "$SCRIPT_DIR/nginx.conf" /etc/nginx/sites-available/gamespotlight
sudo ln -sf /etc/nginx/sites-available/gamespotlight /etc/nginx/sites-enabled/gamespotlight

# Test Nginx config
if sudo nginx -t &> /dev/null; then
    echo -e "${GREEN}✓ Nginx config valid${NC}"
    sudo systemctl restart nginx
    echo -e "${GREEN}✓ Nginx restarted${NC}"
else
    echo -e "${RED}❌ Nginx config error${NC}"
    exit 1
fi
echo ""

# Run migrations (optional)
read -p "Run database migrations? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Running migrations..."
    
    # Get credentials from .env
    source "$ROOT_DIR/.env"
    
    echo "  Running auth-user-service migration..."
    mvn -f services/auth-user-service/pom.xml \
      -Dflyway.url="${POSTGRES_JDBC_URL}" \
      -Dflyway.user="${POSTGRES_USER}" \
      -Dflyway.password="${POSTGRES_PASSWORD}" \
      flyway:migrate -q 2>/dev/null && echo -e "    ${GREEN}✓${NC} Complete" || echo -e "    ${YELLOW}⚠${NC} Check logs"
    
    echo "  Running purchase-service migration..."
    # Adjust URL if different DB
    mvn -f services/purchase-service/pom.xml \
      -Dflyway.url="${POSTGRES_JDBC_URL}" \
      -Dflyway.user="${POSTGRES_USER}" \
      -Dflyway.password="${POSTGRES_PASSWORD}" \
      flyway:migrate -q 2>/dev/null && echo -e "    ${GREEN}✓${NC} Complete" || echo -e "    ${YELLOW}⚠${NC} Check logs"
fi
echo ""

# Summary
echo "╔════════════════════════════════════════════════════════╗"
echo "║        🎉 Deployment Complete! 🎉                      ║"
echo "╠════════════════════════════════════════════════════════╣"
echo "║                                                        ║"
echo "║ 📊 Services:                                           ║"
for service_config in "${SERVICES[@]}"; do
    IFS=':' read -r service_name port _ <<< "$service_config"
    printf "║   %-35s Port %-10s ║\n" "$service_name" "$port"
done
echo "║                                                        ║"
echo "║ 🌐 API Gateway: http://localhost/api/                 ║"
echo "║ 📝 Check logs: sudo journalctl -u auth-service -f     ║"
echo "║ 📊 Status: sudo systemctl status auth-service         ║"
echo "║ ♻️  Restart service: sudo systemctl restart game-svc  ║"
echo "║                                                        ║"
echo "║ ⏭️  Next Steps:                                         ║"
echo "║   1. Deploy frontend to Vercel (Step 15)              ║"
echo "║   2. Update Nginx config with your domain             ║"
echo "║   3. Get SSL cert: sudo certbot certonly --nginx      ║"
echo "║   4. Test: curl http://localhost/health               ║"
echo "║                                                        ║"
echo "╚════════════════════════════════════════════════════════╝"
