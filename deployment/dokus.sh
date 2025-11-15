#!/bin/bash
#
# Dokus Server Installation and Startup Script
# Supports: macOS, Linux
# Usage: ./dokus.sh
#

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Banner
echo -e "${BLUE}"
cat << "EOF"
╔════════════════════════════════════════════╗
║                                            ║
║              DOKUS SERVER                  ║
║     Financial Management Platform          ║
║                                            ║
╚════════════════════════════════════════════╝
EOF
echo -e "${NC}"

# Detect OS
OS="unknown"
if [[ "$OSTYPE" == "darwin"* ]]; then
    OS="macos"
    echo -e "${GREEN}✓ Detected: macOS${NC}"
elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
    OS="linux"
    echo -e "${GREEN}✓ Detected: Linux${NC}"
else
    echo -e "${RED}✗ Unsupported OS: $OSTYPE${NC}"
    echo "This script supports macOS and Linux only."
    echo "For Windows, please use dokus.bat"
    exit 1
fi

# Check if Docker is installed
echo ""
echo -e "${BLUE}[1/6] Checking Docker installation...${NC}"

if ! command -v docker &> /dev/null; then
    echo -e "${YELLOW}⚠ Docker is not installed${NC}"
    echo ""
    read -p "Would you like to install Docker now? (y/n): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${BLUE}Installing Docker...${NC}"

        if [[ "$OS" == "macos" ]]; then
            if ! command -v brew &> /dev/null; then
                echo -e "${YELLOW}Installing Homebrew first...${NC}"
                /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
            fi
            brew install --cask docker
            echo -e "${GREEN}✓ Docker installed via Homebrew${NC}"
            echo -e "${YELLOW}⚠ Please start Docker Desktop from Applications, then run this script again${NC}"
            exit 0
        elif [[ "$OS" == "linux" ]]; then
            # Install Docker on Linux
            curl -fsSL https://get.docker.com -o get-docker.sh
            sudo sh get-docker.sh
            sudo usermod -aG docker $USER
            rm get-docker.sh
            echo -e "${GREEN}✓ Docker installed${NC}"
            echo -e "${YELLOW}⚠ Please log out and log back in for group changes to take effect${NC}"
            echo -e "${YELLOW}⚠ Then run this script again${NC}"
            exit 0
        fi
    else
        echo -e "${RED}✗ Docker is required to run Dokus${NC}"
        echo "Please install Docker manually from: https://docker.com"
        exit 1
    fi
else
    echo -e "${GREEN}✓ Docker is installed${NC}"
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo -e "${RED}✗ Docker is not running${NC}"
    if [[ "$OS" == "macos" ]]; then
        echo "Please start Docker Desktop and run this script again"
    else
        echo "Please start Docker daemon: sudo systemctl start docker"
    fi
    exit 1
fi
echo -e "${GREEN}✓ Docker is running${NC}"

# Check if Docker Compose is installed
if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
    echo -e "${YELLOW}⚠ Docker Compose not found, installing...${NC}"
    if [[ "$OS" == "macos" ]]; then
        brew install docker-compose
    elif [[ "$OS" == "linux" ]]; then
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
    fi
    echo -e "${GREEN}✓ Docker Compose installed${NC}"
else
    echo -e "${GREEN}✓ Docker Compose is installed${NC}"
fi

# Check for .env file
echo ""
echo -e "${BLUE}[2/6] Checking configuration...${NC}"

if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠ .env file not found${NC}"
    echo ""

    if [ -f .env.example ]; then
        cp .env.example .env
        echo -e "${GREEN}✓ Created .env from .env.example${NC}"
    else
        echo -e "${RED}✗ .env.example not found${NC}"
        exit 1
    fi

    echo ""
    echo -e "${YELLOW}════════════════════════════════════════════${NC}"
    echo -e "${YELLOW}IMPORTANT: Configure your environment${NC}"
    echo -e "${YELLOW}════════════════════════════════════════════${NC}"
    echo ""
    echo "A .env file has been created with default values."
    echo "You MUST edit it and set secure passwords before continuing."
    echo ""
    echo "Required variables to set:"
    echo "  - DB_PASSWORD"
    echo "  - REDIS_PASSWORD"
    echo "  - RABBITMQ_PASSWORD"
    echo "  - JWT_SECRET (at least 32 characters)"
    echo ""
    read -p "Press Enter to open .env in your default editor..."

    if [[ "$OS" == "macos" ]]; then
        open -e .env
    else
        ${EDITOR:-nano} .env
    fi

    echo ""
    read -p "Have you updated all required passwords? (y/n): " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo -e "${RED}✗ Please configure .env and run this script again${NC}"
        exit 1
    fi
else
    echo -e "${GREEN}✓ .env file exists${NC}"
fi

# Configure Docker for insecure registry
echo ""
echo -e "${BLUE}[3/6] Configuring Docker registry...${NC}"

DAEMON_JSON="/etc/docker/daemon.json"
REGISTRY="94.111.226.82:5000"

configure_insecure_registry() {
    if [[ "$OS" == "macos" ]]; then
        echo -e "${YELLOW}⚠ On macOS, please configure insecure registry manually:${NC}"
        echo "  1. Open Docker Desktop"
        echo "  2. Go to Settings → Docker Engine"
        echo "  3. Add the following to the JSON config:"
        echo "     \"insecure-registries\": [\"$REGISTRY\"]"
        echo "  4. Click 'Apply & Restart'"
        echo ""
        read -p "Press Enter after you've configured the registry..."
    elif [[ "$OS" == "linux" ]]; then
        if [ -f "$DAEMON_JSON" ]; then
            # Check if already configured
            if grep -q "$REGISTRY" "$DAEMON_JSON"; then
                echo -e "${GREEN}✓ Insecure registry already configured${NC}"
                return
            fi
        fi

        echo -e "${YELLOW}⚠ Configuring insecure registry (requires sudo)${NC}"

        # Backup existing config
        if [ -f "$DAEMON_JSON" ]; then
            sudo cp "$DAEMON_JSON" "$DAEMON_JSON.backup"
        fi

        # Create or update daemon.json
        echo "{
  \"insecure-registries\": [\"$REGISTRY\"]
}" | sudo tee "$DAEMON_JSON" > /dev/null

        # Restart Docker
        sudo systemctl restart docker
        sleep 3
        echo -e "${GREEN}✓ Docker configured for insecure registry${NC}"
    fi
}

configure_insecure_registry

# Pull latest images
echo ""
echo -e "${BLUE}[4/6] Pulling latest Docker images...${NC}"
echo "This may take a few minutes..."

docker compose pull

echo -e "${GREEN}✓ Images pulled successfully${NC}"

# Start services
echo ""
echo -e "${BLUE}[5/6] Starting Dokus services...${NC}"

docker compose up -d

echo -e "${GREEN}✓ Services started${NC}"

# Wait for services to be healthy
echo ""
echo -e "${BLUE}[6/6] Waiting for services to be ready...${NC}"
echo "This may take up to 2 minutes..."

sleep 10

# Check service health
MAX_RETRIES=30
RETRY_COUNT=0

while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    HEALTHY_COUNT=$(docker compose ps | grep -c "healthy" || true)

    if [ $HEALTHY_COUNT -ge 7 ]; then
        echo -e "${GREEN}✓ All services are healthy${NC}"
        break
    fi

    RETRY_COUNT=$((RETRY_COUNT + 1))
    echo -n "."
    sleep 5
done

echo ""

if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
    echo -e "${YELLOW}⚠ Some services may still be starting${NC}"
    echo "Run 'docker compose ps' to check service status"
fi

# Configure auto-start
echo ""
echo -e "${BLUE}Configure Auto-Start${NC}"
read -p "Would you like Dokus to start automatically on system boot? (y/n): " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    if [[ "$OS" == "macos" ]]; then
        # Create LaunchAgent for macOS
        PLIST_FILE="$HOME/Library/LaunchAgents/com.dokus.server.plist"
        SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/dokus.sh"

        cat > "$PLIST_FILE" << EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>com.dokus.server</string>
    <key>ProgramArguments</key>
    <array>
        <string>/bin/bash</string>
        <string>-c</string>
        <string>cd $(dirname $SCRIPT_PATH) && docker compose up -d</string>
    </array>
    <key>RunAtLoad</key>
    <true/>
    <key>StandardErrorPath</key>
    <string>/tmp/dokus-error.log</string>
    <key>StandardOutPath</key>
    <string>/tmp/dokus-output.log</string>
</dict>
</plist>
EOF

        launchctl load "$PLIST_FILE"
        echo -e "${GREEN}✓ Auto-start configured${NC}"
        echo "To disable: launchctl unload $PLIST_FILE"

    elif [[ "$OS" == "linux" ]]; then
        # Create systemd service
        SERVICE_FILE="/etc/systemd/system/dokus.service"
        WORKING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

        echo -e "${YELLOW}Creating systemd service (requires sudo)${NC}"

        sudo bash -c "cat > $SERVICE_FILE" << EOF
[Unit]
Description=Dokus Server
Requires=docker.service
After=docker.service

[Service]
Type=oneshot
RemainAfterExit=yes
WorkingDirectory=$WORKING_DIR
ExecStart=/usr/bin/docker compose up -d
ExecStop=/usr/bin/docker compose down
User=$USER

[Install]
WantedBy=multi-user.target
EOF

        sudo systemctl daemon-reload
        sudo systemctl enable dokus.service
        echo -e "${GREEN}✓ Auto-start configured${NC}"
        echo "To disable: sudo systemctl disable dokus.service"
    fi
else
    echo "Skipping auto-start configuration"
fi

# Display status
echo ""
echo -e "${GREEN}════════════════════════════════════════════${NC}"
echo -e "${GREEN}✓ Dokus Server is running!${NC}"
echo -e "${GREEN}════════════════════════════════════════════${NC}"
echo ""
echo "Services available at:"
echo "  Auth Service:      http://localhost:6091"
echo "  Invoicing Service: http://localhost:6092"
echo "  Expense Service:   http://localhost:6093"
echo "  Payment Service:   http://localhost:6094"
echo "  Reporting Service: http://localhost:6095"
echo "  Audit Service:     http://localhost:6096"
echo "  Banking Service:   http://localhost:6097"
echo ""
echo "  RabbitMQ UI:       http://localhost:15672"
echo ""
echo "Useful commands:"
echo "  View logs:    docker compose logs -f"
echo "  Stop:         docker compose stop"
echo "  Restart:      docker compose restart"
echo "  Update:       docker compose pull && docker compose up -d"
echo "  Uninstall:    docker compose down -v"
echo ""
