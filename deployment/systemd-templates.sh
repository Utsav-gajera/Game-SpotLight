#!/bin/bash
# Quick script to create all systemd service files
# Run on Oracle Cloud VM: bash systemd-templates.sh

SERVICES=(
    "auth-service:8087:auth-user-service"
    "game-service:8082:game-service"
    "purchase-service:8083:purchase-service"
    "wishlist-service:8084:wishlist-service"
    "notification-service:8086:notification-service"
    "storage-service:8085:storage-service"
    "gateway:8080:gateway"
)

for service_config in "${SERVICES[@]}"; do
    IFS=':' read -r service_name port jar_name <<< "$service_config"
    
    echo "Creating systemd service for $service_name..."
    
    sudo tee /etc/systemd/system/${service_name}.service > /dev/null << EOF
[Unit]
Description=GameSpotlight $service_name
After=network.target
Wants=network-online.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/opt/GameSpotlight/Game-SpotLight
EnvironmentFile=/opt/GameSpotlight/Game-SpotLight/.env
ExecStart=/usr/bin/java -Xmx512m -jar services/${jar_name}/target/${jar_name}-0.0.1-SNAPSHOT.jar --server.port=${port}
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=${service_name}

[Install]
WantedBy=multi-user.target
EOF
done

echo "Reloading systemd daemon..."
sudo systemctl daemon-reload

echo "Enabling services..."
for service_config in "${SERVICES[@]}"; do
    IFS=':' read -r service_name _ _ <<< "$service_config"
    sudo systemctl enable ${service_name}.service
    echo "✓ ${service_name} enabled"
done

echo "Starting services..."
for service_config in "${SERVICES[@]}"; do
    IFS=':' read -r service_name _ _ <<< "$service_config"
    sudo systemctl start ${service_name}.service
    echo "✓ ${service_name} started"
done

echo ""
echo "All services created and started!"
echo ""
echo "Check status with:"
echo "  sudo systemctl status auth-service"
echo "  sudo journalctl -u auth-service -f"
echo "  sudo systemctl list-units --type=service | grep gamespotlight"
