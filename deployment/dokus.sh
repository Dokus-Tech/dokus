#!/bin/bash
#
# Dokus Cloud Management Script
# Supports: macOS, Linux
# Usage: ./dokus.sh [command]
#

set -e

# Change to script directory
cd "$(dirname "$0")"

# Check for NO_COLOR environment variable
if [[ -n "${NO_COLOR}" ]]; then
    USE_COLOR=false
else
    USE_COLOR=true
fi

# Modern Pastel Color Palette
# Using $'...' syntax for proper escape sequence interpretation on macOS
if [[ "$USE_COLOR" == true ]]; then
    SOFT_BLUE=$'\033[38;2;130;170;255m'
    SOFT_GREEN=$'\033[38;2;150;220;180m'
    SOFT_RED=$'\033[38;2;255;150;150m'
    SOFT_YELLOW=$'\033[38;2;255;220;150m'
    SOFT_CYAN=$'\033[38;2;150;220;230m'
    SOFT_MAGENTA=$'\033[38;2;220;180;255m'
    SOFT_ORANGE=$'\033[38;2;255;200;150m'
    SOFT_GRAY=$'\033[38;2;160;160;180m'
    BRIGHT_WHITE=$'\033[38;2;255;255;255m'
    DIM_WHITE=$'\033[38;2;200;200;210m'
    GRADIENT_START=$'\033[38;2;130;170;255m'
    GRADIENT_MID=$'\033[38;2;180;140;255m'
    GRADIENT_END=$'\033[38;2;220;180;255m'
    BOLD=$'\033[1m'
    DIM=$'\033[2m'
    NC=$'\033[0m'
else
    SOFT_BLUE=''; SOFT_GREEN=''; SOFT_RED=''; SOFT_YELLOW=''; SOFT_CYAN=''
    SOFT_MAGENTA=''; SOFT_ORANGE=''; SOFT_GRAY=''; BRIGHT_WHITE=''; DIM_WHITE=''
    GRADIENT_START=''; GRADIENT_MID=''; GRADIENT_END=''; BOLD=''; DIM=''; NC=''
fi

# Box Drawing Characters
BOX_TL="╔"; BOX_TR="╗"; BOX_BL="╚"; BOX_BR="╝"; BOX_H="═"; BOX_V="║"
ROUND_TL="╭"; ROUND_TR="╮"; ROUND_BL="╰"; ROUND_BR="╯"; ROUND_H="─"; ROUND_V="│"

# Symbols
SYMBOL_SUCCESS="◆"; SYMBOL_ERROR="◇"; SYMBOL_WARNING="⬡"; SYMBOL_INFO="●"

# Configuration
COMPOSE_FILE="docker-compose.yml"

# Single consolidated database configuration
DB_CONTAINER="postgres"
DB_PORT="15432"
DB_NAME="dokus"

# Get credentials from .env
if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -E 'DB_USERNAME|DB_PASSWORD|REDIS_PASSWORD|RABBITMQ_USERNAME|RABBITMQ_PASSWORD' | xargs)
    DB_USER="${DB_USERNAME:-dokus}"
    DB_PASSWORD="${DB_PASSWORD}"
else
    DB_USER="dokus"
    DB_PASSWORD=""
fi

# Helper to repeat a string
repeat_char() {
    local char=$1
    local count=$2
    local result=""
    for ((i=0; i<count; i++)); do
        result="${result}${char}"
    done
    echo "$result"
}

# Print functions
print_gradient_header() {
    local title=$1
    local width=70
    local padding=$(( (width - ${#title} - 4) / 2 ))
    local padding_right=$(( width - ${#title} - 4 - padding ))
    local line=$(repeat_char "═" $width)
    local spaces=$(printf '%*s' $width "")
    local pad_left=$(printf '%*s' $padding "")
    local pad_right=$(printf '%*s' $padding_right "")

    echo ""
    echo "${SOFT_CYAN}╔${line}╗${NC}"
    echo "${SOFT_CYAN}║${spaces}║${NC}"
    echo "${SOFT_CYAN}║  ${GRADIENT_START}${pad_left}${BRIGHT_WHITE}${BOLD}${title}${NC}${GRADIENT_END}${pad_right}  ${SOFT_CYAN}║${NC}"
    echo "${SOFT_CYAN}║${spaces}║${NC}"
    echo "${SOFT_CYAN}╚${line}╝${NC}"
    echo ""
}

print_separator() {
    echo "${SOFT_GRAY}  ▪ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ▪${NC}"
}

print_status() {
    local status=$1
    local message=$2
    case $status in
        success) echo "  ${SOFT_GREEN}${SYMBOL_SUCCESS}${NC}  ${message}" ;;
        error) echo "  ${SOFT_RED}${SYMBOL_ERROR}${NC}  ${message}" ;;
        warning) echo "  ${SOFT_YELLOW}${SYMBOL_WARNING}${NC}  ${message}" ;;
        info) echo "  ${SOFT_CYAN}${SYMBOL_INFO}${NC}  ${message}" ;;
    esac
}

# Detect OS
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        echo "macos"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        echo "linux"
    else
        echo "unknown"
    fi
}

# Check Docker
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_status error "Docker is not running. Please start Docker first."
        exit 1
    fi
}

# Check .env file
check_env() {
    if [ ! -f .env ]; then
        print_status warning ".env file not found!"
        echo ""
        print_status info "Please run the initial setup first (Option 1 in the menu)"
        return 1
    fi
    return 0
}

# Show service status
show_status() {
    print_gradient_header "📊 Service Status Dashboard"

    docker compose ps
    echo ""

    print_separator
    echo ""
    echo "  ${SOFT_CYAN}${BOLD}Health Status Monitor${NC}"
    echo ""

    # Health status table
    echo "  ${SOFT_GRAY}┌─────────────────────────┬──────────────────┐${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}                 ${SOFT_GRAY}│${NC} ${BOLD}Status${NC}           ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Check single PostgreSQL database
    printf "  ${SOFT_GRAY}│${NC} PostgreSQL (${DB_NAME})      ${SOFT_GRAY}│${NC} "
    if docker compose exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Redis
    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker compose exec -T redis redis-cli --no-auth-warning -a "${REDIS_PASSWORD}" ping &>/dev/null 2>&1; then
        echo "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # RabbitMQ
    printf "  ${SOFT_GRAY}│${NC} RabbitMQ Broker         ${SOFT_GRAY}│${NC} "
    if curl -f -s -u ${RABBITMQ_USERNAME:-dokus}:${RABBITMQ_PASSWORD:-localrabbitpass} http://localhost:25673/api/health/checks/alarms &>/dev/null; then
        echo "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # MinIO
    printf "  ${SOFT_GRAY}│${NC} MinIO Storage           ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:9000/minio/health/live &>/dev/null; then
        echo "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Ollama
    printf "  ${SOFT_GRAY}│${NC} Ollama AI               ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:11434/api/tags &>/dev/null; then
        echo "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Services
    local services=(
        "Auth Service:6091:/metrics"
        "Cashflow Service:6092:/health"
        "Payment Service:6093:/health"
        "Reporting Service:6094:/health"
        "Audit Service:6095:/health"
        "Banking Service:6096:/health"
    )

    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name port endpoint <<< "$service_info"
        printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} " "$service_name"
        if curl -f -s http://localhost:${port}${endpoint} > /dev/null 2>&1; then
            echo "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
        else
            echo "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
        fi
    done

    echo "  ${SOFT_GRAY}└─────────────────────────┴──────────────────┘${NC}"
    echo ""
}

# Show service info
print_services_info() {
    print_separator
    echo ""
    echo "  ${SOFT_CYAN}${BOLD}📍 Service Endpoints${NC}"
    echo ""

    echo "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Endpoints${NC}                               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Auth Service${NC}         ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6091${NC}               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/metrics /health${NC}                    ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Cashflow Service${NC}     ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6092${NC}               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Payment Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6093${NC}               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Reporting Service${NC}    ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6094${NC}               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Audit Service${NC}        ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6095${NC}               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Banking Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6096${NC}               ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    echo ""
    echo "  ${SOFT_CYAN}${BOLD}💾 Infrastructure Services${NC}"
    echo ""

    echo "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}PostgreSQL${NC}           ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:${DB_PORT}${NC} • ${SOFT_GRAY}${DB_NAME}${NC}            ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:16379${NC}                     ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}RabbitMQ${NC}             ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:25672${NC} • ${SOFT_GRAY}UI: 25673${NC}         ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_YELLOW}MinIO Storage${NC}        ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:9000${NC} • ${SOFT_GRAY}Console: 9001${NC}     ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Ollama AI${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:11434${NC}                     ${SOFT_GRAY}│${NC}"
    echo "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"
    echo ""
    echo "  ${DIM_WHITE}Database User: ${SOFT_CYAN}$DB_USER${NC}"
    echo ""
}

# Start services
start_services() {
    if ! check_env; then
        return 1
    fi

    print_gradient_header "🚀 Starting Dokus Cloud Services"

    print_status info "Pulling latest images..."
    docker compose pull -q

    print_status info "Starting services..."
    docker compose up -d

    if [ $? -eq 0 ]; then
        print_status success "All containers started successfully"
        echo ""
        print_status info "Waiting for services to be ready (this may take 2 minutes)..."
        sleep 10

        echo ""
        print_status success "Services are starting up!"
        echo ""
        print_services_info
    else
        print_status error "Failed to start services"
        exit 1
    fi
}

# Stop services
stop_services() {
    print_gradient_header "🛑 Stopping Services"
    docker compose down
    echo ""
    print_status success "All services stopped"
    echo ""
}

# Restart services
restart_services() {
    stop_services
    echo ""
    start_services
}

# Show logs
show_logs() {
    service=$1
    if [ -z "$service" ]; then
        docker compose logs -f
    else
        docker compose logs -f $service
    fi
}

# Access database
access_db() {
    print_gradient_header "🗄️  Database CLI Access"

    print_status info "Connecting to PostgreSQL database (${DB_NAME})..."
    echo ""
    docker compose exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME
}

# Initial setup function (original wizard)
initial_setup() {
    print_gradient_header "🔧 Initial Dokus Cloud Setup"

    local OS=$(detect_os)

    echo "${SOFT_GREEN}✓ Detected: $([ "$OS" = "macos" ] && echo "macOS" || echo "Linux")${NC}"

    # Check if Docker is installed
    echo ""
    echo "${SOFT_BLUE}[1/6] Checking Docker installation...${NC}"

    if ! command -v docker &> /dev/null; then
        echo "${SOFT_YELLOW}⚠ Docker is not installed${NC}"
        echo ""
        read -p "Would you like to install Docker now? (y/n): " -n 1 -r
        echo

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "${SOFT_BLUE}Installing Docker...${NC}"

            if [[ "$OS" == "macos" ]]; then
                if ! command -v brew &> /dev/null; then
                    echo "${SOFT_YELLOW}Installing Homebrew first...${NC}"
                    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                fi
                brew install --cask docker
                echo "${SOFT_GREEN}✓ Docker installed via Homebrew${NC}"
                echo "${SOFT_YELLOW}⚠ Please start Docker Desktop from Applications, then run this script again${NC}"
                exit 0
            elif [[ "$OS" == "linux" ]]; then
                curl -fsSL https://get.docker.com -o get-docker.sh
                sudo sh get-docker.sh
                sudo usermod -aG docker $USER
                rm get-docker.sh
                echo "${SOFT_GREEN}✓ Docker installed${NC}"
                echo "${SOFT_YELLOW}⚠ Please log out and log back in for group changes to take effect${NC}"
                echo "${SOFT_YELLOW}⚠ Then run this script again${NC}"
                exit 0
            fi
        else
            echo "${SOFT_RED}✗ Docker is required to run Dokus${NC}"
            echo "Please install Docker manually from: https://docker.com"
            exit 1
        fi
    else
        echo "${SOFT_GREEN}✓ Docker is installed${NC}"
    fi

    # Check if Docker is running
    if ! docker info &> /dev/null; then
        echo "${SOFT_RED}✗ Docker is not running${NC}"
        if [[ "$OS" == "macos" ]]; then
            echo "Please start Docker Desktop and run this script again"
        else
            echo "Please start Docker daemon: sudo systemctl start docker"
        fi
        exit 1
    fi
    echo "${SOFT_GREEN}✓ Docker is running${NC}"

    # Check if Docker Compose is installed
    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        echo "${SOFT_YELLOW}⚠ Docker Compose not found, installing...${NC}"
        if [[ "$OS" == "macos" ]]; then
            brew install docker-compose
        elif [[ "$OS" == "linux" ]]; then
            sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
            sudo chmod +x /usr/local/bin/docker-compose
        fi
        echo "${SOFT_GREEN}✓ Docker Compose installed${NC}"
    else
        echo "${SOFT_GREEN}✓ Docker Compose is installed${NC}"
    fi

    # Function to generate secure password
    generate_password() {
        if command -v openssl &> /dev/null; then
            openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
        else
            LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32
        fi
    }

    # Function to prompt for value with default
    prompt_with_default() {
        local prompt="$1"
        local default="$2"
        local var_name="$3"
        local is_password="$4"

        echo "" >&2
        echo "${SOFT_BLUE}${prompt}${NC}" >&2
        if [[ "$is_password" == "true" ]]; then
            echo "${SOFT_YELLOW}Default: <auto-generated secure password>${NC}" >&2
        else
            echo "${SOFT_YELLOW}Default: ${default}${NC}" >&2
        fi
        read -p "Value (press Enter for default): " value >&2

        if [ -z "$value" ]; then
            echo "$default"
        else
            echo "$value"
        fi
    }

    # Check for .env file
    echo ""
    echo "${SOFT_BLUE}[2/6] Configuring environment...${NC}"

    if [ -f .env ]; then
        echo "${SOFT_GREEN}✓ .env file exists${NC}"
        echo ""
        read -p "Would you like to reconfigure? (y/n): " -n 1 -r
        echo

        if [[ $REPLY =~ ^[Yy]$ ]]; then
            cp .env .env.backup
            echo "${SOFT_YELLOW}⚠ Backed up existing .env to .env.backup${NC}"
            rm .env
        else
            # Skip to next step
            echo ""
            echo "${SOFT_BLUE}[3/6] Configuring Docker registry...${NC}"
            configure_registry
            return
        fi
    fi

    echo "${SOFT_YELLOW}⚠ .env file not found - let's create one!${NC}"
    echo ""
    echo "${SOFT_BLUE}This wizard will configure your Dokus cloud deployment.${NC}"
    echo "${SOFT_BLUE}Only critical values need your input - everything else is auto-generated.${NC}"
    echo ""

    # Generate secure defaults
    DB_PASS=$(generate_password)
    REDIS_PASS=$(generate_password)
    RABBITMQ_PASS=$(generate_password)
    MINIO_PASS=$(generate_password)
    JWT_SECRET=$(openssl rand -base64 64 | tr -d "=+/" | cut -c1-64 2>/dev/null || LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 64)
    MONITORING_KEY=$(generate_password)
    ADMIN_KEY=$(generate_password)
    INTEGRATION_KEY=$(generate_password)
    REQUEST_SIGNING_SECRET=$(generate_password)

    # Prompt for configuration
    echo ""
    echo "${SOFT_GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "${SOFT_GREEN}  Critical Configuration${NC}"
    echo "${SOFT_GREEN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # Database
    DB_USERNAME=$(prompt_with_default "Database username:" "dokus" "DB_USERNAME")
    DB_PASSWORD=$(prompt_with_default "Database password:" "$DB_PASS" "DB_PASSWORD" "true")

    # Redis
    REDIS_HOST="redis"
    REDIS_PORT="6379"
    REDIS_PASSWORD=$(prompt_with_default "Redis password:" "$REDIS_PASS" "REDIS_PASSWORD" "true")

    # RabbitMQ
    RABBITMQ_USERNAME=$(prompt_with_default "RabbitMQ username:" "dokus" "RABBITMQ_USERNAME")
    RABBITMQ_PASSWORD=$(prompt_with_default "RabbitMQ password:" "$RABBITMQ_PASS" "RABBITMQ_PASSWORD" "true")

    # MinIO Object Storage
    MINIO_ROOT_USER=$(prompt_with_default "MinIO root user:" "dokusadmin" "MINIO_ROOT_USER")
    MINIO_ROOT_PASSWORD=$(prompt_with_default "MinIO root password:" "$MINIO_PASS" "MINIO_ROOT_PASSWORD" "true")
    MINIO_BUCKET="dokus-documents"

    # JWT
    JWT_SECRET_VAL=$(prompt_with_default "JWT secret (64+ chars):" "$JWT_SECRET" "JWT_SECRET" "true")
    JWT_ISSUER="https://dokus.tech"
    JWT_AUDIENCE="dokus-api"

    # Auto-generated values
    CACHE_TYPE="redis"
    MONITORING_API_KEY="$MONITORING_KEY"
    ADMIN_API_KEY="$ADMIN_KEY"
    INTEGRATION_API_KEY="$INTEGRATION_KEY"
    REQUEST_SIGNING_ENABLED="true"
    REQUEST_SIGNING_SECRET="$REQUEST_SIGNING_SECRET"
    RATE_LIMIT_PER_MINUTE="60"
    LOG_LEVEL="INFO"

    # Optional email configuration (disabled by default)
    EMAIL_ENABLED="false"
    EMAIL_PROVIDER="smtp"
    SMTP_HOST="smtp.example.com"
    SMTP_PORT="587"
    SMTP_USERNAME="noreply@dokus.tech"
    SMTP_PASSWORD=""
    SMTP_ENABLE_TLS="true"
    SMTP_ENABLE_AUTH="true"
    EMAIL_FROM_ADDRESS="noreply@dokus.tech"
    EMAIL_FROM_NAME="Dokus"
    EMAIL_REPLY_TO_ADDRESS="support@dokus.tech"
    EMAIL_REPLY_TO_NAME="Dokus Support"
    EMAIL_BASE_URL="https://dokus.tech"
    EMAIL_SUPPORT_ADDRESS="support@dokus.tech"

    # Optional monitoring (enabled with defaults)
    METRICS_ENABLED="true"
    METRICS_PORT="7090"
    TRACING_ENABLED="false"
    JAEGER_ENDPOINT=""

    # GeoIP (enabled by default)
    GEOIP_ENABLED="true"

    # Create .env file
    cat > .env << EOF
# Dokus Cloud Environment Configuration
# Generated on $(date)
#
# IMPORTANT: This file contains sensitive credentials - keep it secure!
# You can modify optional settings below after deployment.

# ============================================================================
# DATABASE CONFIGURATION
# ============================================================================
DB_USERNAME=$DB_USERNAME
DB_PASSWORD=$DB_PASSWORD

# ============================================================================
# REDIS CACHE CONFIGURATION
# ============================================================================
REDIS_HOST=$REDIS_HOST
REDIS_PORT=$REDIS_PORT
REDIS_PASSWORD=$REDIS_PASSWORD

# ============================================================================
# RABBITMQ MESSAGE BROKER
# ============================================================================
RABBITMQ_USERNAME=$RABBITMQ_USERNAME
RABBITMQ_PASSWORD=$RABBITMQ_PASSWORD

# ============================================================================
# MINIO OBJECT STORAGE
# ============================================================================
MINIO_ROOT_USER=$MINIO_ROOT_USER
MINIO_ROOT_PASSWORD=$MINIO_ROOT_PASSWORD
MINIO_BUCKET=$MINIO_BUCKET

# ============================================================================
# JWT AUTHENTICATION
# ============================================================================
JWT_SECRET=$JWT_SECRET_VAL
JWT_ISSUER=$JWT_ISSUER
JWT_AUDIENCE=$JWT_AUDIENCE

# ============================================================================
# CACHING (CRITICAL!)
# ============================================================================
CACHE_TYPE=$CACHE_TYPE

# ============================================================================
# SECURITY & API KEYS
# ============================================================================
# API Keys (auto-generated - rotate these regularly)
MONITORING_API_KEY=$MONITORING_API_KEY
ADMIN_API_KEY=$ADMIN_API_KEY
INTEGRATION_API_KEY=$INTEGRATION_API_KEY

# Request Signing
REQUEST_SIGNING_ENABLED=$REQUEST_SIGNING_ENABLED
REQUEST_SIGNING_SECRET=$REQUEST_SIGNING_SECRET

# Rate Limiting
RATE_LIMIT_PER_MINUTE=$RATE_LIMIT_PER_MINUTE

# ============================================================================
# EMAIL CONFIGURATION (Optional - currently disabled)
# ============================================================================
EMAIL_ENABLED=$EMAIL_ENABLED
EMAIL_PROVIDER=$EMAIL_PROVIDER
SMTP_HOST=$SMTP_HOST
SMTP_PORT=$SMTP_PORT
SMTP_USERNAME=$SMTP_USERNAME
SMTP_PASSWORD=$SMTP_PASSWORD
SMTP_ENABLE_TLS=$SMTP_ENABLE_TLS
SMTP_ENABLE_AUTH=$SMTP_ENABLE_AUTH
EMAIL_FROM_ADDRESS=$EMAIL_FROM_ADDRESS
EMAIL_FROM_NAME=$EMAIL_FROM_NAME
EMAIL_REPLY_TO_ADDRESS=$EMAIL_REPLY_TO_ADDRESS
EMAIL_REPLY_TO_NAME=$EMAIL_REPLY_TO_NAME
EMAIL_BASE_URL=$EMAIL_BASE_URL
EMAIL_SUPPORT_ADDRESS=$EMAIL_SUPPORT_ADDRESS

# ============================================================================
# MONITORING & OBSERVABILITY (Optional)
# ============================================================================
METRICS_ENABLED=$METRICS_ENABLED
METRICS_PORT=$METRICS_PORT
TRACING_ENABLED=$TRACING_ENABLED
JAEGER_ENDPOINT=$JAEGER_ENDPOINT

# ============================================================================
# GEOLOCATION
# ============================================================================
GEOIP_ENABLED=$GEOIP_ENABLED

# ============================================================================
# CORS CONFIGURATION
# ============================================================================
CORS_ALLOWED_HOSTS=*

# ============================================================================
# AI CONFIGURATION (Document Processing)
# ============================================================================
AI_DEFAULT_PROVIDER=ollama
AI_OLLAMA_ENABLED=true
AI_OLLAMA_MODEL=mistral:7b
AI_OPENAI_ENABLED=false
AI_OPENAI_API_KEY=
AI_OPENAI_MODEL=gpt-4o-mini

# Ollama Performance (adjust for your hardware)
# Raspberry Pi 4 (4GB): OLLAMA_NUM_PARALLEL=1, OLLAMA_MAX_LOADED_MODELS=1
# Raspberry Pi 5 (8GB): OLLAMA_NUM_PARALLEL=2, OLLAMA_MAX_LOADED_MODELS=1
# Server with GPU: OLLAMA_NUM_PARALLEL=4, OLLAMA_MAX_LOADED_MODELS=2
OLLAMA_NUM_PARALLEL=1
OLLAMA_MAX_LOADED_MODELS=1

# ============================================================================
# LOGGING
# ============================================================================
LOG_LEVEL=$LOG_LEVEL
EOF

    echo ""
    echo "${SOFT_GREEN}✓ Configuration saved to .env${NC}"
    echo ""
    echo "${SOFT_YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "${SOFT_YELLOW}  Optional Configuration${NC}"
    echo "${SOFT_YELLOW}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo "${SOFT_BLUE}The following optional features are available:${NC}"
    echo ""
    echo "  ${SOFT_GREEN}✓${NC} Metrics & monitoring (enabled on port $METRICS_PORT)"
    echo "  ${SOFT_YELLOW}⚠${NC} Email notifications (disabled - edit .env to enable)"
    echo "  ${SOFT_YELLOW}⚠${NC} Distributed tracing (disabled - edit .env to enable)"
    echo ""
    echo "${SOFT_BLUE}To modify any optional settings:${NC}"
    echo "  1. Edit the .env file: ${SOFT_YELLOW}nano .env${NC}"
    echo "  2. Update desired values"
    echo "  3. Restart services: ${SOFT_YELLOW}docker compose restart${NC}"
    echo ""
    echo "${SOFT_RED}⚠ SECURITY: Keep your .env file secure - it contains sensitive credentials!${NC}"

    # Configure Docker registry
    echo ""
    echo "${SOFT_BLUE}[3/6] Configuring Docker registry...${NC}"
    configure_registry

    # Pull images and start
    echo ""
    echo "${SOFT_BLUE}[4/6] Pulling latest Docker images...${NC}"
    echo "This may take a few minutes..."
    docker compose pull
    echo "${SOFT_GREEN}✓ Images pulled successfully${NC}"

    echo ""
    echo "${SOFT_BLUE}[5/6] Starting Dokus services...${NC}"
    docker compose up -d
    echo "${SOFT_GREEN}✓ Services started${NC}"

    echo ""
    echo "${SOFT_BLUE}[6/6] Waiting for services to be ready...${NC}"
    echo "This may take up to 2 minutes..."
    sleep 10

    MAX_RETRIES=30
    RETRY_COUNT=0
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        HEALTHY_COUNT=$(docker compose ps | grep -c "healthy" || true)
        if [ $HEALTHY_COUNT -ge 7 ]; then
            echo "${SOFT_GREEN}✓ All services are healthy${NC}"
            break
        fi
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo -n "."
        sleep 5
    done

    echo ""
    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        echo "${SOFT_YELLOW}⚠ Some services may still be starting${NC}"
        echo "Run './dokus.sh status' to check service status"
    fi

    # Configure auto-start
    echo ""
    echo "${SOFT_BLUE}Configure Auto-Start${NC}"
    read -p "Would you like Dokus to start automatically on system boot? (y/n): " -n 1 -r
    echo

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        configure_autostart "$OS"
    else
        echo "Skipping auto-start configuration"
    fi

    # Display final status
    echo ""
    echo "${SOFT_GREEN}════════════════════════════════════════════${NC}"
    echo "${SOFT_GREEN}✓ Dokus Server is running!${NC}"
    echo "${SOFT_GREEN}════════════════════════════════════════════${NC}"
    echo ""
    print_services_info
}

# Configure Docker registry
configure_registry() {
    local OS=$(detect_os)
    local DAEMON_JSON="/etc/docker/daemon.json"
    local REGISTRY="94.111.226.82:5000"

    if [[ "$OS" == "macos" ]]; then
        echo "${SOFT_YELLOW}⚠ On macOS, please configure insecure registry manually:${NC}"
        echo "  1. Open Docker Desktop"
        echo "  2. Go to Settings → Docker Engine"
        echo "  3. Add the following to the JSON config:"
        echo "     \"insecure-registries\": [\"$REGISTRY\"]"
        echo "  4. Click 'Apply & Restart'"
        echo ""
        read -p "Press Enter after you've configured the registry..."
    elif [[ "$OS" == "linux" ]]; then
        if [ -f "$DAEMON_JSON" ]; then
            if grep -q "$REGISTRY" "$DAEMON_JSON"; then
                echo "${SOFT_GREEN}✓ Insecure registry already configured${NC}"
                return
            fi
        fi

        echo "${SOFT_YELLOW}⚠ Configuring insecure registry (requires sudo)${NC}"
        if [ -f "$DAEMON_JSON" ]; then
            sudo cp "$DAEMON_JSON" "$DAEMON_JSON.backup"
        fi

        echo "{
  \"insecure-registries\": [\"$REGISTRY\"]
}" | sudo tee "$DAEMON_JSON" > /dev/null

        sudo systemctl restart docker
        sleep 3
        echo "${SOFT_GREEN}✓ Docker configured for insecure registry${NC}"
    fi
}

# Configure auto-start
configure_autostart() {
    local OS=$1

    if [[ "$OS" == "macos" ]]; then
        local PLIST_FILE="$HOME/Library/LaunchAgents/com.dokus.server.plist"
        local SCRIPT_PATH="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

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
        <string>cd $SCRIPT_PATH && docker compose up -d</string>
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
        echo "${SOFT_GREEN}✓ Auto-start configured${NC}"
        echo "To disable: launchctl unload $PLIST_FILE"

    elif [[ "$OS" == "linux" ]]; then
        local SERVICE_FILE="/etc/systemd/system/dokus.service"
        local WORKING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

        echo "${SOFT_YELLOW}Creating systemd service (requires sudo)${NC}"

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
        echo "${SOFT_GREEN}✓ Auto-start configured${NC}"
        echo "To disable: sudo systemctl disable dokus.service"
    fi
}

# Show interactive menu
show_menu() {
    clear
    local line=$(repeat_char "═" 68)
    echo ""
    echo "${SOFT_CYAN}╔${line}╗${NC}"
    echo "${SOFT_CYAN}║                                                                    ║${NC}"
    echo "${SOFT_CYAN}║              ${GRADIENT_START}${BOLD}☁️  Dokus Cloud Management${NC}${SOFT_CYAN}               ║${NC}"
    echo "${SOFT_CYAN}║                                                                    ║${NC}"
    echo "${SOFT_CYAN}╚${line}╝${NC}"
    echo ""
    echo "  ${SOFT_CYAN}${BOLD}What would you like to do?${NC}"
    echo ""

    echo "  ${SOFT_GREEN}${BOLD}Service Management${NC}"
    echo "    ${SOFT_CYAN}①${NC}  Initial Setup (first time only)"
    echo "    ${SOFT_CYAN}②${NC}  Start services"
    echo "    ${SOFT_CYAN}③${NC}  Stop services"
    echo "    ${SOFT_CYAN}④${NC}  Restart services"
    echo "    ${SOFT_CYAN}⑤${NC}  Show status"
    echo ""

    echo "  ${SOFT_MAGENTA}${BOLD}Development Tools${NC}"
    echo "    ${SOFT_CYAN}⑥${NC}  View logs"
    echo "    ${SOFT_CYAN}⑦${NC}  Access database"
    echo ""

    echo "  ${SOFT_GRAY}⓪${NC}  Exit"
    echo ""

    printf "  ${BOLD}Enter choice ${DIM_WHITE}[0-7]:${NC} "
    read choice

    echo ""

    case $choice in
        1) initial_setup ;;
        2) check_docker && start_services ;;
        3) stop_services ;;
        4) check_docker && restart_services ;;
        5) show_status ;;
        6) show_logs ;;
        7) access_db ;;
        0) echo "  ${SOFT_CYAN}👋 Goodbye!${NC}\n" && exit 0 ;;
        *) print_status error "Invalid choice" && sleep 2 && show_menu ;;
    esac

    echo ""
    printf "  ${DIM}Press Enter to continue...${NC}"
    read
    show_menu
}

# Main function
main() {
    case ${1:-} in
        setup)
            initial_setup
            ;;
        start)
            check_docker
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            check_docker
            restart_services
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs ${2:-}
            ;;
        db)
            access_db
            ;;
        *)
            # If no command, show interactive menu
            show_menu
            ;;
    esac
}

# Run main function
main "$@"
