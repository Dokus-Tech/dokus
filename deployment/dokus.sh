#!/bin/bash
#
# Dokus Cloud Management Script — Neon Onboarding Edition
# Supports: macOS, Linux
# Usage: ./dokus.sh [command]
#

set -e

# Change to script directory
cd "$(dirname "$0")"

# Detect interactive terminal (for subtle animations)
IS_TTY=false
if [ -t 1 ]; then
    IS_TTY=true
fi

# Cross-platform echo replacement
echo_e() { printf '%b\n' "$*"; }

# Color palette
if [[ -n "${NO_COLOR:-}" ]]; then
    USE_COLOR=false
else
    USE_COLOR=true
fi

if [[ "$USE_COLOR" == true ]]; then
    SOFT_BLUE=$'\033[38;2;0;228;255m'
    SOFT_GREEN=$'\033[38;2;141;255;185m'
    SOFT_RED=$'\033[38;2;255;140;140m'
    SOFT_YELLOW=$'\033[38;2;255;203;112m'
    SOFT_CYAN=$'\033[38;2;0;255;214m'
    SOFT_MAGENTA=$'\033[38;2;255;111;210m'
    SOFT_ORANGE=$'\033[38;2;255;180;140m'
    SOFT_GRAY=$'\033[38;2;150;165;190m'
    BRIGHT_WHITE=$'\033[38;2;244;246;255m'
    DIM_WHITE=$'\033[38;2;190;200;215m'
    GRADIENT_START=$'\033[38;2;0;240;255m'
    GRADIENT_MID=$'\033[38;2;191;120;255m'
    GRADIENT_END=$'\033[38;2;255;120;215m'
    BOLD=$'\033[1m'
    DIM=$'\033[2m'
    NC=$'\033[0m'
else
    SOFT_BLUE=''; SOFT_GREEN=''; SOFT_RED=''; SOFT_YELLOW=''; SOFT_CYAN='';
    SOFT_MAGENTA=''; SOFT_ORANGE=''; SOFT_GRAY=''; BRIGHT_WHITE=''; DIM_WHITE='';
    GRADIENT_START=''; GRADIENT_MID=''; GRADIENT_END=''; BOLD=''; DIM=''; NC='';
fi

# Symbols
SYMBOL_OK="◎"
SYMBOL_ERR="⨯"
SYMBOL_WARN="◒"
SYMBOL_INFO="◦"
SYMBOL_TASK="▹"

# Configuration
COMPOSE_FILE="docker-compose.yml"
DB_CONTAINER="postgres"
DB_PORT="15432"
DB_NAME="dokus"

# Credentials
if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -E 'DB_USERNAME|DB_PASSWORD|REDIS_PASSWORD|RABBITMQ_USERNAME|RABBITMQ_PASSWORD' | xargs)
    DB_USER="${DB_USERNAME:-dokus}"
    DB_PASSWORD="${DB_PASSWORD}"
else
    DB_USER="dokus"
    DB_PASSWORD=""
fi

# Helpers
repeat_char() {
    local char=$1
    local count=$2
    local result=""
    for ((i=0; i<count; i++)); do
        result+="$char"
    done
    echo "$result"
}

beam_line() {
    local width=${1:-66}
    local colors=("$GRADIENT_START" "$GRADIENT_MID" "$GRADIENT_END")
    for ((i=0; i<width; i++)); do
        printf "%b━" "${colors[i % ${#colors[@]}]}"
    done
    printf "%b\n" "$NC"
}

header_pulse() {
    if [[ "$IS_TTY" != true ]]; then
        return
    fi
    local frames=("▛▜" "▙▟" "▙▟")
    for frame in "${frames[@]}"; do
        printf "  ${SOFT_MAGENTA}%s${NC}\r" "$frame"
        sleep 0.07
    done
    printf "  \r"
}

print_gradient_header() {
    local title=$1
    local subtitle=${2:-}
    local width=70

    echo ""
    header_pulse
    echo -n "  "
    beam_line $width
    printf "  ${SOFT_MAGENTA}▌${NC} ${BOLD}${BRIGHT_WHITE}%s${NC}\n" "$title"
    if [ -n "$subtitle" ]; then
        printf "  ${SOFT_MAGENTA}▌${NC} ${DIM_WHITE}%s${NC}\n" "$subtitle"
    fi
    echo -n "  "
    beam_line $width
    echo ""
}

print_separator() {
    local width=${1:-46}
    printf "  "
    for ((i=0; i<width; i++)); do
        if (( i % 2 == 0 )); then
            printf "%b╼" "$SOFT_GRAY"
        else
            printf "%b╾" "$DIM_WHITE"
        fi
    done
    printf "%b\n" "$NC"
}

print_status() {
    local status=$1
    local message=$2
    case $status in
        success)
            printf "  %b${SYMBOL_OK}%b  %s\n" "$SOFT_GREEN" "$NC" "$message"
            ;;
        error)
            printf "  %b${SYMBOL_ERR}%b  %s\n" "$SOFT_RED" "$NC" "$message"
            ;;
        warning)
            printf "  %b${SYMBOL_WARN}%b  %s\n" "$SOFT_YELLOW" "$NC" "$message"
            ;;
        info)
            printf "  %b${SYMBOL_INFO}%b  %s\n" "$SOFT_CYAN" "$NC" "$message"
            ;;
        task)
            printf "  %b${SYMBOL_TASK}%b  %s\n" "$SOFT_MAGENTA" "$NC" "$message"
            ;;
    esac
}

splash_screen() {
    clear
    print_gradient_header "Dokus Cloud" "Install. Launch. Observe."
    local art=(
"      ╔╦╗╔═╗╦╦ ╦╔═╗  ╔═╗╦  ╔═╗╔╗ ╦ ╦"
"      ║║║║╣ ║║ ║╚═╗  ║  ║  ║ ║╠╩╗╚╦╝"
"      ╩ ╩╚═╝╩╚═╝╚═╝  ╚═╝╩═╝╚═╝╚═╝ ╩ "
    )
    for line in "${art[@]}"; do
        printf "  ${SOFT_MAGENTA}%s${NC}\n" "$line"
        if [[ "$IS_TTY" == true ]]; then sleep 0.04; fi
    done
    print_separator 52
    echo_e "  ${DIM_WHITE}Synth-styled orchestrator for PostgreSQL, Redis, RabbitMQ, MinIO, Ollama, and services.${NC}\n"
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

check_docker() {
    if ! command -v docker &> /dev/null; then
        print_status error "Docker is not installed."
        return 1
    fi
    if ! docker info > /dev/null 2>&1; then
        print_status error "Docker is not running. Start Docker then retry."
        return 1
    fi
    print_status success "Docker engine is live"
}

check_env() {
    if [ ! -f .env ]; then
        print_status warning ".env missing — run the guided setup (option 1)."
        return 1
    fi
    return 0
}

show_status() {
    print_gradient_header "📊 Service Status" "Docker compose + health probes"

    docker compose ps
    echo ""

    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}Health Monitor${NC}\n"

    echo_e "  ${SOFT_GRAY}┌─────────────────────────┬──────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}                 ${SOFT_GRAY}│${NC} ${BOLD}Status${NC}           ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    printf "  ${SOFT_GRAY}│${NC} PostgreSQL (${DB_NAME})     ${SOFT_GRAY}│${NC} "
    if docker compose exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker compose exec -T redis redis-cli --no-auth-warning -a "${REDIS_PASSWORD}" ping &>/dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} RabbitMQ Broker         ${SOFT_GRAY}│${NC} "
    if curl -f -s -u ${RABBITMQ_USERNAME:-dokus}:${RABBITMQ_PASSWORD:-localrabbitpass} http://localhost:25673/api/health/checks/alarms &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} MinIO Storage           ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:9000/minio/health/live &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} Ollama AI               ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:11434/api/tags &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

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
            echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
        else
            echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
        fi
    done

    echo_e "  ${SOFT_GRAY}└─────────────────────────┴──────────────────┘${NC}"
    echo ""
}

print_services_info() {
    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}📍 Service Endpoints${NC}\n"

    echo_e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Endpoints${NC}                               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Auth Service${NC}         ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6091${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/metrics /health${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Cashflow Service${NC}     ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6092${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Payment Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6093${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Reporting Service${NC}    ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6094${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Audit Service${NC}        ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6095${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Banking Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:6096${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}💾 Infrastructure Services${NC}\n"

    echo_e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}PostgreSQL${NC}           ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:${DB_PORT}${NC} • ${SOFT_GRAY}${DB_NAME}${NC}            ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:16379${NC}                     ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}RabbitMQ${NC}             ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:25672${NC} • ${SOFT_GRAY}UI: 25673${NC}         ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_YELLOW}MinIO Storage${NC}        ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:9000${NC} • ${SOFT_GRAY}Console: 9001${NC}     ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Ollama AI${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:11434${NC}                     ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"
    echo ""
    echo "  ${DIM_WHITE}Database User: ${SOFT_CYAN}$DB_USER${NC}"
    echo ""
}

start_services() {
    if ! check_env; then
        return 1
    fi

    print_gradient_header "🚀 Launching Dokus" "Compose stack + health probes"

    print_status info "Pulling latest images..."
    docker compose pull -q

    print_status info "Starting services..."
    docker compose up -d

    if [ $? -eq 0 ]; then
        print_status success "Containers ignited"
        echo ""
        print_status info "Stabilizing services (give us a moment)..."
        sleep 10
        echo ""
        print_status success "Services are starting up"
        echo ""
        print_services_info
    else
        print_status error "Failed to start services"
        exit 1
    fi
}

stop_services() {
    print_gradient_header "🛑 Stopping Services"
    docker compose down
    echo ""
    print_status success "All services stopped"
    echo ""
}

restart_services() {
    stop_services
    echo ""
    start_services
}

show_logs() {
    service=$1
    if [ -z "$service" ]; then
        docker compose logs -f
    else
        docker compose logs -f $service
    fi
}

access_db() {
    print_gradient_header "🗄️  Database CLI"
    print_status info "Connecting to PostgreSQL (${DB_NAME})..."
    echo ""
    docker compose exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME
}

initial_setup() {
    splash_screen
    print_gradient_header "🔧 Guided Setup" "First-time install wizard"

    local OS=$(detect_os)
    print_status info "Detected OS: $([ "$OS" = "macos" ] && echo "macOS" || echo "Linux")"

    print_status task "Checking Docker installation"
    if ! command -v docker &> /dev/null; then
        print_status warning "Docker not found"
        read -p "  Install Docker now? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            if [[ "$OS" == "macos" ]]; then
                if ! command -v brew &> /dev/null; then
                    print_status info "Installing Homebrew first..."
                    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
                fi
                brew install --cask docker
                print_status success "Docker installed via Homebrew"
                print_status warning "Start Docker Desktop, then rerun this script"
                exit 0
            elif [[ "$OS" == "linux" ]]; then
                curl -fsSL https://get.docker.com -o get-docker.sh
                sudo sh get-docker.sh
                sudo usermod -aG docker $USER
                rm get-docker.sh
                print_status success "Docker installed"
                print_status warning "Log out/in to refresh group membership"
                exit 0
            fi
        else
            print_status error "Docker is required to run Dokus"
            exit 1
        fi
    fi

    if ! docker info &> /dev/null; then
        print_status error "Docker is not running"
        if [[ "$OS" == "macos" ]]; then
            echo "  Start Docker Desktop and rerun the script"
        else
            echo "  Start docker: sudo systemctl start docker"
        fi
        exit 1
    fi
    print_status success "Docker engine ready"

    if ! command -v docker-compose &> /dev/null && ! docker compose version &> /dev/null; then
        print_status warning "Docker Compose not found, installing..."
        if [[ "$OS" == "macos" ]]; then
            brew install docker-compose
        elif [[ "$OS" == "linux" ]]; then
            sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
            sudo chmod +x /usr/local/bin/docker-compose
        fi
        print_status success "Docker Compose installed"
    else
        print_status success "Docker Compose available"
    fi

    # Generate secure password
    generate_password() {
        if command -v openssl &> /dev/null; then
            openssl rand -base64 32 | tr -d "=+/" | cut -c1-32
        else
            LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 32
        fi
    }

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

    print_status task "Configuring environment (.env)"
    if [ -f .env ]; then
        print_status info ".env already exists"
        read -p "  Reconfigure? (y/n): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            cp .env .env.backup
            print_status warning "Backed up to .env.backup"
            rm .env
        else
            print_status info "Keeping existing .env"
            echo ""
            print_status task "Configuring Docker registry"
            configure_registry
            return
        fi
    fi

    echo ""
    echo "  ${SOFT_BLUE}This wizard will mint fresh credentials and defaults.${NC}"
    echo ""

    DB_PASS=$(generate_password)
    REDIS_PASS=$(generate_password)
    RABBITMQ_PASS=$(generate_password)
    MINIO_PASS=$(generate_password)
    JWT_SECRET=$(openssl rand -base64 64 | tr -d "=+/" | cut -c1-64 2>/dev/null || LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 64)
    MONITORING_KEY=$(generate_password)
    ADMIN_KEY=$(generate_password)
    INTEGRATION_KEY=$(generate_password)
    REQUEST_SIGNING_SECRET=$(generate_password)

    DB_USERNAME=$(prompt_with_default "Database username:" "dokus" "DB_USERNAME")
    DB_PASSWORD=$(prompt_with_default "Database password:" "$DB_PASS" "DB_PASSWORD" "true")

    REDIS_HOST="redis"
    REDIS_PORT="6379"
    REDIS_PASSWORD=$(prompt_with_default "Redis password:" "$REDIS_PASS" "REDIS_PASSWORD" "true")

    RABBITMQ_USERNAME=$(prompt_with_default "RabbitMQ username:" "dokus" "RABBITMQ_USERNAME")
    RABBITMQ_PASSWORD=$(prompt_with_default "RabbitMQ password:" "$RABBITMQ_PASS" "RABBITMQ_PASSWORD" "true")

    MINIO_ROOT_USER=$(prompt_with_default "MinIO root user:" "dokusadmin" "MINIO_ROOT_USER")
    MINIO_ROOT_PASSWORD=$(prompt_with_default "MinIO root password:" "$MINIO_PASS" "MINIO_ROOT_PASSWORD" "true")
    MINIO_BUCKET="dokus-documents"

    JWT_SECRET_VAL=$(prompt_with_default "JWT secret (64+ chars):" "$JWT_SECRET" "JWT_SECRET" "true")
    JWT_ISSUER="https://dokus.tech"
    JWT_AUDIENCE="dokus-api"

    CACHE_TYPE="redis"
    MONITORING_API_KEY="$MONITORING_KEY"
    ADMIN_API_KEY="$ADMIN_KEY"
    INTEGRATION_API_KEY="$INTEGRATION_KEY"
    REQUEST_SIGNING_ENABLED="true"
    REQUEST_SIGNING_SECRET="$REQUEST_SIGNING_SECRET"
    RATE_LIMIT_PER_MINUTE="60"
    LOG_LEVEL="INFO"

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

    METRICS_ENABLED="true"
    METRICS_PORT="7090"
    TRACING_ENABLED="false"
    JAEGER_ENDPOINT=""

    GEOIP_ENABLED="true"

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
    print_status success ".env minted with fresh credentials"
    echo ""
    print_status info "Optional features: metrics enabled by default; email & tracing disabled."

    echo ""
    print_status task "Configuring Docker registry"
    configure_registry

    echo ""
    print_status task "Pulling latest Docker images"
    docker compose pull
    print_status success "Images pulled"

    echo ""
    print_status task "Starting Dokus services"
    docker compose up -d
    print_status success "Services started"

    echo ""
    print_status task "Waiting for services to report healthy"
    sleep 10

    local MAX_RETRIES=30
    local RETRY_COUNT=0
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        HEALTHY_COUNT=$(docker compose ps | grep -c "healthy" || true)
        if [ $HEALTHY_COUNT -ge 7 ]; then
            print_status success "All services are healthy"
            break
        fi
        RETRY_COUNT=$((RETRY_COUNT + 1))
        printf "."
        sleep 5
    done
    echo ""

    if [ $RETRY_COUNT -ge $MAX_RETRIES ]; then
        print_status warning "Some services may still be starting"
        echo "  Run './dokus.sh status' to inspect health"
    fi

    echo ""
    print_status task "Configure auto-start"
    read -p "  Auto-start Dokus on boot? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        configure_autostart "$OS"
    else
        print_status info "Skipping auto-start configuration"
    fi

    echo ""
    print_status success "Dokus server is online"
    print_services_info
}

configure_registry() {
    local OS=$(detect_os)
    local DAEMON_JSON="/etc/docker/daemon.json"
    local REGISTRY="94.111.226.82:5000"

    if [[ "$OS" == "macos" ]]; then
        print_status warning "On macOS, configure insecure registry via Docker Desktop:"
        echo "  Settings → Docker Engine → add: \"insecure-registries\": [\"$REGISTRY\"]"
        read -p "  Press Enter after configuring the registry..."
    elif [[ "$OS" == "linux" ]]; then
        if [ -f "$DAEMON_JSON" ] && grep -q "$REGISTRY" "$DAEMON_JSON"; then
            print_status success "Insecure registry already configured"
            return
        fi

        print_status warning "Configuring insecure registry (sudo required)"
        if [ -f "$DAEMON_JSON" ]; then
            sudo cp "$DAEMON_JSON" "$DAEMON_JSON.backup"
        fi

        echo "{
  \"insecure-registries\": [\"$REGISTRY\"]
}" | sudo tee "$DAEMON_JSON" > /dev/null

        sudo systemctl restart docker
        sleep 3
        print_status success "Docker configured for insecure registry"
    fi
}

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
        print_status success "Auto-start configured"
        echo "  To disable: launchctl unload $PLIST_FILE"

    elif [[ "$OS" == "linux" ]]; then
        local SERVICE_FILE="/etc/systemd/system/dokus.service"
        local WORKING_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

        print_status warning "Creating systemd service (sudo required)"

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
        print_status success "Auto-start configured"
        echo "  To disable: sudo systemctl disable dokus.service"
    fi
}

show_menu() {
    splash_screen
    echo_e "  ${SOFT_GRAY}${DIM}Choose a lane:${NC}\n"

    echo_e "  ${SOFT_GREEN}${BOLD}Launchpad${NC}"
    echo_e "    ${SOFT_CYAN}1${NC}   Guided initial setup"
    echo_e "    ${SOFT_CYAN}2${NC}   Start services"
    echo_e "    ${SOFT_CYAN}3${NC}   Stop services"
    echo_e "    ${SOFT_CYAN}4${NC}   Restart services"
    echo ""

    echo_e "  ${SOFT_MAGENTA}${BOLD}Diagnostics${NC}"
    echo_e "    ${SOFT_CYAN}5${NC}   Status dashboard"
    echo_e "    ${SOFT_CYAN}6${NC}   View logs"
    echo_e "    ${SOFT_CYAN}7${NC}   Database console"
    echo ""

    echo_e "  ${SOFT_GRAY}0${NC}    Exit"
    echo ""

    printf "  ${BOLD}Select channel ${DIM_WHITE}[0-7]:${NC} "
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
        *) print_status error "Invalid choice" && sleep 1 && show_menu ;;
    esac

    echo ""
    printf "  ${DIM}Press Enter to continue...${NC}"
    read
    show_menu
}

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
            show_menu
            ;;
    esac
}

main "$@"
