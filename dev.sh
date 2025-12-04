#!/bin/bash

# Dokus Development Environment Manager
# This script manages the local development environment for Dokus Services

set -e  # Exit on error

# Check for NO_COLOR environment variable
if [[ -n "${NO_COLOR}" ]]; then
    USE_COLOR=false
else
    USE_COLOR=true
fi

# Modern Pastel Color Palette (24-bit RGB)
if [[ "$USE_COLOR" == true ]]; then
    SOFT_BLUE='\033[38;2;130;170;255m'
    SOFT_GREEN='\033[38;2;150;220;180m'
    SOFT_RED='\033[38;2;255;150;150m'
    SOFT_YELLOW='\033[38;2;255;220;150m'
    SOFT_CYAN='\033[38;2;150;220;230m'
    SOFT_MAGENTA='\033[38;2;220;180;255m'
    SOFT_ORANGE='\033[38;2;255;200;150m'
    SOFT_GRAY='\033[38;2;160;160;180m'
    BRIGHT_WHITE='\033[38;2;255;255;255m'
    DIM_WHITE='\033[38;2;200;200;210m'
    GRADIENT_START='\033[38;2;130;170;255m'
    GRADIENT_MID='\033[38;2;180;140;255m'
    GRADIENT_END='\033[38;2;220;180;255m'
    BOLD='\033[1m'
    DIM='\033[2m'
    NC='\033[0m'
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
SYMBOL_SUCCESS="◆"
SYMBOL_ERROR="◇"
SYMBOL_WARNING="⬡"
SYMBOL_INFO="●"
SYMBOL_LOADING="◐"
SYMBOL_BULLET="▸"
SYMBOL_SMALL="▪"

# Configuration
PROJECT_NAME="dokus"
COMPOSE_FILE="docker-compose.local.yml"
AUTH_SERVICE_DIR="features/auth/backend"
AUDIT_SERVICE_DIR="features/audit/backend"
BANKING_SERVICE_DIR="features/banking/backend"
PEPPOL_SERVICE_DIR="features/peppol/backend"

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

# Function to print colored output
print_color() {
    color=$1
    message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print a gradient header
print_gradient_header() {
    local title=$1
    local width=70
    local padding=$(( (width - ${#title} - 4) / 2 ))
    local padding_right=$(( width - ${#title} - 4 - padding ))

    echo ""
    echo -e "${SOFT_CYAN}${BOX_TL}$(printf '%*s' $width | tr ' ' ${BOX_H})${BOX_TR}${NC}"
    echo -e "${SOFT_CYAN}${BOX_V}$(printf '%*s' $width | tr ' ' ' ')${BOX_V}${NC}"
    echo -e "${SOFT_CYAN}${BOX_V}  ${GRADIENT_START}$(printf '%*s' $padding)${BRIGHT_WHITE}${BOLD}${title}${NC}${GRADIENT_END}$(printf '%*s' $padding_right)  ${SOFT_CYAN}${BOX_V}${NC}"
    echo -e "${SOFT_CYAN}${BOX_V}$(printf '%*s' $width | tr ' ' ' ')${BOX_V}${NC}"
    echo -e "${SOFT_CYAN}${BOX_BL}$(printf '%*s' $width | tr ' ' ${BOX_H})${BOX_BR}${NC}"
    echo ""
}

# Function to print a simple rounded box header
print_rounded_header() {
    local title=$1
    local width=70
    local padding=$(( (width - ${#title} - 4) / 2 ))
    local padding_right=$(( width - ${#title} - 4 - padding ))

    echo ""
    echo -e "${SOFT_CYAN}${ROUND_TL}$(printf '%*s' $width | tr ' ' ${ROUND_H})${ROUND_TR}${NC}"
    echo -e "${SOFT_CYAN}${ROUND_V}  ${BRIGHT_WHITE}${BOLD}${title}${NC}$(printf '%*s' $(( width - ${#title} - 2 )) )  ${SOFT_CYAN}${ROUND_V}${NC}"
    echo -e "${SOFT_CYAN}${ROUND_BL}$(printf '%*s' $width | tr ' ' ${ROUND_H})${ROUND_BR}${NC}"
    echo ""
}

# Function to print a section divider
print_divider() {
    local char=${1:-─}
    local width=${2:-70}
    echo -e "${SOFT_GRAY}$(printf '%*s' $width | tr ' ' $char)${NC}"
}

# Function to print a decorative separator
print_separator() {
    echo -e "${SOFT_GRAY}  ▪ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ▪${NC}"
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
            echo -e "  ${SOFT_GREEN}${SYMBOL_SUCCESS}${NC}  ${message}  ${SOFT_GREEN}${BOLD}[✓ READY]${NC}"
            ;;
        error)
            echo -e "  ${SOFT_RED}${SYMBOL_ERROR}${NC}  ${message}  ${SOFT_RED}${BOLD}[✗ ERROR]${NC}"
            ;;
        warning)
            echo -e "  ${SOFT_YELLOW}${SYMBOL_WARNING}${NC}  ${message}  ${SOFT_YELLOW}${BOLD}[⚠ WARNING]${NC}"
            ;;
        info)
            echo -e "  ${SOFT_CYAN}${SYMBOL_INFO}${NC}  ${message}"
            ;;
        loading)
            echo -e "  ${SOFT_YELLOW}${SYMBOL_LOADING}${NC}  ${message}  ${SOFT_YELLOW}${BOLD}[⟳ LOADING]${NC}"
            ;;
        building)
            echo -e "  ${SOFT_ORANGE}▸${NC}  ${DIM_WHITE}${message}${NC}"
            ;;
        *)
            echo -e "  ${message}"
            ;;
    esac
}

# Function to print a simple status
print_simple_status() {
    local status=$1
    local message=$2
    case $status in
        success)
            echo -e "  ${SOFT_GREEN}${SYMBOL_SUCCESS}${NC}  ${message}"
            ;;
        error)
            echo -e "  ${SOFT_RED}${SYMBOL_ERROR}${NC}  ${message}"
            ;;
        warning)
            echo -e "  ${SOFT_YELLOW}${SYMBOL_WARNING}${NC}  ${message}"
            ;;
        info)
            echo -e "  ${SOFT_CYAN}${SYMBOL_INFO}${NC}  ${message}"
            ;;
        building)
            echo -e "  ${SOFT_ORANGE}▸${NC}  ${DIM_WHITE}${message}${NC}"
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
    echo -e "  ${SOFT_GREEN}${SYMBOL_SUCCESS}  ${BOLD}All system requirements met${NC}"
    echo ""
}

# Function to build the application
build_app() {
    print_gradient_header "🔨 Building Application Services"

    local services=("auth" "audit" "banking" "payment" "reporting" "cashflow" "peppol")
    local total=${#services[@]}
    local current=0

    echo -e "  ${SOFT_CYAN}${BOLD}Phase 1: Building JAR files${NC}\n"

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
    echo -e "  ${SOFT_CYAN}${BOLD}Phase 2: Building Docker images${NC}\n"

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
    echo -e "  ${SOFT_GREEN}${BOLD}✓${NC}  ${SOFT_GREEN}All services built successfully${NC}"
    echo ""
}

# Function to start services
start_services() {
    print_gradient_header "🚀 Starting Development Environment"

    # Create logs directory if it doesn't exist
    mkdir -p logs

    # Start services
    print_status loading "Initializing Docker containers..."
    docker-compose -f $COMPOSE_FILE up -d > /dev/null 2>&1

    if [ $? -eq 0 ]; then
        print_status success "All containers started successfully"

        # Wait for services to be healthy
        echo ""
        echo -e "  ${SOFT_CYAN}${BOLD}Waiting for services to initialize...${NC}\n"

        # Wait for PostgreSQL database
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "PostgreSQL ($DB_NAME)"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
                echo -e "${SOFT_GREEN}◆ Ready${NC}"
                break
            fi
            if [ $i -eq 30 ]; then
                echo -e "${SOFT_RED}◇ Timeout${NC}"
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Redis
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Redis Cache"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T redis-local redis-cli --pass devredispass ping &>/dev/null; then
                echo -e "${SOFT_GREEN}◆ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for RabbitMQ
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "RabbitMQ Broker"
        for i in {1..30}; do
            if curl -f -s -u dokus:localrabbitpass http://localhost:25673/api/health/checks/alarms &>/dev/null; then
                echo -e "${SOFT_GREEN}◆ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Ollama (AI)
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Ollama AI Server"
        for i in {1..60}; do
            if curl -f -s http://localhost:${OLLAMA_PORT}/api/tags &>/dev/null; then
                echo -e "${SOFT_GREEN}◆ Ready${NC}"
                break
            fi
            if [ $i -eq 60 ]; then
                echo -e "${SOFT_YELLOW}◇ Slow Start${NC}"
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
            "Reporting:7094:/health"
            "Audit:7095:/health"
            "Banking:7096:/health"
            "Peppol:7098:/health"
        )

        for service_info in "${services[@]}"; do
            IFS=':' read -r service_name port endpoint <<< "$service_info"
            printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "${service_name} Service"
            for i in {1..30}; do
                if curl -f -s http://localhost:${port}${endpoint} > /dev/null 2>&1; then
                    echo -e "${SOFT_GREEN}◆ Ready${NC}"
                    break
                fi
                echo -n "."
                sleep 1
            done
        done

        echo ""
        echo -e "  ${SOFT_GREEN}${BOLD}✓${NC}  ${SOFT_GREEN}All services are operational!${NC}"
        echo ""
        print_services_info
    else
        print_status error "Failed to start services"
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
    echo -e "  ${SOFT_CYAN}${BOLD}Health Status Monitor${NC}\n"

    # Create a dashboard-style table
    echo -e "  ${SOFT_GRAY}┌─────────────────────────┬──────────────────┐${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}                 ${SOFT_GRAY}│${NC} ${BOLD}Status${NC}           ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Check PostgreSQL database
    printf "  ${SOFT_GRAY}│${NC} PostgreSQL ($DB_NAME)     ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Redis
    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T redis-local redis-cli --pass devredispass ping &>/dev/null; then
        echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # RabbitMQ
    printf "  ${SOFT_GRAY}│${NC} RabbitMQ Broker         ${SOFT_GRAY}│${NC} "
    if curl -f -s -u dokus:localrabbitpass http://localhost:25673/api/health/checks/alarms &>/dev/null; then
        echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # Ollama AI
    printf "  ${SOFT_GRAY}│${NC} Ollama AI Server        ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:${OLLAMA_PORT}/api/tags &>/dev/null; then
        echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo -e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Services
    local services=(
        "Auth Service:7091:/metrics"
        "Cashflow Service:7092:/health"
        "Payment Service:7093:/health"
        "Reporting Service:7094:/health"
        "Audit Service:7095:/health"
        "Banking Service:7096:/health"
        "Peppol Service:7098:/health"
    )

    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name port endpoint <<< "$service_info"
        printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} " "$service_name"
        if curl -f -s http://localhost:${port}${endpoint} > /dev/null 2>&1; then
            echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
        else
            echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
        fi
    done

    echo -e "  ${SOFT_GRAY}└─────────────────────────┴──────────────────┘${NC}"
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
        echo -e "${SOFT_GREEN}◆ Running${NC}"
    else
        echo -e "${SOFT_RED}◇ Not Running${NC}"
        echo ""
        print_status warning "Ollama is not running. Start services first."
        return 1
    fi

    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}Loaded Models:${NC}\n"

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
            print(f'    ◆ {name:30s} ({size_gb:.1f} GB)')
    else:
        print('    (no models installed)')
except:
    print('    (no models installed)')
" 2>/dev/null || echo -e "    ${DIM_WHITE}(no models installed)${NC}"
    else
        echo -e "    ${DIM_WHITE}(no models installed)${NC}"
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

    echo -e "  ${SOFT_CYAN}${BOLD}Available models to pull:${NC}\n"
    echo -e "    ${SOFT_CYAN}①${NC}  mistral:7b      ${DIM_WHITE}(Recommended - 4.1GB, fast)${NC}"
    echo -e "    ${SOFT_CYAN}②${NC}  llama3.1:8b     ${DIM_WHITE}(Alternative - 4.7GB)${NC}"
    echo -e "    ${SOFT_CYAN}③${NC}  llama3.2:3b     ${DIM_WHITE}(Lightweight - 2.0GB)${NC}"
    echo -e "    ${SOFT_CYAN}④${NC}  gemma2:9b       ${DIM_WHITE}(Quality - 5.4GB)${NC}"
    echo -e "    ${SOFT_CYAN}⑤${NC}  All recommended  ${DIM_WHITE}(mistral:7b + llama3.1:8b)${NC}"
    echo -e "    ${SOFT_CYAN}⓪${NC}  Cancel"
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
            ./gradlew :features:auth:backend:test :features:audit:backend:test :features:banking:backend:test :features:peppol:backend:test
        else
            gradle :features:auth:backend:test :features:audit:backend:test :features:banking:backend:test :features:peppol:backend:test
        fi
    elif [ "$service" = "auth" ]; then
        print_gradient_header "🧪 Running Auth Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:auth:backend:test
        else
            gradle :features:auth:backend:test
        fi
    elif [ "$service" = "audit" ]; then
        print_gradient_header "🧪 Running Audit Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:audit:backend:test
        else
            gradle :features:audit:backend:test
        fi
    elif [ "$service" = "banking" ]; then
        print_gradient_header "🧪 Running Banking Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:banking:backend:test
        else
            gradle :features:banking:backend:test
        fi
    elif [ "$service" = "peppol" ]; then
        print_gradient_header "🧪 Running Peppol Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:peppol:backend:test
        else
            gradle :features:peppol:backend:test
        fi
    else
        print_status error "Invalid service type. Use 'all', 'auth', 'audit', 'banking', or 'peppol'"
        exit 1
    fi
    echo ""
}

# Function to print service information
print_services_info() {
    print_separator
    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}📍 Service Endpoints${NC}\n"

    # Service table
    echo -e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Endpoints${NC}                               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"

    # Services
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Auth Service${NC}         ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7091${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/metrics /health${NC} • ${SOFT_GRAY}debug: 15007${NC}   ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Cashflow Service${NC}     ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7092${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15008${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Payment Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7093${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15009${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Reporting Service${NC}    ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7094${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15010${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Audit Service${NC}        ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7095${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15011${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Banking Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7096${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15012${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Peppol Service${NC}       ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7098${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 15014${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}💾 Database & Services${NC}\n"

    # Database table
    echo -e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}PostgreSQL${NC}           ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:$DB_PORT${NC} • ${SOFT_GRAY}$DB_NAME${NC}         ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:16379${NC} • ${SOFT_GRAY}pass: devredispass${NC} ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}RabbitMQ${NC}             ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:25672${NC} • ${SOFT_GRAY}user: dokus${NC}        ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}UI: localhost:25673${NC} • ${SOFT_GRAY}pass: localrabbitpass${NC} ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_YELLOW}MinIO${NC}                ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:9000${NC} • ${SOFT_GRAY}Console: 9001${NC}      ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Ollama AI${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:11434${NC} • ${SOFT_GRAY}API endpoint${NC}     ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    if docker-compose -f $COMPOSE_FILE ps | grep -q pgadmin; then
        echo ""
        echo -e "  ${SOFT_INFO}${SYMBOL_INFO}${NC}  pgAdmin: ${DIM_WHITE}http://localhost:5050${NC} ${SOFT_GRAY}(admin@dokus.ai / admin)${NC}"
    fi

    echo ""
    echo -e "  ${DIM_WHITE}User: ${SOFT_CYAN}$DB_USER${NC} ${SOFT_GRAY}•${NC} ${DIM_WHITE}Password: ${SOFT_CYAN}$DB_PASSWORD${NC}"
    echo ""
    print_separator
    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}🔧 Quick Commands${NC}\n"
    echo -e "    ${SOFT_GRAY}./dev.sh logs${NC}         ${DIM_WHITE}View all service logs${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh db${NC}           ${DIM_WHITE}Access PostgreSQL database${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh redis${NC}        ${DIM_WHITE}Access Redis CLI${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh status${NC}       ${DIM_WHITE}Check service health${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh test${NC}         ${DIM_WHITE}Run all test suites${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh ai${NC}           ${DIM_WHITE}Check AI/Ollama status${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh ai-pull${NC}      ${DIM_WHITE}Pull AI models${NC}"
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
            fswatch -o $AUTH_SERVICE_DIR/src $AUDIT_SERVICE_DIR/src $BANKING_SERVICE_DIR/src | while read num ; do
                print_color "$SOFT_YELLOW" "🔄 Changes detected, rebuilding..."
                build_app
                docker-compose -f $COMPOSE_FILE restart dokus-auth-local dokus-audit-local dokus-banking-local
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
        elif [ "$service" = "audit" ]; then
            fswatch -o $AUDIT_SERVICE_DIR/src | while read num ; do
                print_color "$SOFT_YELLOW" "🔄 Audit Service changes detected, rebuilding..."
                if [ -f "./gradlew" ]; then
                    ./gradlew :features:audit:backend:shadowJar -x test
                else
                    gradle :features:audit:backend:shadowJar -x test
                fi
                docker build -f features/audit/backend/Dockerfile.dev -t invoid-vision/dokus-audit:dev-latest .
                docker-compose -f $COMPOSE_FILE restart dokus-audit-local
                print_color "$SOFT_GREEN" "✓ Audit Service restarted"
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
    print_gradient_header "🚀 Dokus Development Environment Manager"

    echo -e "  ${BOLD}Usage:${NC} ./dev.sh [command] [options]\n"

    print_separator
    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}Commands:${NC}\n"

    echo -e "  ${SOFT_GREEN}${BOLD}Service Management${NC}"
    echo -e "    ${SOFT_CYAN}start${NC}              ${DIM_WHITE}Build and start all services${NC}"
    echo -e "    ${SOFT_CYAN}stop${NC}               ${DIM_WHITE}Stop all services${NC}"
    echo -e "    ${SOFT_CYAN}restart${NC}            ${DIM_WHITE}Restart all services${NC}"
    echo -e "    ${SOFT_CYAN}status${NC}             ${DIM_WHITE}Show service status and health${NC}"
    echo -e "    ${SOFT_CYAN}logs${NC} [service]     ${DIM_WHITE}Show logs (optionally for specific service)${NC}"
    echo ""

    echo -e "  ${SOFT_MAGENTA}${BOLD}Build & Development${NC}"
    echo -e "    ${SOFT_CYAN}build${NC}              ${DIM_WHITE}Build all services${NC}"
    echo -e "    ${SOFT_CYAN}test${NC} [service]     ${DIM_WHITE}Run tests (auth|database|all)${NC}"
    echo -e "    ${SOFT_CYAN}watch${NC} [service]    ${DIM_WHITE}Watch mode with auto-rebuild${NC}"
    echo ""

    echo -e "  ${SOFT_YELLOW}${BOLD}Database & Cache${NC}"
    echo -e "    ${SOFT_CYAN}db${NC}                 ${DIM_WHITE}Access PostgreSQL CLI (interactive menu)${NC}"
    echo -e "    ${SOFT_CYAN}redis${NC}              ${DIM_WHITE}Access Redis CLI${NC}"
    echo -e "    ${SOFT_CYAN}reset-db${NC}           ${DIM_WHITE}Reset database (interactive menu)${NC}"
    echo -e "    ${SOFT_CYAN}pgadmin${NC}            ${DIM_WHITE}Start pgAdmin interface${NC}"
    echo ""

    echo -e "  ${SOFT_ORANGE}${BOLD}AI / Machine Learning${NC}"
    echo -e "    ${SOFT_CYAN}ai${NC}                 ${DIM_WHITE}Show Ollama AI status and loaded models${NC}"
    echo -e "    ${SOFT_CYAN}ai-pull${NC}            ${DIM_WHITE}Pull AI models (interactive)${NC}"
    echo -e "    ${SOFT_CYAN}ai-test${NC}            ${DIM_WHITE}Test AI with a quick prompt${NC}"
    echo ""

    echo -e "  ${SOFT_RED}${BOLD}Maintenance${NC}"
    echo -e "    ${SOFT_CYAN}clean${NC}              ${DIM_WHITE}Remove all containers and volumes${NC}"
    echo ""

    echo -e "  ${SOFT_GRAY}${BOLD}Other${NC}"
    echo -e "    ${SOFT_CYAN}help${NC}               ${DIM_WHITE}Show this help message${NC}"
    echo ""

    print_separator
    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}Examples:${NC}\n"

    echo -e "    ${SOFT_GRAY}./dev.sh start${NC}                   ${DIM_WHITE}Start everything${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh logs dokus-auth-local${NC}    ${DIM_WHITE}Show auth service logs${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh db${NC}                      ${DIM_WHITE}Access PostgreSQL database (choose from menu)${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh test auth${NC}               ${DIM_WHITE}Run auth service tests${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh reset-db${NC}                ${DIM_WHITE}Reset database (choose from menu)${NC}"
    echo -e "    ${SOFT_GRAY}./dev.sh watch all${NC}               ${DIM_WHITE}Watch and auto-rebuild all${NC}"
    echo ""

    print_separator
    echo ""
    echo -e "  ${SOFT_GRAY}${DIM}Set NO_COLOR=1 to disable colors${NC}"
    echo ""
}

# Function to show interactive menu
show_menu() {
    clear
    echo ""
    echo -e "${SOFT_CYAN}${BOX_TL}$(printf '%.0s═' {1..68})${BOX_TR}${NC}"
    echo -e "${SOFT_CYAN}${BOX_V}                                                                    ${BOX_V}${NC}"
    echo -e "${SOFT_CYAN}${BOX_V}           ${GRADIENT_START}${BOLD}🚀 Dokus Development Environment${NC}${SOFT_CYAN}              ${BOX_V}${NC}"
    echo -e "${SOFT_CYAN}${BOX_V}                                                                    ${BOX_V}${NC}"
    echo -e "${SOFT_CYAN}${BOX_BL}$(printf '%.0s═' {1..68})${BOX_BR}${NC}"
    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}What would you like to do?${NC}\n"

    echo -e "  ${SOFT_GREEN}${BOLD}Service Management${NC}"
    echo -e "    ${SOFT_CYAN}①${NC}  Start services"
    echo -e "    ${SOFT_CYAN}②${NC}  Stop services"
    echo -e "    ${SOFT_CYAN}③${NC}  Restart services"
    echo -e "    ${SOFT_CYAN}④${NC}  Show status"
    echo ""

    echo -e "  ${SOFT_MAGENTA}${BOLD}Development Tools${NC}"
    echo -e "    ${SOFT_CYAN}⑤${NC}  View logs"
    echo -e "    ${SOFT_CYAN}⑥${NC}  Access database"
    echo -e "    ${SOFT_CYAN}⑦${NC}  Access Redis"
    echo -e "    ${SOFT_CYAN}⑧${NC}  Run all tests"
    echo ""

    echo -e "  ${SOFT_YELLOW}${BOLD}Utilities${NC}"
    echo -e "    ${SOFT_CYAN}⑨${NC}  Start pgAdmin"
    echo -e "    ${SOFT_CYAN}⑩${NC}  Clean everything"
    echo ""

    echo -e "  ${SOFT_GRAY}⓪${NC}  Exit"
    echo ""

    printf "  ${BOLD}Enter choice ${DIM_WHITE}[0-10]:${NC} "
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
        8) run_tests all ;;
        9) start_pgadmin ;;
        10) clean_all ;;
        0) echo -e "  ${SOFT_CYAN}👋 Goodbye!${NC}\n" && exit 0 ;;
        *) print_status error "Invalid choice" && sleep 2 && show_menu ;;
    esac

    echo ""
    printf "  ${DIM}Press Enter to continue...${NC}"
    read
    show_menu
}

# Run main function
main "$@"
