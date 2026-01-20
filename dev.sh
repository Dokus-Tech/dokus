#!/bin/bash

# Dokus Development Environment Manager
# This script manages the local development environment for Dokus Services

set -e  # Exit on error

# Detect interactive terminal (for small animations)
IS_TTY=false
if [ -t 1 ]; then
    IS_TTY=true
fi

# Cross-platform echo replacement (colors are pre-interpreted with $'...' syntax)
# This function handles printf %b format strings for backward compatibility
echo_e() {
    # Use printf to handle any remaining \n or other escape sequences in strings
    printf '%b\n' "$*"
}

# Check for NO_COLOR environment variable
if [[ -n "${NO_COLOR}" ]]; then
    USE_COLOR=false
else
    USE_COLOR=true
fi

# Modern Pastel Color Palette (24-bit RGB)
# Using $'...' syntax for proper escape sequence interpretation on macOS
if [[ "$USE_COLOR" == true ]]; then
    # Neon / synthwave palette
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
    SOFT_BLUE=''
    SOFT_GREEN=''
    SOFT_RED=''
    SOFT_YELLOW=''
    SOFT_CYAN=''
    SOFT_MAGENTA=''
    SOFT_ORANGE=''
    SOFT_GRAY=''
    BRIGHT_WHITE=''
    DIM_WHITE=''
    GRADIENT_START=''
    GRADIENT_MID=''
    GRADIENT_END=''
    BOLD=''
    DIM=''
    NC=''
fi

# Enhanced Box Drawing Characters
BOX_TL="╔"
BOX_TR="╗"
BOX_BL="╚"
BOX_BR="╝"
BOX_H="═"
BOX_V="║"
BOX_VL="╣"
BOX_VR="╠"
BOX_HT="╦"
BOX_HB="╩"
BOX_CROSS="╬"

# Rounded corners for modern feel
ROUND_TL="╭"
ROUND_TR="╮"
ROUND_BL="╰"
ROUND_BR="╯"
ROUND_H="─"
ROUND_V="│"

# Tree characters
TREE_BRANCH="├"
TREE_LAST="└"
TREE_VERT="│"
TREE_RIGHT="─"

# Modern Unicode Symbols
SYMBOL_SUCCESS="◎"
SYMBOL_ERROR="⨯"
SYMBOL_WARNING="◒"
SYMBOL_INFO="◦"
SYMBOL_LOADING="⟳"
SYMBOL_BULLET="▹"
SYMBOL_SMALL="⋄"

# Configuration
PROJECT_NAME="dokus"
COMPOSE_FILE="deployment/docker-compose.pro.yml -f deployment/docker-compose.local.yml"
SERVER_SERVICE_DIR="backendApp"

# Database configuration - consolidated single database
DB_CONTAINER="dokus-postgres"
DB_SERVICE="postgres"  # Service name for docker-compose exec
DB_PORT="15432"
DB_NAME="dokus"
DB_USER="dev"
DB_PASSWORD="devpassword"

# Export dev credentials for docker-compose variable substitution
export DB_PASSWORD="devpassword"
export REDIS_PASSWORD="devredispass"
export MINIO_PASSWORD="dokusadminpassword"

# Ensure empty .env exists (compose validates before applying overrides)
touch deployment/.env 2>/dev/null || true

# AI configuration
# Ollama (custom API)
OLLAMA_PORT="11434"
# LM Studio (OpenAI-compatible API)
LM_STUDIO_PORT="1234"
# Models used by IntelligenceMode (Assisted mode as default for dev)
OLLAMA_DEFAULT_MODELS=("qwen3-vl:2b" "qwen3-vl:8b" "qwen3:8b" "nomic-embed-text")

# Gateway configuration
GATEWAY_PORT="8000"
GATEWAY_DASHBOARD_PORT="8080"

# Function to get local IP address (defined early for STORAGE_PUBLIC_URL)
_get_local_ip() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        ipconfig getifaddr en0 2>/dev/null || \
        ipconfig getifaddr en1 2>/dev/null || \
        ipconfig getifaddr en2 2>/dev/null || \
        echo "localhost"
    else
        hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost"
    fi
}

# Storage public URL (for presigned URLs accessible from home network)
LOCAL_IP=$(_get_local_ip)
export STORAGE_PUBLIC_URL="http://${LOCAL_IP}:${GATEWAY_PORT}/storage"

# Function to capitalize first letter (Bash 3.2 compatible)
capitalize() {
    local str=$1
    echo "$(echo ${str:0:1} | tr '[:lower:]' '[:upper:]')${str:1}"
}

# Helper to repeat a Unicode character (tr doesn't work with multi-byte chars)
repeat_char() {
    local char=$1
    local count=$2
    local result=""
    for ((i=0; i<count; i++)); do
        result="${result}${char}"
    done
    echo "$result"
}

# Neon gradient line builder
beam_line() {
    local width=${1:-68}
    local colors=("$GRADIENT_START" "$GRADIENT_MID" "$GRADIENT_END")
    for ((i=0; i<width; i++)); do
        printf "%b━" "${colors[i % ${#colors[@]}]}"
    done
    printf "%b\n" "$NC"
}

# Minimal pulse animation for headers (TTY only)
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

# Function to print colored output
print_color() {
    local color=$1
    local message=$2
    echo "${color}${message}${NC}"
}

# Function to print a gradient header
print_gradient_header() {
    local title=$1
    local subtitle=${2:-}
    local width=72

    echo ""
    header_pulse
    printf "  "
    beam_line $width
    printf "  ${SOFT_MAGENTA}▌${NC} ${BOLD}${BRIGHT_WHITE}%s${NC}\n" "$title"
    if [ -n "$subtitle" ]; then
        printf "  ${SOFT_MAGENTA}▌${NC} ${DIM_WHITE}%s${NC}\n" "$subtitle"
    fi
    printf "  "
    beam_line $width
    echo ""
}

# Function to print a simple rounded box header
print_rounded_header() {
    print_gradient_header "$@"
}

# Function to print a section divider
print_divider() {
    local width=${2:-72}
    printf "  "
    beam_line "$width"
}

# Function to print a decorative separator
print_separator() {
    local width=${1:-44}
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

# Function to print a modern spinner
spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='◐◓◑◒'
    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf " ${SOFT_YELLOW}%c${NC}  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

# Function to print a progress bar
print_progress_bar() {
    local current=$1
    local total=$2
    local width=40
    local percentage=$((current * 100 / total))
    local filled=$((current * width / total))
    local empty=$((width - filled))

    printf "\r  ${SOFT_CYAN}["
    printf "%${filled}s" | tr ' ' '█'
    printf "%${empty}s" | tr ' ' '░'
    printf "]${NC} ${BRIGHT_WHITE}%3d%%${NC}" "$percentage"
}

# Function to print status with modern badges
print_status() {
    local status=$1
    local message=$2
    case $status in
        success)
            printf "  %b◎%b  %s  %b[%s]%b\n" "${SOFT_GREEN}" "${NC}" "${message}" "${SOFT_GREEN}" "READY" "${NC}"
            ;;
        error)
            printf "  %b⨯%b  %s  %b[%s]%b\n" "${SOFT_RED}" "${NC}" "${message}" "${SOFT_RED}" "FAULT" "${NC}"
            ;;
        warning)
            printf "  %b◒%b  %s  %b[%s]%b\n" "${SOFT_YELLOW}" "${NC}" "${message}" "${SOFT_YELLOW}" "NOTICE" "${NC}"
            ;;
        info)
            printf "  %b◦%b  %s\n" "${SOFT_CYAN}" "${NC}" "${message}"
            ;;
        loading)
            printf "  %b⟳%b  %s  %b[%s]%b\n" "${SOFT_ORANGE}" "${NC}" "${message}" "${SOFT_ORANGE}" "BOOT" "${NC}"
            ;;
        building)
            printf "  %b▹%b  %b%s%b\n" "${SOFT_MAGENTA}" "${NC}" "${DIM_WHITE}" "${message}" "${NC}"
            ;;
        *)
            printf "  %s\n" "${message}"
            ;;
    esac
}

# Function to print a simple status
print_simple_status() {
    local status=$1
    local message=$2
    case $status in
        success)
            printf "  %b◎%b  %s\n" "${SOFT_GREEN}" "${NC}" "${message}"
            ;;
        error)
            printf "  %b⨯%b  %s\n" "${SOFT_RED}" "${NC}" "${message}"
            ;;
        warning)
            printf "  %b◒%b  %s\n" "${SOFT_YELLOW}" "${NC}" "${message}"
            ;;
        info)
            printf "  %b◦%b  %s\n" "${SOFT_CYAN}" "${NC}" "${message}"
            ;;
        building)
            printf "  %b▹%b  %b%s%b\n" "${SOFT_MAGENTA}" "${NC}" "${DIM_WHITE}" "${message}" "${NC}"
            ;;
    esac
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_status error "Docker is not running. Please start Docker Desktop first."
        exit 1
    fi
    print_status success "Docker daemon is running"
}

# Function to check if required tools are installed
check_requirements() {
    print_gradient_header "🔍 System Requirements Check"

    check_docker

    if ! command -v docker-compose &> /dev/null; then
        print_status error "docker-compose is not installed"
        exit 1
    fi
    print_status success "docker-compose is available"

    if ! command -v gradle &> /dev/null && ! [ -f "./gradlew" ]; then
        print_status warning "Gradle not found, using gradlew wrapper"
    else
        print_status success "Gradle build tool detected"
    fi

    echo ""
    echo_e "  ${SOFT_GREEN}◎${NC}  ${BOLD}All system requirements met${NC}"
    echo ""
}

# Function to build the application
build_app() {
    print_gradient_header "🔨 Building Dokus Server"

    echo_e "  ${SOFT_CYAN}${BOLD}Phase 1: Building backend fat JAR${NC}\n"

    print_simple_status building "Building dokus-server JAR..."
    if [ -f "./gradlew" ]; then
        ./gradlew :backendApp:shadowJar -x test -q > /dev/null 2>&1
    else
        gradle :backendApp:shadowJar -x test -q > /dev/null 2>&1
    fi

    if [ $? -ne 0 ]; then
        print_status error "dokus-server JAR build failed"
        exit 1
    fi
    print_simple_status success "dokus-server JAR compiled"

    echo ""
    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}Phase 2: Building Docker image${NC}\n"

    print_simple_status building "Building dokus-server image..."
    docker-compose -f $COMPOSE_FILE build dokus-server > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        print_status error "dokus-server Docker image build failed"
        exit 1
    fi
    print_simple_status success "dokus-server image ready"

    echo ""
    echo_e "  ${SOFT_GREEN}${BOLD}✓${NC}  ${SOFT_GREEN}Server build complete${NC}"
    echo ""
}

# Function to start services
start_services() {
    print_gradient_header "🚀 Starting Development Environment"

    # Create logs directory if it doesn't exist
    mkdir -p logs

    # Start services
    print_status loading "Initializing Docker containers..."
    local compose_output
    compose_output=$(docker-compose -f $COMPOSE_FILE up -d 2>&1)
    local compose_exit_code=$?

    if [ $compose_exit_code -eq 0 ]; then
        print_status success "All containers started successfully"

        # Wait for services to be healthy
        echo ""
        echo_e "  ${SOFT_CYAN}${BOLD}Waiting for services to initialize...${NC}\n"

        # Wait for PostgreSQL database
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "PostgreSQL ($DB_NAME)"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T $DB_SERVICE pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo_e "${SOFT_RED}⨯ Timeout${NC}"
            fi
            printf "."
            sleep 1
        done

        # Wait for Redis
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Redis Cache"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T redis redis-cli --pass devredispass ping &>/dev/null; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            printf "."
            sleep 1
        done

        # Wait for MinIO
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "MinIO Storage"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T minio curl -fs http://localhost:9000/minio/health/live &>/dev/null 2>&1; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo_e "${SOFT_YELLOW}◒ Slow Start${NC}"
            fi
            printf "."
            sleep 1
        done

        # Check LM Studio (AI) - runs on the host (optional)
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "LM Studio AI"
        if curl -f -s http://localhost:${LM_STUDIO_PORT}/v1/models &>/dev/null; then
            echo_e "${SOFT_GREEN}◎ Ready${NC}"
        elif curl -f -s http://localhost:${OLLAMA_PORT}/api/tags &>/dev/null; then
            echo_e "${SOFT_GREEN}◎ Ollama${NC}"
        else
            echo_e "${SOFT_YELLOW}◒ Optional${NC}"
        fi

        # Wait for Traefik Gateway
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Traefik Gateway"
        for i in {1..30}; do
            if curl -f -s http://localhost:${GATEWAY_PORT}/health &>/dev/null 2>&1 || curl -f -s http://localhost:${GATEWAY_DASHBOARD_PORT}/api/overview &>/dev/null 2>&1; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo_e "${SOFT_YELLOW}◒ Slow Start${NC}"
            fi
            printf "."
            sleep 1
        done

        # Wait for services with proper spacing
        sleep 3

        # Check server via gateway
        local services=(
            "Dokus Server:/api/v1/server/info"
        )

        for service_info in "${services[@]}"; do
            IFS=':' read -r service_name endpoint <<< "$service_info"
            printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "${service_name}"
            for i in {1..30}; do
                # Accept any HTTP response (401/404 means service is reachable through gateway)
                http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${GATEWAY_PORT}${endpoint}" 2>/dev/null)
                if [ "$http_code" != "000" ] && [ "$http_code" != "" ]; then
                    echo_e "${SOFT_GREEN}◎ Ready${NC}"
                    break
                fi
                if [ $i -eq 30 ]; then
                    echo_e "${SOFT_YELLOW}◒ Slow Start${NC}"
                fi
                printf "."
                sleep 1
            done
        done

        echo ""
        echo_e "  ${SOFT_GREEN}${BOLD}✓${NC}  ${SOFT_GREEN}All services are operational!${NC}"
        echo ""
        print_services_info
    else
        print_status error "Failed to start services"
        echo ""
        echo "${SOFT_RED}Docker compose output:${NC}"
        echo "$compose_output"
        echo ""
        exit 1
    fi
}

# Function to stop services
stop_services() {
    print_gradient_header "🛑 Stopping Services"
    docker-compose -f $COMPOSE_FILE down
    echo ""
    print_status success "All services stopped"
    echo ""
}

# Function to restart services
restart_services() {
    stop_services
    echo ""
    start_services
}

# Function to show logs
show_logs() {
    service=$1
    if [ -z "$service" ]; then
        docker-compose -f $COMPOSE_FILE logs -f
    else
        docker-compose -f $COMPOSE_FILE logs -f $service
    fi
}

# Function to show service status
show_status() {
    print_gradient_header "📊 Service Status Dashboard"

    docker-compose -f $COMPOSE_FILE ps
    echo ""

    # Check health endpoints
    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}Health Status Monitor${NC}\n"

    # Create a dashboard-style table
    echo_e "  ${SOFT_GRAY}┌─────────────────────────┬──────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}                 ${SOFT_GRAY}│${NC} ${BOLD}Status${NC}           ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Check PostgreSQL database
    printf "  ${SOFT_GRAY}│${NC} PostgreSQL ($DB_NAME)     ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T $DB_SERVICE pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Redis
    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T redis redis-cli --pass devredispass ping &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # MinIO
    printf "  ${SOFT_GRAY}│${NC} MinIO Storage           ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T minio curl -fs http://localhost:9000/minio/health/live &>/dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # LM Studio AI (primary)
    printf "  ${SOFT_GRAY}│${NC} LM Studio AI            ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:${LM_STUDIO_PORT}/v1/models &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    elif curl -f -s http://localhost:${OLLAMA_PORT}/api/tags &>/dev/null; then
        echo_e "${SOFT_YELLOW}◒ OLLAMA${NC}        ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Traefik Gateway
    printf "  ${SOFT_GRAY}│${NC} Traefik Gateway         ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:${GATEWAY_DASHBOARD_PORT}/api/overview &>/dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Dokus Server (via gateway)
    printf "  ${SOFT_GRAY}│${NC} Dokus Server            ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:${GATEWAY_PORT}/api/v1/server/info &>/dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Services - Check via gateway using path-based routing
    local services=(
        "Dokus Server:dokus-server:/api/v1/server/info"
    )

    check_service() {
        local name=$1
        local container=$2
        local endpoint=$3

        # If container is not running, surface that clearly
        if ! docker-compose -f $COMPOSE_FILE ps -q "$container" | grep -q .; then
            printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} ${SOFT_RED}⨯ NOT RUNNING${NC}   ${SOFT_GRAY}│${NC}\n" "$name"
            return
        fi

        # Health probe via gateway - accept any HTTP response (401/404 means service is reachable)
        local http_code=$(curl -s -o /dev/null -w "%{http_code}" "http://localhost:${GATEWAY_PORT}${endpoint}" 2>/dev/null)
        if [ "$http_code" != "000" ] && [ "$http_code" != "" ]; then
            printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} ${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}\n" "$name"
        else
            printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} ${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}\n" "$name"
        fi
    }

    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name container endpoint <<< "$service_info"
        check_service "$service_name" "$container" "$endpoint"
    done

    echo_e "  ${SOFT_GRAY}└─────────────────────────┴──────────────────┘${NC}"
    echo ""
}

# Function to clean everything
clean_all() {
    print_gradient_header "🧹 Deep Clean"

    print_status warning "This will remove all containers, volumes, and data!"
    echo ""
    printf "  ${BOLD}${SOFT_RED}Are you sure?${NC} ${DIM_WHITE}(y/N):${NC} "
    read -n 1 -r
    echo ""
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status loading "Removing containers and volumes..."
        docker-compose -f $COMPOSE_FILE down -v
        rm -rf logs/*
        echo ""
        print_status success "Cleanup complete - all data removed"
    else
        print_status info "Operation cancelled"
    fi
    echo ""
}

# Function to reset database
reset_db() {
    print_gradient_header "🔄 Database Reset"

    print_status warning "This will reset the database ($DB_NAME) and delete all data!"
    echo ""
    printf "  ${BOLD}${SOFT_RED}Are you sure?${NC} ${DIM_WHITE}(y/N):${NC} "
    read -n 1 -r
    echo ""
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status loading "Resetting database..."
        docker-compose -f $COMPOSE_FILE stop postgres > /dev/null 2>&1
        docker-compose -f $COMPOSE_FILE rm -f postgres > /dev/null 2>&1
        docker volume rm deployment_postgres-data 2>/dev/null || true
        docker-compose -f $COMPOSE_FILE up -d postgres > /dev/null 2>&1
        echo ""
        print_status success "Database reset complete"
    else
        print_status info "Operation cancelled"
    fi
    echo ""
}

# Function to access database
access_db() {
    print_gradient_header "🗄️  Database CLI Access"

    print_status info "Connecting to database ($DB_NAME)..."
    echo ""
    docker-compose -f $COMPOSE_FILE exec $DB_SERVICE psql -U $DB_USER -d $DB_NAME
}

# Function to access Redis
access_redis() {
    print_gradient_header "🗄️  Redis CLI Access"
    docker-compose -f $COMPOSE_FILE exec redis redis-cli -a devredispass
}

# Function to check AI status (LM Studio or Ollama)
ollama_status() {
    print_gradient_header "🤖 AI Server Status"

    # Check LM Studio first (primary)
    printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "LM Studio"
    local lm_studio_running=false
    if curl -f -s http://localhost:${LM_STUDIO_PORT}/v1/models > /dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ Running${NC}"
        lm_studio_running=true
    else
        echo_e "${SOFT_YELLOW}◒ Not Running${NC}"
    fi

    # Check Ollama (fallback)
    printf "  ${SOFT_CYAN}${TREE_LAST}${TREE_RIGHT}${NC} %-22s" "Ollama"
    local ollama_running=false
    if curl -f -s http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ Running${NC}"
        ollama_running=true
    else
        echo_e "${SOFT_YELLOW}◒ Not Running${NC}"
    fi

    if [ "$lm_studio_running" = false ] && [ "$ollama_running" = false ]; then
        echo ""
        print_status warning "No AI server is running."
        echo_e "    ${DIM_WHITE}Start LM Studio (recommended) or Ollama.${NC}"
        return 1
    fi

    echo ""

    # Show LM Studio models if running
    if [ "$lm_studio_running" = true ]; then
        echo_e "  ${SOFT_CYAN}${BOLD}LM Studio Loaded Models:${NC}\n"
        local lm_models=$(curl -s http://localhost:${LM_STUDIO_PORT}/v1/models 2>/dev/null)
        if [ -n "$lm_models" ]; then
            echo "$lm_models" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    models = data.get('data', [])
    if models:
        for m in models:
            model_id = m.get('id', 'unknown')
            print(f'    ◎ {model_id}')
    else:
        print('    (no models loaded - load a model in LM Studio)')
except Exception as e:
    print('    (no models loaded)')
" 2>/dev/null || echo_e "    ${DIM_WHITE}(no models loaded)${NC}"
        fi
        echo ""
    fi

    # Show Ollama models if running
    if [ "$ollama_running" = true ]; then
        echo_e "  ${SOFT_CYAN}${BOLD}Ollama Loaded Models:${NC}\n"
        local models=$(curl -s http://localhost:${OLLAMA_PORT}/api/tags 2>/dev/null)
        if [ -n "$models" ]; then
            echo "$models" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    models = data.get('models', [])
    if models:
        for m in models:
            name = m.get('name', 'unknown')
            size = m.get('size', 0)
            size_gb = size / (1024**3)
            print(f'    ◎ {name:30s} ({size_gb:.1f} GB)')
    else:
        print('    (no models installed)')
except:
    print('    (no models installed)')
" 2>/dev/null || echo_e "    ${DIM_WHITE}(no models installed)${NC}"
        else
            echo_e "    ${DIM_WHITE}(no models installed)${NC}"
        fi
        echo ""
    fi
}

# Function to pull AI models
ollama_pull() {
    print_gradient_header "🤖 Pull AI Models"

    # Check if LM Studio is running
    if curl -f -s http://localhost:${LM_STUDIO_PORT}/v1/models > /dev/null 2>&1; then
        echo_e "  ${SOFT_GREEN}${BOLD}LM Studio Detected${NC}\n"
        echo_e "  ${DIM_WHITE}LM Studio models are managed through the LM Studio app.${NC}"
        echo_e "  ${DIM_WHITE}Open LM Studio → Search → Download models directly.${NC}\n"
        echo_e "  ${SOFT_CYAN}${BOLD}Recommended models for Dokus:${NC}"
        echo_e "    ${SOFT_CYAN}•${NC} qwen2.5-vl-7b-instruct  ${DIM_WHITE}(Vision model - classification & extraction)${NC}"
        echo_e "    ${SOFT_CYAN}•${NC} qwen2.5-32b-instruct    ${DIM_WHITE}(Orchestrator - reasoning & tool calling)${NC}"
        echo_e "    ${SOFT_CYAN}•${NC} nomic-embed-text        ${DIM_WHITE}(Embeddings for RAG)${NC}"
        echo ""
        print_status info "If you also want to pull models via Ollama, start Ollama first."
        echo ""
        return 0
    fi

    # Check if Ollama is running
    if ! curl -f -s http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        print_status error "No AI server is running."
        echo_e "  ${DIM_WHITE}Start LM Studio (manage models via GUI) or Ollama (pull via CLI).${NC}"
        return 1
    fi

    echo_e "  ${SOFT_CYAN}${BOLD}Ollama: Models by IntelligenceMode:${NC}\n"
    echo_e "    ${SOFT_CYAN}${BOLD}Assisted${NC} ${DIM_WHITE}(≤16GB RAM, edge devices)${NC}"
    echo_e "    ${SOFT_CYAN}①${NC}  qwen3-vl:2b     ${DIM_WHITE}(Vision, fast - ~1.5GB)${NC}"
    echo_e "    ${SOFT_CYAN}②${NC}  qwen3-vl:8b     ${DIM_WHITE}(Vision, expert - ~5GB)${NC}"
    echo_e "    ${SOFT_CYAN}③${NC}  qwen3:8b        ${DIM_WHITE}(Chat - ~5GB)${NC}"
    echo ""
    echo_e "    ${SOFT_CYAN}${BOLD}Autonomous${NC} ${DIM_WHITE}(32-48GB RAM, MacBook Pro)${NC}"
    echo_e "    ${SOFT_CYAN}④${NC}  qwen3-vl:32b    ${DIM_WHITE}(Vision, expert - ~20GB)${NC}"
    echo_e "    ${SOFT_CYAN}⑤${NC}  qwen3:32b       ${DIM_WHITE}(Chat - ~20GB)${NC}"
    echo ""
    echo_e "    ${SOFT_CYAN}${BOLD}Sovereign${NC} ${DIM_WHITE}(≥64GB RAM, Mac Studio)${NC}"
    echo_e "    ${SOFT_CYAN}⑥${NC}  qwen3-vl:72b    ${DIM_WHITE}(Vision, expert - ~45GB)${NC}"
    echo ""
    echo_e "    ${SOFT_CYAN}${BOLD}Required for all modes${NC}"
    echo_e "    ${SOFT_CYAN}⑦${NC}  nomic-embed-text ${DIM_WHITE}(Embeddings - ~275MB)${NC}"
    echo ""
    echo_e "    ${SOFT_CYAN}${BOLD}Bundles${NC}"
    echo_e "    ${SOFT_CYAN}⑧${NC}  Assisted bundle  ${DIM_WHITE}(qwen3-vl:2b + qwen3-vl:8b + qwen3:8b + nomic)${NC}"
    echo_e "    ${SOFT_CYAN}⑨${NC}  Autonomous bundle ${DIM_WHITE}(Assisted + qwen3-vl:32b + qwen3:32b)${NC}"
    echo_e "    ${SOFT_CYAN}⓪${NC}  Cancel"
    echo ""

    printf "  ${BOLD}Enter choice ${DIM_WHITE}[0-9]:${NC} "
    read choice

    echo ""

    case $choice in
        1) pull_model "qwen3-vl:2b" ;;
        2) pull_model "qwen3-vl:8b" ;;
        3) pull_model "qwen3:8b" ;;
        4) pull_model "qwen3-vl:32b" ;;
        5) pull_model "qwen3:32b" ;;
        6) pull_model "qwen3-vl:72b" ;;
        7) pull_model "nomic-embed-text" ;;
        8)
            pull_model "qwen3-vl:2b"
            pull_model "qwen3-vl:8b"
            pull_model "qwen3:8b"
            pull_model "nomic-embed-text"
            ;;
        9)
            pull_model "qwen3-vl:2b"
            pull_model "qwen3-vl:8b"
            pull_model "qwen3:8b"
            pull_model "qwen3-vl:32b"
            pull_model "qwen3:32b"
            pull_model "nomic-embed-text"
            ;;
        0) print_status info "Cancelled" && return ;;
        *) print_status error "Invalid choice" ;;
    esac
}

# Helper function to pull a single model
pull_model() {
    local model=$1
    print_status loading "Pulling ${model}... (this may take a while)"
    echo ""

    # Pull model using Ollama HTTP API (works for host Ollama and for a container with 11434 published)
    curl -s --no-buffer "http://localhost:${OLLAMA_PORT}/api/pull" \
        -H "Content-Type: application/json" \
        -d "{\"name\":\"${model}\"}" \
        | while IFS= read -r line; do
            status=$(echo "$line" | sed -n 's/.*"status":"\\([^"]*\\)".*/\\1/p')
            error=$(echo "$line" | sed -n 's/.*"error":"\\([^"]*\\)".*/\\1/p')

            if [ -n "$error" ]; then
                echo_e "  ${SOFT_RED}${SYMBOL_ERROR}${NC} ${error}"
            elif [ -n "$status" ]; then
                echo_e "  ${DIM_WHITE}${status}${NC}"
            fi
        done

    echo ""
    if curl -s "http://localhost:${OLLAMA_PORT}/api/tags" | grep -q "\"name\":\"${model}\""; then
        print_status success "${model} pulled successfully"
    else
        print_status warning "Pull finished, but ${model} not found in tags yet (it may still be downloading)"
    fi
}

# Function to run a quick AI test
ollama_test() {
    print_gradient_header "🧪 AI Test"

    # Check LM Studio first
    if curl -f -s http://localhost:${LM_STUDIO_PORT}/v1/models > /dev/null 2>&1; then
        print_status loading "Testing LM Studio with a quick prompt..."
        echo ""

        # Get the first available model
        local model_id=$(curl -s http://localhost:${LM_STUDIO_PORT}/v1/models 2>/dev/null | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    models = data.get('data', [])
    if models:
        print(models[0].get('id', ''))
except:
    pass
" 2>/dev/null)

        if [ -z "$model_id" ]; then
            print_status error "No model loaded in LM Studio. Load a model first."
            return 1
        fi

        echo_e "  ${DIM_WHITE}Using model: ${model_id}${NC}\n"

        local response=$(curl -s http://localhost:${LM_STUDIO_PORT}/v1/chat/completions \
            -H "Content-Type: application/json" \
            -d "{
                \"model\": \"${model_id}\",
                \"messages\": [{\"role\": \"user\", \"content\": \"Say hello in one sentence.\"}],
                \"max_tokens\": 100
            }" 2>/dev/null)

        if [ -n "$response" ]; then
            echo "$response" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    choices = data.get('choices', [])
    if choices:
        content = choices[0].get('message', {}).get('content', 'No response')
        print('  Response:', content)
    else:
        error = data.get('error', {}).get('message', 'Unknown error')
        print('  Error:', error)
except Exception as e:
    print('  (Failed to parse response)')
" 2>/dev/null
            echo ""
            print_status success "LM Studio is responding correctly"
        else
            print_status error "No response from LM Studio."
        fi
        echo ""
        return 0
    fi

    # Fall back to Ollama
    if ! curl -f -s http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        print_status error "No AI server is running (LM Studio or Ollama)"
        return 1
    fi

    print_status loading "Testing Ollama with a quick prompt..."
    echo ""

    local response=$(curl -s http://localhost:${OLLAMA_PORT}/api/generate -d '{
        "model": "qwen3:8b",
        "prompt": "Say hello in one sentence.",
        "stream": false
    }' 2>/dev/null)

    if [ -n "$response" ]; then
        echo "$response" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    print('  Response:', data.get('response', 'No response'))
except:
    print('  (Failed to parse response)')
" 2>/dev/null
        echo ""
        print_status success "Ollama is responding correctly"
    else
        print_status error "No response from Ollama. Make sure a model is loaded."
    fi
    echo ""
}

# Function to run tests
run_tests() {
    service=${1:-all}

    if [ "$service" = "all" ]; then
        print_gradient_header "🧪 Running All Test Suites"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:auth:backend:test :features:banking:backend:test :features:cashflow:backend:test :features:contacts:backend:test
        else
            gradle :features:auth:backend:test :features:banking:backend:test :features:cashflow:backend:test :features:contacts:backend:test
        fi
    elif [ "$service" = "auth" ]; then
        print_gradient_header "🧪 Running Auth Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:auth:backend:test
        else
            gradle :features:auth:backend:test
        fi
    elif [ "$service" = "banking" ]; then
        print_gradient_header "🧪 Running Banking Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:banking:backend:test
        else
            gradle :features:banking:backend:test
        fi
    elif [ "$service" = "cashflow" ]; then
        print_gradient_header "🧪 Running Cashflow Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:cashflow:backend:test
        else
            gradle :features:cashflow:backend:test
        fi
    elif [ "$service" = "contacts" ]; then
        print_gradient_header "🧪 Running Contacts Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:contacts:backend:test
        else
            gradle :features:contacts:backend:test
        fi
    else
        print_status error "Invalid service type. Use 'all', 'auth', 'banking', 'cashflow', or 'contacts'"
        exit 1
    fi
    echo ""
}

# Function to get local IP address
get_local_ip() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        # macOS - try multiple interfaces
        ipconfig getifaddr en0 2>/dev/null || \
        ipconfig getifaddr en1 2>/dev/null || \
        ipconfig getifaddr en2 2>/dev/null || \
        echo "localhost"
    else
        # Linux
        hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost"
    fi
}

# Function to show mobile app connection info with QR code
show_mobile_connection() {
    print_gradient_header "📱 Mobile App Connection" "Connect your mobile app to this server"

    # Get local IP address
    local LOCAL_IP=$(get_local_ip)

    # Generate deep link URL (dokus:// scheme opens the app directly)
    local CONNECT_URL="dokus://connect?host=${LOCAL_IP}&port=${GATEWAY_PORT}&protocol=http"

    echo_e "  ${SOFT_CYAN}${BOLD}Server Connection Details${NC}\n"

    echo_e "  ${SOFT_GRAY}┌───────────────────────────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Manual Entry (in app: Connect to Server)${NC}                      ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├───────────────────────────────────────────────────────────────┤${NC}"
    printf "  ${SOFT_GRAY}│${NC}  Protocol:  ${SOFT_CYAN}%-47s${NC} ${SOFT_GRAY}│${NC}\n" "http"
    printf "  ${SOFT_GRAY}│${NC}  Host:      ${SOFT_CYAN}%-47s${NC} ${SOFT_GRAY}│${NC}\n" "${LOCAL_IP}"
    printf "  ${SOFT_GRAY}│${NC}  Port:      ${SOFT_CYAN}%-47s${NC} ${SOFT_GRAY}│${NC}\n" "${GATEWAY_PORT}"
    echo_e "  ${SOFT_GRAY}└───────────────────────────────────────────────────────────────┘${NC}"

    echo ""

    # Generate QR code (requires qrencode)
    if command -v qrencode &> /dev/null; then
        echo_e "  ${SOFT_CYAN}${BOLD}Scan with Device Camera${NC}\n"
        qrencode -t ANSIUTF8 -m 2 "$CONNECT_URL"
        echo ""
        echo_e "  ${SOFT_GRAY}Deep Link:${NC} ${DIM_WHITE}${CONNECT_URL}${NC}"
    else
        echo_e "  ${SOFT_YELLOW}${SYMBOL_WARNING}${NC}  Install 'qrencode' for QR code display:"
        echo_e "     ${DIM_WHITE}brew install qrencode${NC}  (macOS)"
        echo_e "     ${DIM_WHITE}apt install qrencode${NC}   (Linux)"
        echo ""
        echo_e "  ${SOFT_GRAY}Deep Link:${NC} ${DIM_WHITE}${CONNECT_URL}${NC}"
    fi

    echo ""
    echo_e "  ${SOFT_GRAY}${DIM}Scan QR code with your device camera to open Dokus app,${NC}"
    echo_e "  ${SOFT_GRAY}${DIM}or enter the details manually via 'Connect to Server'.${NC}"
    echo ""
}

# Function to print service information
print_services_info() {
    print_separator
    echo ""
    echo_e "  ${SOFT_GREEN}${BOLD}🌐 API Gateway${NC}\n"

    # Gateway info box
    echo_e "  ${SOFT_GRAY}┌──────────────────────────────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}  ${BOLD}${SOFT_CYAN}http://${LOCAL_IP}:${GATEWAY_PORT}${NC}   ${DIM_WHITE}← API Gateway (network)${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}  ${DIM_WHITE}Dashboard: ${SOFT_ORANGE}http://localhost:${GATEWAY_DASHBOARD_PORT}${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}  ${DIM_WHITE}Storage:   ${SOFT_YELLOW}${STORAGE_PUBLIC_URL}${NC}"
    echo_e "  ${SOFT_GRAY}└──────────────────────────────────────────────────────────────────┘${NC}"

    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}📍 API Routes (via Gateway)${NC}\n"

    # Routes table
    echo_e "  ${SOFT_GRAY}┌─────────────────────────────┬────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Route Prefix${NC}                ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}                            ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────────┼────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/identity/*${NC}         ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/account/*${NC}          ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/tenants/*${NC}          ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/team/*${NC}             ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────────┼────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/invoices/*${NC}         ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/expenses/*${NC}         ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/cashflow/*${NC}         ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/documents/*${NC}        ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────────┼────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/payments/*${NC}         ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────────┼────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/banking/*${NC}          ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────────┼────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/api/v1/contacts/*${NC}         ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Dokus Server${NC}                    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}└─────────────────────────────┴────────────────────────────────────┘${NC}"

    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}💾 Database & Services${NC}\n"

    # Database table
    echo_e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}PostgreSQL${NC}           ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:$DB_PORT${NC} • ${SOFT_GRAY}$DB_NAME${NC}         ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:16379${NC} • ${SOFT_GRAY}pass: devredispass${NC} ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_YELLOW}MinIO${NC}                ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:9000${NC} • ${SOFT_GRAY}Console: 9001${NC}      ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}LM Studio${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:1234${NC} • ${SOFT_GRAY}OpenAI-compat${NC}    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    if docker-compose -f $COMPOSE_FILE ps | grep -q pgadmin; then
        echo ""
        echo_e "  ${SOFT_INFO}${SYMBOL_INFO}${NC}  pgAdmin: ${DIM_WHITE}http://localhost:5050${NC} ${SOFT_GRAY}(admin@dokus.ai / admin)${NC}"
    fi

    echo ""
    echo_e "  ${DIM_WHITE}User: ${SOFT_CYAN}$DB_USER${NC} ${SOFT_GRAY}•${NC} ${DIM_WHITE}Password: ${SOFT_CYAN}$DB_PASSWORD${NC}"
    echo ""
    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}🔧 Quick Commands${NC}\n"
    echo_e "    ${SOFT_GRAY}./dev.sh logs${NC}         ${DIM_WHITE}View all service logs${NC}"
    echo_e "    ${SOFT_GRAY}./dev.sh db${NC}           ${DIM_WHITE}Access PostgreSQL database${NC}"
    echo_e "    ${SOFT_GRAY}./dev.sh redis${NC}        ${DIM_WHITE}Access Redis CLI${NC}"
    echo_e "    ${SOFT_GRAY}./dev.sh status${NC}       ${DIM_WHITE}Check service health${NC}"
    echo_e "    ${SOFT_GRAY}./dev.sh test${NC}         ${DIM_WHITE}Run all test suites${NC}"
    echo_e "    ${SOFT_GRAY}./dev.sh ai${NC}           ${DIM_WHITE}Check AI status (LM Studio/Ollama)${NC}"
    echo_e "    ${SOFT_GRAY}./dev.sh ai-pull${NC}      ${DIM_WHITE}Pull AI models${NC}"
    echo ""
}

# Function to start pgAdmin
start_pgadmin() {
    print_gradient_header "🗄️  Starting pgAdmin"
    docker-compose -f $COMPOSE_FILE --profile tools up -d pgadmin
    echo ""
    print_status success "pgAdmin started at http://localhost:5050"
    print_status info "Login: admin@dokus.ai / admin"
    echo ""
}

# Function to watch for changes and rebuild
watch_mode() {
    print_color "$SOFT_BLUE" "👁️  Watch mode - rebuilding on changes..."
    print_color "$SOFT_YELLOW" "Press Ctrl+C to stop"

    # Initial build and start
    build_app
    restart_services

    # Watch for changes (requires fswatch)
    if command -v fswatch &> /dev/null; then
        fswatch -o \
            ${SERVER_SERVICE_DIR}/src \
            foundation/*/src \
            features/*/backend/src \
            | while read num ; do
                print_color "$SOFT_YELLOW" "🔄 Changes detected, rebuilding server..."
                build_app
                docker-compose -f $COMPOSE_FILE restart dokus-server
                print_color "$SOFT_GREEN" "✓ Server restarted"
            done
    else
        print_color "$SOFT_YELLOW" "⚠️  fswatch not installed. Install it for file watching:"
        echo "  macOS: brew install fswatch"
        echo "  Linux: apt-get install fswatch"
    fi
}

# Main script
main() {
    case ${1:-} in
        start)
            check_requirements
            build_app
            start_services
            ;;
        stop)
            stop_services
            ;;
        restart)
            check_requirements
            build_app
            restart_services
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs ${2:-}
            ;;
        build)
            check_requirements
            build_app
            ;;
        clean)
            clean_all
            ;;
        reset-db)
            reset_db
            ;;
        db)
            access_db
            ;;
        redis)
            access_redis
            ;;
        ai)
            ollama_status
            ;;
        ai-pull)
            ollama_pull
            ;;
        ai-test)
            ollama_test
            ;;
        test)
            run_tests ${2:-all}
            ;;
        pgadmin)
            start_pgadmin
            ;;
        watch)
            check_requirements
            watch_mode ${2:-all}
            ;;
        connect|qr)
            show_mobile_connection
            ;;
        help)
            show_help
            ;;
        *)
            # If no command, show interactive menu
            show_menu
            ;;
    esac
}

# Function to show help
show_help() {
    print_gradient_header "🚀 Dokus Dev Console" "Command palette"

    echo_e "  ${BOLD}Usage:${NC} ./dev.sh <command> [options]\n"

    print_separator
    echo ""
    echo_e "  ${SOFT_GREEN}${BOLD}Stack Control${NC}"
    echo_e "    ${SOFT_CYAN}start${NC}        ${DIM_WHITE}Build + launch all services${NC}"
    echo_e "    ${SOFT_CYAN}stop${NC}         ${DIM_WHITE}Shutdown containers${NC}"
    echo_e "    ${SOFT_CYAN}restart${NC}      ${DIM_WHITE}Rebuild and restart stack${NC}"
    echo_e "    ${SOFT_CYAN}status${NC}       ${DIM_WHITE}Dashboard with health probes${NC}"
    echo_e "    ${SOFT_CYAN}logs${NC} [svc]   ${DIM_WHITE}Tail logs (all or target service)${NC}"
    echo ""

    echo_e "  ${SOFT_MAGENTA}${BOLD}Build & Development${NC}"
    echo_e "    ${SOFT_CYAN}build${NC}        ${DIM_WHITE}Build dokus-server JAR + image${NC}"
    echo_e "    ${SOFT_CYAN}watch${NC}        ${DIM_WHITE}Auto rebuild + restart server${NC}"
    echo_e "    ${SOFT_CYAN}test${NC} [svc]   ${DIM_WHITE}Run tests (auth|banking|cashflow|contacts|all)${NC}"
    echo ""

    echo_e "  ${SOFT_YELLOW}${BOLD}Data & Tooling${NC}"
    echo_e "    ${SOFT_CYAN}db${NC}           ${DIM_WHITE}psql into the local database${NC}"
    echo_e "    ${SOFT_CYAN}redis${NC}        ${DIM_WHITE}Redis CLI access${NC}"
    echo_e "    ${SOFT_CYAN}reset-db${NC}     ${DIM_WHITE}Blow away the DB volume${NC}"
    echo_e "    ${SOFT_CYAN}pgadmin${NC}      ${DIM_WHITE}Launch pgAdmin profile${NC}"
    echo ""

    echo_e "  ${SOFT_ORANGE}${BOLD}AI (LM Studio / Ollama)${NC}"
    echo_e "    ${SOFT_CYAN}ai${NC}           ${DIM_WHITE}AI server status + loaded models${NC}"
    echo_e "    ${SOFT_CYAN}ai-pull${NC}      ${DIM_WHITE}Model download guide${NC}"
    echo_e "    ${SOFT_CYAN}ai-test${NC}      ${DIM_WHITE}Send a quick prompt${NC}"
    echo ""

    echo_e "  ${SOFT_RED}${BOLD}Maintenance${NC}"
    echo_e "    ${SOFT_CYAN}clean${NC}        ${DIM_WHITE}Remove containers and volumes${NC}"
    echo ""

    echo_e "  ${SOFT_BLUE}${BOLD}Mobile App${NC}"
    echo_e "    ${SOFT_CYAN}connect${NC}      ${DIM_WHITE}Show QR code for mobile app connection${NC}"
    echo_e "    ${SOFT_CYAN}qr${NC}           ${DIM_WHITE}Alias for connect${NC}"
    echo ""

    print_separator
    echo ""
    echo_e "  ${SOFT_GRAY}${DIM}Tip: run './dev.sh' without args for the animated console.${NC}"
    echo ""
}

# Function to show interactive menu
show_menu() {
    clear
    print_gradient_header "Dokus Dev Console" "Neon control deck for the local stack"
    echo_e "  ${DIM_WHITE}Navigate with numbers, hit Enter to confirm.${NC}\n"

    echo_e "  ${SOFT_GREEN}${BOLD}Launch Deck${NC}"
    echo_e "    ${SOFT_CYAN}1${NC}   Ignite stack (build + start)"
    echo_e "    ${SOFT_CYAN}2${NC}   Halt stack"
    echo_e "    ${SOFT_CYAN}3${NC}   Rebuild + restart"
    echo ""

    echo_e "  ${SOFT_MAGENTA}${BOLD}Diagnostics${NC}"
    echo_e "    ${SOFT_CYAN}4${NC}   Status dashboard"
    echo_e "    ${SOFT_CYAN}5${NC}   Live logs"
    echo_e "    ${SOFT_CYAN}6${NC}   Database console"
    echo_e "    ${SOFT_CYAN}7${NC}   Redis console"
    echo ""

    echo_e "  ${SOFT_ORANGE}${BOLD}AI (LM Studio/Ollama)${NC}"
    echo_e "    ${SOFT_CYAN}8${NC}   AI server status"
    echo_e "    ${SOFT_CYAN}9${NC}   Model download guide"
    echo_e "    ${SOFT_CYAN}10${NC}  Run all tests"
    echo ""

    echo_e "  ${SOFT_YELLOW}${BOLD}Utilities${NC}"
    echo_e "    ${SOFT_CYAN}11${NC}  Start pgAdmin"
    echo_e "    ${SOFT_CYAN}12${NC}  Deep clean"
    echo ""

    echo_e "  ${SOFT_BLUE}${BOLD}Mobile App${NC}"
    echo_e "    ${SOFT_CYAN}13${NC}  Show mobile connection (QR code)"
    echo ""

    echo_e "  ${SOFT_GRAY}0${NC}    Exit"
    echo ""

    printf "  ${BOLD}Select channel ${DIM_WHITE}[0-13]:${NC} "
    read choice

    echo ""

    case $choice in
        1) check_requirements && build_app && start_services ;;
        2) stop_services ;;
        3) check_requirements && build_app && restart_services ;;
        4) show_status ;;
        5) show_logs ;;
        6) access_db ;;
        7) access_redis ;;
        8) ollama_status ;;
        9) ollama_pull ;;
        10) run_tests all ;;
        11) start_pgadmin ;;
        12) clean_all ;;
        13) show_mobile_connection ;;
        0) echo_e "  ${SOFT_CYAN}👋 See you in the grid.${NC}\n" && exit 0 ;;
        *) print_status error "Invalid choice" && sleep 1 && show_menu ;;
    esac

    echo ""
    printf "  ${DIM}Press Enter to continue...${NC}"
    read
    show_menu
}

# Run main function
main "$@"
