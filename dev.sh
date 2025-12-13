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
COMPOSE_FILE="docker-compose.local.yml"
AUTH_SERVICE_DIR="features/auth/backend"
BANKING_SERVICE_DIR="features/banking/backend"

# Database configuration - consolidated single database
DB_CONTAINER="postgres-local"
DB_PORT="15432"
DB_NAME="dokus"
DB_USER="dev"
DB_PASSWORD="devpassword"

# AI/Ollama configuration
OLLAMA_CONTAINER="ollama-local"
OLLAMA_PORT="11434"
OLLAMA_DEFAULT_MODELS=("mistral:7b" "llama3.1:8b")

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

# Function to print a simple rounded box header
print_rounded_header() {
    print_gradient_header "$@"
}

# Function to print a section divider
print_divider() {
    local width=${2:-72}
    echo -n "  "
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
    print_gradient_header "🔨 Building Application Services"

    local services=("auth" "banking" "payment" "cashflow" "contacts")
    local total=${#services[@]}
    local current=0

    echo_e "  ${SOFT_CYAN}${BOLD}Phase 1: Building JAR files${NC}\n"

    for service in "${services[@]}"; do
        current=$((current + 1))
        local service_name="$(capitalize "$service") Service"

        print_simple_status building "Building ${service_name} JAR..."

        if [ -f "./gradlew" ]; then
            ./gradlew :features:${service}:backend:shadowJar -x test -q > /dev/null 2>&1
        else
            gradle :features:${service}:backend:shadowJar -x test -q > /dev/null 2>&1
        fi

        if [ $? -ne 0 ]; then
            print_status error "${service_name} JAR build failed"
            exit 1
        fi
        print_simple_status success "${service_name} JAR compiled"
    done

    echo ""
    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}Phase 2: Building Docker images${NC}\n"

    current=0
    for service in "${services[@]}"; do
        current=$((current + 1))
        local service_name="$(capitalize "$service") Service"

        print_simple_status building "Building ${service_name} image..."
        docker build -f features/${service}/backend/Dockerfile.dev -t invoid-vision/dokus-${service}:dev-latest . -q > /dev/null 2>&1
        if [ $? -ne 0 ]; then
            print_status error "${service_name} Docker image build failed"
            exit 1
        fi
        print_simple_status success "${service_name} image ready"
    done

    echo ""
    echo_e "  ${SOFT_GREEN}${BOLD}✓${NC}  ${SOFT_GREEN}All services built successfully${NC}"
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
            if docker-compose -f $COMPOSE_FILE exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo_e "${SOFT_RED}⨯ Timeout${NC}"
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Redis
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Redis Cache"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T redis-local redis-cli --pass devredispass ping &>/dev/null; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for RabbitMQ
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "RabbitMQ Broker"
        for i in {1..30}; do
            if curl -f -s -u dokus:localrabbitpass http://localhost:25673/api/health/checks/alarms &>/dev/null; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Ollama (AI)
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Ollama AI Server"
        for i in {1..60}; do
            if curl -f -s http://localhost:${OLLAMA_PORT}/api/tags &>/dev/null; then
                echo_e "${SOFT_GREEN}◎ Ready${NC}"
                break
            fi
            if [ $i -eq 60 ]; then
                echo_e "${SOFT_YELLOW}◒ Slow Start${NC}"
            fi
            echo -n "."
            sleep 1
        done

        # Wait for services with proper spacing
        sleep 3

        local services=(
            "Auth:7091:/metrics"
            "Cashflow:7092:/health"
            "Payment:7093:/health"
            "Banking:7096:/health"
            "Contacts:7097:/health"
        )

        for service_info in "${services[@]}"; do
            IFS=':' read -r service_name port endpoint <<< "$service_info"
            printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "${service_name} Service"
            for i in {1..30}; do
                if curl -f -s http://localhost:${port}${endpoint} > /dev/null 2>&1; then
                    echo_e "${SOFT_GREEN}◎ Ready${NC}"
                    break
                fi
                echo -n "."
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
    if docker-compose -f $COMPOSE_FILE exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Redis
    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T redis-local redis-cli --pass devredispass ping &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # RabbitMQ
    printf "  ${SOFT_GRAY}│${NC} RabbitMQ Broker         ${SOFT_GRAY}│${NC} "
    if curl -f -s -u dokus:localrabbitpass http://localhost:25673/api/health/checks/alarms &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Ollama AI
    printf "  ${SOFT_GRAY}│${NC} Ollama AI Server        ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:${OLLAMA_PORT}/api/tags &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Services
    local services=(
        "Auth Service:auth-service-local:7091:/metrics"
        "Cashflow Service:cashflow-service-local:7092:/health"
        "Payment Service:payment-service-local:7093:/health"
        "Banking Service:banking-service-local:7096:/health"
        "Contacts Service:contacts-service-local:7097:/health"
    )

    check_service() {
        local name=$1
        local container=$2
        local port=$3
        local endpoint=$4

        # If container is not running, surface that clearly
        if ! docker-compose -f $COMPOSE_FILE ps -q "$container" | grep -q .; then
            printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} ${SOFT_RED}⨯ NOT RUNNING${NC}   ${SOFT_GRAY}│${NC}\n" "$name"
            return
        fi

        # Health probe
        if curl -f -s "http://localhost:${port}${endpoint}" > /dev/null 2>&1; then
            printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} ${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}\n" "$name"
        else
            printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} ${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}\n" "$name"
        fi
    }

    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name container port endpoint <<< "$service_info"
        check_service "$service_name" "$container" "$port" "$endpoint"
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
        docker-compose -f $COMPOSE_FILE stop $DB_CONTAINER > /dev/null 2>&1
        docker-compose -f $COMPOSE_FILE rm -f $DB_CONTAINER > /dev/null 2>&1
        docker volume rm the-predict_postgres-local 2>/dev/null || true
        docker-compose -f $COMPOSE_FILE up -d $DB_CONTAINER > /dev/null 2>&1
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
    docker-compose -f $COMPOSE_FILE exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME
}

# Function to access Redis
access_redis() {
    print_gradient_header "🗄️  Redis CLI Access"
    docker-compose -f $COMPOSE_FILE exec redis-local redis-cli -a devredispass
}

# Function to check Ollama status
ollama_status() {
    print_gradient_header "🤖 Ollama AI Status"

    # Check if container is running
    printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Ollama Server"
    if curl -f -s http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ Running${NC}"
    else
        echo_e "${SOFT_RED}⨯ Not Running${NC}"
        echo ""
        print_status warning "Ollama is not running. Start services first."
        return 1
    fi

    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}Loaded Models:${NC}\n"

    # Get list of models
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
}

# Function to pull AI models
ollama_pull() {
    print_gradient_header "🤖 Pull AI Models"

    # Check if Ollama is running
    if ! curl -f -s http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        print_status error "Ollama is not running. Start services first with: ./dev.sh start"
        return 1
    fi

    echo_e "  ${SOFT_CYAN}${BOLD}Available models to pull:${NC}\n"
    echo_e "    ${SOFT_CYAN}①${NC}  mistral:7b      ${DIM_WHITE}(Recommended - 4.1GB, fast)${NC}"
    echo_e "    ${SOFT_CYAN}②${NC}  llama3.1:8b     ${DIM_WHITE}(Alternative - 4.7GB)${NC}"
    echo_e "    ${SOFT_CYAN}③${NC}  llama3.2:3b     ${DIM_WHITE}(Lightweight - 2.0GB)${NC}"
    echo_e "    ${SOFT_CYAN}④${NC}  gemma2:9b       ${DIM_WHITE}(Quality - 5.4GB)${NC}"
    echo_e "    ${SOFT_CYAN}⑤${NC}  All recommended  ${DIM_WHITE}(mistral:7b + llama3.1:8b)${NC}"
    echo_e "    ${SOFT_CYAN}⓪${NC}  Cancel"
    echo ""

    printf "  ${BOLD}Enter choice ${DIM_WHITE}[0-5]:${NC} "
    read choice

    echo ""

    case $choice in
        1) pull_model "mistral:7b" ;;
        2) pull_model "llama3.1:8b" ;;
        3) pull_model "llama3.2:3b" ;;
        4) pull_model "gemma2:9b" ;;
        5)
            pull_model "mistral:7b"
            pull_model "llama3.1:8b"
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

    # Pull model using Ollama API
    docker-compose -f $COMPOSE_FILE exec -T $OLLAMA_CONTAINER ollama pull $model

    if [ $? -eq 0 ]; then
        echo ""
        print_status success "${model} pulled successfully"
    else
        echo ""
        print_status error "Failed to pull ${model}"
    fi
}

# Function to run a quick AI test
ollama_test() {
    print_gradient_header "🧪 AI Test"

    if ! curl -f -s http://localhost:${OLLAMA_PORT}/api/tags > /dev/null 2>&1; then
        print_status error "Ollama is not running"
        return 1
    fi

    print_status loading "Testing Ollama with a quick prompt..."
    echo ""

    local response=$(curl -s http://localhost:${OLLAMA_PORT}/api/generate -d '{
        "model": "mistral:7b",
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
        print_status success "AI is responding correctly"
    else
        print_status error "No response from AI. Make sure a model is loaded."
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

# Function to print service information
print_services_info() {
    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}📍 Service Endpoints${NC}\n"

    # Service table
    echo_e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Endpoints${NC}                               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"

    # Services
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Auth Service${NC}         ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7091${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/metrics /health${NC} • ${SOFT_GRAY}debug: 15007${NC}   ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Cashflow Service${NC}     ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7092${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15008${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Payment Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7093${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15009${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Banking Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7096${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15012${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Contacts Service${NC}     ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7097${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15013${NC}               ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}💾 Database & Services${NC}\n"

    # Database table
    echo_e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}PostgreSQL${NC}           ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:$DB_PORT${NC} • ${SOFT_GRAY}$DB_NAME${NC}         ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:16379${NC} • ${SOFT_GRAY}pass: devredispass${NC} ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}RabbitMQ${NC}             ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:25672${NC} • ${SOFT_GRAY}user: dokus${NC}        ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}UI: localhost:25673${NC} • ${SOFT_GRAY}pass: localrabbitpass${NC} ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_YELLOW}MinIO${NC}                ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:19000${NC} • ${SOFT_GRAY}Console: 19001${NC}    ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Ollama AI${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:11434${NC} • ${SOFT_GRAY}API endpoint${NC}     ${SOFT_GRAY}│${NC}"
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
    echo_e "    ${SOFT_GRAY}./dev.sh ai${NC}           ${DIM_WHITE}Check AI/Ollama status${NC}"
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
    service=${1:-all}

    print_color "$SOFT_BLUE" "👁️  Watch mode - rebuilding on changes..."
    print_color "$SOFT_YELLOW" "Press Ctrl+C to stop"

    # Initial build and start
    build_app
    restart_services

    # Watch for changes (requires fswatch or inotify-tools)
    if command -v fswatch &> /dev/null; then
        if [ "$service" = "all" ]; then
            fswatch -o $AUTH_SERVICE_DIR/src $BANKING_SERVICE_DIR/src | while read num ; do
                print_color "$SOFT_YELLOW" "🔄 Changes detected, rebuilding..."
                build_app
                docker-compose -f $COMPOSE_FILE restart dokus-auth-local dokus-banking-local
                print_color "$SOFT_GREEN" "✓ Services restarted"
            done
        elif [ "$service" = "auth" ]; then
            fswatch -o $AUTH_SERVICE_DIR/src | while read num ; do
                print_color "$SOFT_YELLOW" "🔄 Auth Service changes detected, rebuilding..."
                if [ -f "./gradlew" ]; then
                    ./gradlew :features:auth:backend:shadowJar -x test
                else
                    gradle :features:auth:backend:shadowJar -x test
                fi
                docker build -f features/auth/backend/Dockerfile.dev -t invoid-vision/dokus-auth:dev-latest .
                docker-compose -f $COMPOSE_FILE restart dokus-auth-local
                print_color "$SOFT_GREEN" "✓ Auth Service restarted"
            done
        elif [ "$service" = "banking" ]; then
            fswatch -o $BANKING_SERVICE_DIR/src | while read num ; do
                print_color "$SOFT_YELLOW" "🔄 Banking Service changes detected, rebuilding..."
                if [ -f "./gradlew" ]; then
                    ./gradlew :features:banking:backend:shadowJar -x test
                else
                    gradle :features:banking:backend:shadowJar -x test
                fi
                docker build -f features/banking/backend/Dockerfile.dev -t invoid-vision/dokus-banking:dev-latest .
                docker-compose -f $COMPOSE_FILE restart dokus-banking-local
                print_color "$SOFT_GREEN" "✓ Banking Service restarted"
            done
        fi
    else
        print_color "$SOFT_YELLOW" "⚠️  fswatch not installed. Install it for file watching:"
        echo "  macOS: brew install fswatch"
        echo "  Linux: apt-get install inotify-tools"
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
    echo_e "    ${SOFT_CYAN}build${NC}        ${DIM_WHITE}Create service JARs + images${NC}"
    echo_e "    ${SOFT_CYAN}watch${NC} [svc]  ${DIM_WHITE}Auto rebuild on changes${NC}"
    echo_e "    ${SOFT_CYAN}test${NC} [svc]   ${DIM_WHITE}Run tests (auth|banking|cashflow|contacts|all)${NC}"
    echo ""

    echo_e "  ${SOFT_YELLOW}${BOLD}Data & Tooling${NC}"
    echo_e "    ${SOFT_CYAN}db${NC}           ${DIM_WHITE}psql into the local database${NC}"
    echo_e "    ${SOFT_CYAN}redis${NC}        ${DIM_WHITE}Redis CLI access${NC}"
    echo_e "    ${SOFT_CYAN}reset-db${NC}     ${DIM_WHITE}Blow away the DB volume${NC}"
    echo_e "    ${SOFT_CYAN}pgadmin${NC}      ${DIM_WHITE}Launch pgAdmin profile${NC}"
    echo ""

    echo_e "  ${SOFT_ORANGE}${BOLD}AI Mesh${NC}"
    echo_e "    ${SOFT_CYAN}ai${NC}           ${DIM_WHITE}Ollama status + loaded models${NC}"
    echo_e "    ${SOFT_CYAN}ai-pull${NC}      ${DIM_WHITE}Guided model download${NC}"
    echo_e "    ${SOFT_CYAN}ai-test${NC}      ${DIM_WHITE}Send a quick prompt${NC}"
    echo ""

    echo_e "  ${SOFT_RED}${BOLD}Maintenance${NC}"
    echo_e "    ${SOFT_CYAN}clean${NC}        ${DIM_WHITE}Remove containers and volumes${NC}"
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

    echo_e "  ${SOFT_ORANGE}${BOLD}AI / Tests${NC}"
    echo_e "    ${SOFT_CYAN}8${NC}   AI status"
    echo_e "    ${SOFT_CYAN}9${NC}   Pull AI models"
    echo_e "    ${SOFT_CYAN}10${NC}  Run all tests"
    echo ""

    echo_e "  ${SOFT_YELLOW}${BOLD}Utilities${NC}"
    echo_e "    ${SOFT_CYAN}11${NC}  Start pgAdmin"
    echo_e "    ${SOFT_CYAN}12${NC}  Deep clean"
    echo ""

    echo_e "  ${SOFT_GRAY}0${NC}    Exit"
    echo ""

    printf "  ${BOLD}Select channel ${DIM_WHITE}[0-12]:${NC} "
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
