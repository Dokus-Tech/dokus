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

# Database configuration for multi-database architecture
# Format: container:port:dbname
# Bash 3.2 compatible (no associative arrays)

DB_KEYS="auth invoicing expense payment reporting audit banking"

# Function to get database config for a given key
# Returns: container:port:dbname
get_db_config() {
    local key=$1
    case $key in
        auth)      echo "postgres-auth-dev:5541:dokus_auth" ;;
        invoicing) echo "postgres-invoicing-dev:5542:dokus_invoicing" ;;
        expense)   echo "postgres-expense-dev:5543:dokus_expense" ;;
        payment)   echo "postgres-payment-dev:5544:dokus_payment" ;;
        reporting) echo "postgres-reporting-dev:5545:dokus_reporting" ;;
        audit)     echo "postgres-audit-dev:5546:dokus_audit" ;;
        banking)   echo "postgres-banking-dev:5547:dokus_banking" ;;
        *) echo "" ;;
    esac
}

DB_USER="dev"
DB_PASSWORD="devpassword"

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

    local services=("auth" "audit" "banking" "invoicing" "expense" "payment" "reporting")
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

        # Wait for all PostgreSQL databases
        for db_key in $DB_KEYS; do
            IFS=':' read -r container port dbname <<< "$(get_db_config $db_key)"
            printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "PostgreSQL ($db_key)"
            for i in {1..30}; do
                if docker-compose -f $COMPOSE_FILE exec -T $container pg_isready -U $DB_USER -d $dbname &>/dev/null; then
                    echo -e "${SOFT_GREEN}◆ Ready${NC}"
                    break
                fi
                if [ $i -eq 30 ]; then
                    echo -e "${SOFT_RED}◇ Timeout${NC}"
                fi
                echo -n "."
                sleep 1
            done
        done

        # Wait for Redis
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "Redis Cache"
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T redis-dev redis-cli --pass devredispass ping &>/dev/null; then
                echo -e "${SOFT_GREEN}◆ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for RabbitMQ
        printf "  ${SOFT_CYAN}${TREE_BRANCH}${TREE_RIGHT}${NC} %-22s" "RabbitMQ Broker"
        for i in {1..30}; do
            if curl -f -s -u dokus:devrabbitpass http://localhost:15672/api/health/checks/alarms &>/dev/null; then
                echo -e "${SOFT_GREEN}◆ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for services with proper spacing
        sleep 3

        local services=(
            "Auth:7091:/metrics"
            "Audit:7096:/health"
            "Banking:7097:/health"
            "Invoicing:7092:/health"
            "Expense:7093:/health"
            "Payment:7094:/health"
            "Reporting:7095:/health"
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

    # Check all PostgreSQL databases
    for db_key in $DB_KEYS; do
        IFS=':' read -r container port dbname <<< "$(get_db_config $db_key)"
        printf "  ${SOFT_GRAY}│${NC} PostgreSQL (%-9s) ${SOFT_GRAY}│${NC} " "$db_key"
        if docker-compose -f $COMPOSE_FILE exec -T $container pg_isready -U $DB_USER -d $dbname &>/dev/null; then
            echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
        else
            echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
        fi
    done

    # Redis
    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker-compose -f $COMPOSE_FILE exec -T redis-dev redis-cli --pass devredispass ping &>/dev/null; then
        echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    # RabbitMQ
    printf "  ${SOFT_GRAY}│${NC} RabbitMQ Broker         ${SOFT_GRAY}│${NC} "
    if curl -f -s -u dokus:devrabbitpass http://localhost:15672/api/health/checks/alarms &>/dev/null; then
        echo -e "${SOFT_GREEN}◆ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo -e "${SOFT_RED}◇ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo -e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Services
    local services=(
        "Auth Service:7091:/metrics"
        "Audit Service:7096:/health"
        "Banking Service:7097:/health"
        "Invoicing Service:7092:/health"
        "Expense Service:7093:/health"
        "Payment Service:7094:/health"
        "Reporting Service:7095:/health"
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

    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}Select database to reset:${NC}\n"

    local options=(
        "① Auth (dokus_auth)"
        "② Invoicing (dokus_invoicing)"
        "③ Expense (dokus_expense)"
        "④ Payment (dokus_payment)"
        "⑤ Reporting (dokus_reporting)"
        "⑥ Audit (dokus_audit)"
        "⑦ Banking (dokus_banking)"
        "⑧ All databases"
    )

    for option in "${options[@]}"; do
        echo -e "  ${SOFT_CYAN}${option}${NC}"
    done
    echo ""
    echo -e "  ${SOFT_GRAY}⓪ Cancel${NC}"
    echo ""
    printf "  ${BOLD}Enter choice ${DIM_WHITE}[0-8]:${NC} "
    read choice
    echo ""

    case $choice in
        1) reset_single_db "auth" ;;
        2) reset_single_db "invoicing" ;;
        3) reset_single_db "expense" ;;
        4) reset_single_db "payment" ;;
        5) reset_single_db "reporting" ;;
        6) reset_single_db "audit" ;;
        7) reset_single_db "banking" ;;
        8) reset_all_databases ;;
        0) print_status info "Operation cancelled"; echo ""; return ;;
        *) print_status error "Invalid choice"; echo ""; return ;;
    esac
}

# Helper function to reset a single database
reset_single_db() {
    local db_key=$1
    IFS=':' read -r container port dbname <<< "$(get_db_config $db_key)"

    print_status warning "This will reset the $db_key database ($dbname)!"
    echo ""
    printf "  ${BOLD}${SOFT_RED}Are you sure?${NC} ${DIM_WHITE}(y/N):${NC} "
    read -n 1 -r
    echo ""
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status loading "Resetting $db_key database..."
        docker-compose -f $COMPOSE_FILE stop $container > /dev/null 2>&1
        docker-compose -f $COMPOSE_FILE rm -f $container > /dev/null 2>&1
        docker volume rm dokus_$container 2>/dev/null || true
        docker-compose -f $COMPOSE_FILE up -d $container > /dev/null 2>&1
        echo ""
        print_status success "$db_key database reset complete"
    else
        print_status info "Operation cancelled"
    fi
    echo ""
}

# Helper function to reset all databases
reset_all_databases() {
    print_status warning "This will reset ALL databases!"
    echo ""
    printf "  ${BOLD}${SOFT_RED}Are you sure?${NC} ${DIM_WHITE}(y/N):${NC} "
    read -n 1 -r
    echo ""
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status loading "Resetting all databases..."
        echo ""

        for db_key in $DB_KEYS; do
            IFS=':' read -r container port dbname <<< "$(get_db_config $db_key)"
            print_simple_status building "Resetting $db_key..."
            docker-compose -f $COMPOSE_FILE stop $container > /dev/null 2>&1
            docker-compose -f $COMPOSE_FILE rm -f $container > /dev/null 2>&1
            docker volume rm dokus_$container 2>/dev/null || true
        done

        echo ""
        print_status loading "Starting all databases..."
        for db_key in $DB_KEYS; do
            IFS=':' read -r container port dbname <<< "$(get_db_config $db_key)"
            docker-compose -f $COMPOSE_FILE up -d $container > /dev/null 2>&1
        done

        echo ""
        print_status success "All databases reset complete"
    else
        print_status info "Operation cancelled"
    fi
    echo ""
}

# Function to access database
access_db() {
    print_gradient_header "🗄️  Database CLI Access"

    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}Select database to access:${NC}\n"

    local options=(
        "① Auth (dokus_auth) - localhost:5541"
        "② Invoicing (dokus_invoicing) - localhost:5542"
        "③ Expense (dokus_expense) - localhost:5543"
        "④ Payment (dokus_payment) - localhost:5544"
        "⑤ Reporting (dokus_reporting) - localhost:5545"
        "⑥ Audit (dokus_audit) - localhost:5546"
        "⑦ Banking (dokus_banking) - localhost:5547"
    )

    for option in "${options[@]}"; do
        echo -e "  ${SOFT_CYAN}${option}${NC}"
    done
    echo ""
    echo -e "  ${SOFT_GRAY}⓪ Cancel${NC}"
    echo ""
    printf "  ${BOLD}Enter choice ${DIM_WHITE}[0-7]:${NC} "
    read choice
    echo ""

    case $choice in
        1) access_single_db "auth" ;;
        2) access_single_db "invoicing" ;;
        3) access_single_db "expense" ;;
        4) access_single_db "payment" ;;
        5) access_single_db "reporting" ;;
        6) access_single_db "audit" ;;
        7) access_single_db "banking" ;;
        0) print_status info "Operation cancelled"; echo ""; return ;;
        *) print_status error "Invalid choice"; echo ""; return ;;
    esac
}

# Helper function to access a single database
access_single_db() {
    local db_key=$1
    IFS=':' read -r container port dbname <<< "$(get_db_config $db_key)"

    print_status info "Connecting to $db_key database ($dbname)..."
    echo ""
    docker-compose -f $COMPOSE_FILE exec $container psql -U $DB_USER -d $dbname
}

# Function to access Redis
access_redis() {
    print_gradient_header "🗄️  Redis CLI Access"
    docker-compose -f $COMPOSE_FILE exec redis-dev redis-cli -a devredispass
}

# Function to run tests
run_tests() {
    service=${1:-all}

    if [ "$service" = "all" ]; then
        print_gradient_header "🧪 Running All Test Suites"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:auth:backend:test :features:audit:backend:test :features:banking:backend:test
        else
            gradle :features:auth:backend:test :features:audit:backend:test :features:banking:backend:test
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
    else
        print_status error "Invalid service type. Use 'all', 'auth', 'audit', or 'banking'"
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
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/metrics /health${NC} • ${SOFT_GRAY}debug: 5007${NC}    ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Invoicing Service${NC}    ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7092${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 5009${NC}                ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Expense Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7093${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 5010${NC}                ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Payment Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7094${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 5011${NC}                ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Reporting Service${NC}    ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7095${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 5012${NC}                ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Audit Service${NC}        ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7096${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 5013${NC}                ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}Banking Service${NC}      ${SOFT_GRAY}│${NC} ${DIM_WHITE}http://localhost:7097${NC}               ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}/health${NC} • ${SOFT_GRAY}debug: 5014${NC}                ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}└──────────────────────┴─────────────────────────────────────────┘${NC}"

    echo ""
    echo -e "  ${SOFT_CYAN}${BOLD}💾 Database Connections${NC}\n"

    # Database table
    echo -e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${BOLD}Database${NC}             ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Auth${NC}                 ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5541${NC} • ${SOFT_GRAY}dokus_auth${NC}         ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Invoicing${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5542${NC} • ${SOFT_GRAY}dokus_invoicing${NC}    ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Expense${NC}              ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5543${NC} • ${SOFT_GRAY}dokus_expense${NC}      ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Payment${NC}              ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5544${NC} • ${SOFT_GRAY}dokus_payment${NC}      ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Reporting${NC}            ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5545${NC} • ${SOFT_GRAY}dokus_reporting${NC}    ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Audit${NC}                ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5546${NC} • ${SOFT_GRAY}dokus_audit${NC}        ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Banking${NC}              ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5547${NC} • ${SOFT_GRAY}dokus_banking${NC}      ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:6380${NC} • ${SOFT_GRAY}pass: devredispass${NC} ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC} ${SOFT_MAGENTA}RabbitMQ${NC}             ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:5672${NC} • ${SOFT_GRAY}user: dokus${NC}        ${SOFT_GRAY}│${NC}"
    echo -e "  ${SOFT_GRAY}│${NC}                      ${SOFT_GRAY}│${NC} ${DIM_WHITE}UI: localhost:15672${NC} • ${SOFT_GRAY}pass: devrabbitpass${NC} ${SOFT_GRAY}│${NC}"
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
                docker-compose -f $COMPOSE_FILE restart auth-service-dev audit-service-dev banking-service-dev
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
                docker-compose -f $COMPOSE_FILE restart auth-service-dev
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
                docker-compose -f $COMPOSE_FILE restart audit-service-dev
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
                docker-compose -f $COMPOSE_FILE restart banking-service-dev
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
    echo -e "    ${SOFT_GRAY}./dev.sh logs auth-service-dev${NC}   ${DIM_WHITE}Show auth service logs${NC}"
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