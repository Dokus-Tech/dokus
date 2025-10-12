#!/bin/bash

# Dokus Development Environment Manager
# This script manages the local development environment for Dokus Services

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
WHITE='\033[1;37m'
GRAY='\033[0;90m'
BOLD='\033[1m'
DIM='\033[2m'
NC='\033[0m' # No Color

# Box drawing characters
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

# Configuration
PROJECT_NAME="dokus"
COMPOSE_FILE="docker-compose.dev.yml"
AUTH_SERVICE_DIR="features/auth/backend"
DATABASE_SERVICE_DIR="foundation/database"
DB_NAME="dokus"
DB_USER="dev"
DB_PASSWORD="devpassword"

# Function to print colored output
print_color() {
    color=$1
    message=$2
    echo -e "${color}${message}${NC}"
}

# Function to print a box header
print_box_header() {
    local title=$1
    local width=${2:-60}
    local padding=$(( (width - ${#title} - 2) / 2 ))
    local padding_right=$(( width - ${#title} - 2 - padding ))

    echo ""
    echo -e "${CYAN}${BOX_TL}$(printf '%*s' $width | tr ' ' ${BOX_H})${BOX_TR}${NC}"
    echo -e "${CYAN}${BOX_V}$(printf '%*s' $padding)${WHITE}${BOLD}${title}${NC}${CYAN}$(printf '%*s' $padding_right)${BOX_V}${NC}"
    echo -e "${CYAN}${BOX_BL}$(printf '%*s' $width | tr ' ' ${BOX_H})${BOX_BR}${NC}"
    echo ""
}

# Function to print a section divider
print_divider() {
    local char=${1:-─}
    local width=${2:-60}
    echo -e "${GRAY}$(printf '%*s' $width | tr ' ' $char)${NC}"
}

# Function to print a spinner
spinner() {
    local pid=$1
    local delay=0.1
    local spinstr='⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏'
    while [ "$(ps a | awk '{print $1}' | grep $pid)" ]; do
        local temp=${spinstr#?}
        printf " [%c]  " "$spinstr"
        local spinstr=$temp${spinstr%"$temp"}
        sleep $delay
        printf "\b\b\b\b\b\b"
    done
    printf "    \b\b\b\b"
}

# Function to print status with icon
print_status() {
    local status=$1
    local message=$2
    case $status in
        success)
            echo -e "  ${GREEN}✔${NC} ${message}"
            ;;
        error)
            echo -e "  ${RED}✖${NC} ${message}"
            ;;
        warning)
            echo -e "  ${YELLOW}⚠${NC} ${message}"
            ;;
        info)
            echo -e "  ${CYAN}ℹ${NC} ${message}"
            ;;
        loading)
            echo -e "  ${YELLOW}⟳${NC} ${message}"
            ;;
        *)
            echo -e "  ${message}"
            ;;
    esac
}

# Function to check if Docker is running
check_docker() {
    if ! docker info > /dev/null 2>&1; then
        print_status error "Docker is not running. Please start Docker Desktop first."
        exit 1
    fi
    print_status success "Docker is running"
}

# Function to check if required tools are installed
check_requirements() {
    print_box_header "🔍 System Requirements Check"

    check_docker

    if ! command -v docker-compose &> /dev/null; then
        print_status error "docker-compose is not installed"
        exit 1
    fi
    print_status success "docker-compose is installed"

    if ! command -v gradle &> /dev/null && ! [ -f "./gradlew" ]; then
        print_status warning "Gradle is not installed, using gradlew"
    else
        print_status success "Gradle is available"
    fi

    echo ""
    print_status success "All requirements met"
    echo ""
}

# Function to build the application
build_app() {
    print_box_header "🔨 Building Applications"

    # Build Auth Service JAR
    print_status loading "Building Auth Service JAR..."
    if [ -f "./gradlew" ]; then
        ./gradlew :features:auth:backend:shadowJar -x test -q
    else
        gradle :features:auth:backend:shadowJar -x test -q
    fi

    if [ $? -ne 0 ]; then
        print_status error "Auth Service JAR build failed"
        exit 1
    fi
    print_status success "Auth Service JAR built"

    # Build Database Service JAR
    print_status loading "Building Database Service JAR..."
    if [ -f "./gradlew" ]; then
        ./gradlew :foundation:database:shadowJar -x test -q
    else
        gradle :foundation:database:shadowJar -x test -q
    fi

    if [ $? -ne 0 ]; then
        print_status error "Database Service JAR build failed"
        exit 1
    fi
    print_status success "Database Service JAR built"

    print_divider
    echo ""
    print_status loading "Building Docker images..."
    echo ""

    # Auth Service image
    print_status loading "Building Auth Service image..."
    docker build -f features/auth/backend/Dockerfile.dev -t invoid-vision/dokus-auth:dev-latest . -q > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        print_status error "Auth Service Docker image build failed"
        exit 1
    fi
    print_status success "Auth Service image built"

    # Database Service image
    print_status loading "Building Database Service image..."
    docker build -f foundation/database/Dockerfile.dev -t invoid-vision/dokus-database:dev-latest . -q > /dev/null 2>&1
    if [ $? -ne 0 ]; then
        print_status error "Database Service Docker image build failed"
        exit 1
    fi
    print_status success "Database Service image built"

    echo ""
}

# Function to start services
start_services() {
    print_box_header "🚀 Starting Services"

    # Create logs directory if it doesn't exist
    mkdir -p logs

    # Start services
    print_status loading "Starting Docker containers..."
    docker-compose -f $COMPOSE_FILE up -d > /dev/null 2>&1

    if [ $? -eq 0 ]; then
        print_status success "Containers started"

        # Wait for services to be healthy
        echo ""
        print_status loading "Waiting for services to become healthy..."
        echo ""

        # Wait for PostgreSQL
        printf "  ${CYAN}▸${NC} PostgreSQL          "
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T postgres-dev pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
                echo -e "${GREEN}✔ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Redis
        printf "  ${CYAN}▸${NC} Redis               "
        for i in {1..30}; do
            if docker-compose -f $COMPOSE_FILE exec -T redis-dev redis-cli --pass devredispass ping &>/dev/null; then
                echo -e "${GREEN}✔ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Auth Service
        sleep 3  # Give it a moment to start
        printf "  ${CYAN}▸${NC} Auth Service        "
        for i in {1..30}; do
            if curl -f -s http://localhost:9093/metrics > /dev/null 2>&1; then
                echo -e "${GREEN}✔ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        # Wait for Database Service
        printf "  ${CYAN}▸${NC} Database Service    "
        for i in {1..30}; do
            if curl -f -s http://localhost:9071/metrics > /dev/null 2>&1; then
                echo -e "${GREEN}✔ Ready${NC}"
                break
            fi
            echo -n "."
            sleep 1
        done

        echo ""
        print_status success "All services are running!"
        echo ""
        print_services_info
    else
        print_status error "Failed to start services"
        exit 1
    fi
}

# Function to stop services
stop_services() {
    print_box_header "🛑 Stopping Services"
    docker-compose -f $COMPOSE_FILE down
    print_status success "Services stopped"
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
    print_box_header "📊 Service Status"

    docker-compose -f $COMPOSE_FILE ps
    echo ""

    # Check health endpoints
    print_divider
    echo -e "\n  ${CYAN}${BOLD}Health Checks${NC}\n"

    # PostgreSQL
    printf "  ${CYAN}▸${NC} PostgreSQL          "
    if docker-compose -f $COMPOSE_FILE exec -T postgres-dev pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo -e "${GREEN}✔ Healthy${NC}"
    else
        echo -e "${RED}✖ Not responding${NC}"
    fi

    # Redis
    printf "  ${CYAN}▸${NC} Redis               "
    if docker-compose -f $COMPOSE_FILE exec -T redis-dev redis-cli --pass devredispass ping &>/dev/null; then
        echo -e "${GREEN}✔ Healthy${NC}"
    else
        echo -e "${RED}✖ Not responding${NC}"
    fi

    # Auth Service
    printf "  ${CYAN}▸${NC} Auth Service        "
    if curl -f -s http://localhost:9093/metrics > /dev/null 2>&1; then
        echo -e "${GREEN}✔ Healthy${NC}"
    else
        echo -e "${RED}✖ Not responding${NC}"
    fi

    # Database Service
    printf "  ${CYAN}▸${NC} Database Service    "
    if curl -f -s http://localhost:9071/metrics > /dev/null 2>&1; then
        echo -e "${GREEN}✔ Healthy${NC}"
    else
        echo -e "${RED}✖ Not responding${NC}"
    fi

    echo ""
}

# Function to clean everything
clean_all() {
    print_box_header "🧹 Clean Everything"

    print_status warning "This will remove all containers, volumes, and data!"
    echo ""
    printf "  ${BOLD}Are you sure? (y/N):${NC} "
    read -n 1 -r
    echo ""
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status loading "Removing containers and volumes..."
        docker-compose -f $COMPOSE_FILE down -v
        rm -rf logs/*
        print_status success "Cleanup complete"
    else
        print_status info "Cancelled"
    fi
    echo ""
}

# Function to reset database
reset_db() {
    print_box_header "🔄 Reset Database"
    print_status warning "This will reset the database!"

    echo ""
    printf "  ${BOLD}Are you sure? (y/N):${NC} "
    read -n 1 -r
    echo ""
    echo ""

    if [[ $REPLY =~ ^[Yy]$ ]]; then
        print_status loading "Resetting database..."
        docker-compose -f $COMPOSE_FILE stop postgres-dev > /dev/null 2>&1
        docker-compose -f $COMPOSE_FILE rm -f postgres-dev > /dev/null 2>&1
        docker volume rm dokus_postgres-dev 2>/dev/null || true
        docker-compose -f $COMPOSE_FILE up -d postgres-dev > /dev/null 2>&1
        print_status success "Database reset complete"
    else
        print_status info "Cancelled"
    fi
    echo ""
}

# Function to access database
access_db() {
    print_box_header "🗄️ Accessing PostgreSQL"
    docker-compose -f $COMPOSE_FILE exec postgres-dev psql -U $DB_USER -d $DB_NAME
}

# Function to access Redis
access_redis() {
    print_box_header "🗄️ Accessing Redis"
    docker-compose -f $COMPOSE_FILE exec redis-dev redis-cli -a devredispass
}

# Function to run tests
run_tests() {
    service=${1:-all}

    if [ "$service" = "all" ]; then
        print_box_header "🧪 Running All Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:auth:backend:test :foundation:database:test
        else
            gradle :features:auth:backend:test :foundation:database:test
        fi
    elif [ "$service" = "auth" ]; then
        print_box_header "🧪 Running Auth Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :features:auth:backend:test
        else
            gradle :features:auth:backend:test
        fi
    elif [ "$service" = "database" ]; then
        print_box_header "🧪 Running Database Service Tests"
        if [ -f "./gradlew" ]; then
            ./gradlew :foundation:database:test
        else
            gradle :foundation:database:test
        fi
    else
        print_status error "Invalid service type. Use 'all', 'auth', or 'database'"
        exit 1
    fi
    echo ""
}

# Function to print service information
print_services_info() {
    print_divider "─"
    echo ""
    echo -e "  ${CYAN}${BOLD}📍 Service Endpoints${NC}\n"

    # Auth Service
    echo -e "  ${MAGENTA}▸ Auth Service${NC}"
    echo -e "    ${GRAY}•${NC} API:        ${WHITE}http://localhost:9093${NC}"
    echo -e "    ${GRAY}•${NC} Metrics:    ${WHITE}http://localhost:9093/metrics${NC}"
    echo -e "    ${GRAY}•${NC} Health:     ${WHITE}http://localhost:9093/health${NC}"
    echo -e "    ${GRAY}•${NC} Debug:      ${WHITE}localhost:5007${NC}"
    echo ""

    # Database Service
    echo -e "  ${MAGENTA}▸ Database Service${NC}"
    echo -e "    ${GRAY}•${NC} API:        ${WHITE}http://localhost:9071${NC}"
    echo -e "    ${GRAY}•${NC} Metrics:    ${WHITE}http://localhost:9071/metrics${NC}"
    echo -e "    ${GRAY}•${NC} Health:     ${WHITE}http://localhost:9071/health${NC}"
    echo -e "    ${GRAY}•${NC} Debug:      ${WHITE}localhost:5008${NC}"
    echo ""

    # Databases
    echo -e "  ${MAGENTA}▸ Infrastructure${NC}"
    echo -e "    ${GRAY}•${NC} PostgreSQL: ${WHITE}localhost:5543${NC} ${DIM}(user: $DB_USER, db: $DB_NAME)${NC}"
    echo -e "    ${GRAY}•${NC} Redis:      ${WHITE}localhost:6380${NC} ${DIM}(password: devredispass)${NC}"

    if docker-compose -f $COMPOSE_FILE ps | grep -q pgadmin; then
        echo -e "    ${GRAY}•${NC} pgAdmin:    ${WHITE}http://localhost:5050${NC} ${DIM}(admin@dokus.ai / admin)${NC}"
    fi

    echo ""
    print_divider "─"
    echo ""
    echo -e "  ${CYAN}${BOLD}🔧 Quick Commands${NC}\n"
    echo -e "    ${GRAY}./dev.sh logs${NC}         View all logs"
    echo -e "    ${GRAY}./dev.sh db${NC}           Access PostgreSQL database"
    echo -e "    ${GRAY}./dev.sh redis${NC}        Access Redis CLI"
    echo -e "    ${GRAY}./dev.sh status${NC}       Check service health"
    echo -e "    ${GRAY}./dev.sh test${NC}         Run all tests"
    echo ""
}

# Function to start pgAdmin
start_pgadmin() {
    print_box_header "🗄️ Starting pgAdmin"
    docker-compose -f $COMPOSE_FILE --profile tools up -d pgadmin
    echo ""
    print_status success "pgAdmin started at http://localhost:5050"
    print_status info "Login: admin@dokus.ai / admin"
    echo ""
}

# Function to watch for changes and rebuild
watch_mode() {
    service=${1:-all}

    print_color "$BLUE" "👁️  Watch mode - rebuilding on changes..."
    print_color "$YELLOW" "Press Ctrl+C to stop"

    # Initial build and start
    build_app
    restart_services

    # Watch for changes (requires fswatch or inotify-tools)
    if command -v fswatch &> /dev/null; then
        if [ "$service" = "all" ]; then
            fswatch -o $AUTH_SERVICE_DIR/src $DATABASE_SERVICE_DIR/src | while read num ; do
                print_color "$YELLOW" "🔄 Changes detected, rebuilding..."
                build_app
                docker-compose -f $COMPOSE_FILE restart auth-service-dev database-service-dev
                print_color "$GREEN" "✓ Services restarted"
            done
        elif [ "$service" = "auth" ]; then
            fswatch -o $AUTH_SERVICE_DIR/src | while read num ; do
                print_color "$YELLOW" "🔄 Auth Service changes detected, rebuilding..."
                if [ -f "./gradlew" ]; then
                    ./gradlew :features:auth:backend:shadowJar -x test
                else
                    gradle :features:auth:backend:shadowJar -x test
                fi
                docker build -f features/auth/backend/Dockerfile.dev -t invoid-vision/dokus-auth:dev-latest .
                docker-compose -f $COMPOSE_FILE restart auth-service-dev
                print_color "$GREEN" "✓ Auth Service restarted"
            done
        elif [ "$service" = "database" ]; then
            fswatch -o $DATABASE_SERVICE_DIR/src | while read num ; do
                print_color "$YELLOW" "🔄 Database Service changes detected, rebuilding..."
                if [ -f "./gradlew" ]; then
                    ./gradlew :foundation:database:shadowJar -x test
                else
                    gradle :foundation:database:shadowJar -x test
                fi
                docker build -f foundation/database/Dockerfile.dev -t invoid-vision/dokus-database:dev-latest .
                docker-compose -f $COMPOSE_FILE restart database-service-dev
                print_color "$GREEN" "✓ Database Service restarted"
            done
        fi
    else
        print_color "$YELLOW" "⚠️  fswatch not installed. Install it for file watching:"
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
    print_box_header "🚀 Dokus Development Environment Manager"

    echo -e "  ${BOLD}Usage:${NC} ./dev.sh [command] [options]\n"

    print_divider
    echo ""
    echo -e "  ${CYAN}${BOLD}Commands:${NC}\n"

    echo -e "  ${GREEN}Service Management${NC}"
    echo -e "    ${WHITE}start${NC}              Build and start all services"
    echo -e "    ${WHITE}stop${NC}               Stop all services"
    echo -e "    ${WHITE}restart${NC}            Restart all services"
    echo -e "    ${WHITE}status${NC}             Show service status and health"
    echo -e "    ${WHITE}logs${NC} [service]     Show logs (optionally for specific service)"
    echo ""

    echo -e "  ${MAGENTA}Build & Development${NC}"
    echo -e "    ${WHITE}build${NC}              Build all services"
    echo -e "    ${WHITE}test${NC} [service]     Run tests (auth|database|all)"
    echo -e "    ${WHITE}watch${NC} [service]    Watch mode with auto-rebuild"
    echo ""

    echo -e "  ${YELLOW}Database & Cache${NC}"
    echo -e "    ${WHITE}db${NC}                 Access PostgreSQL CLI"
    echo -e "    ${WHITE}redis${NC}              Access Redis CLI"
    echo -e "    ${WHITE}reset-db${NC}           Reset database"
    echo -e "    ${WHITE}pgadmin${NC}            Start pgAdmin interface"
    echo ""

    echo -e "  ${RED}Maintenance${NC}"
    echo -e "    ${WHITE}clean${NC}              Remove all containers and volumes"
    echo ""

    echo -e "  ${GRAY}Other${NC}"
    echo -e "    ${WHITE}help${NC}               Show this help message"
    echo ""

    print_divider
    echo ""
    echo -e "  ${CYAN}${BOLD}Examples:${NC}\n"

    echo -e "    ${GRAY}./dev.sh start${NC}                   Start everything"
    echo -e "    ${GRAY}./dev.sh logs auth-service-dev${NC}   Show auth service logs"
    echo -e "    ${GRAY}./dev.sh db${NC}                      Access PostgreSQL database"
    echo -e "    ${GRAY}./dev.sh test auth${NC}               Run auth service tests"
    echo -e "    ${GRAY}./dev.sh reset-db${NC}                Reset database"
    echo -e "    ${GRAY}./dev.sh watch all${NC}               Watch and auto-rebuild all"
    echo ""
}

# Function to show interactive menu
show_menu() {
    clear
    echo ""
    echo -e "${CYAN}${BOX_TL}$(printf '%.0s═' {1..58})${BOX_TR}${NC}"
    echo -e "${CYAN}${BOX_V}                                                          ${BOX_V}${NC}"
    echo -e "${CYAN}${BOX_V}        ${WHITE}${BOLD}🚀 Dokus Development Environment${NC}${CYAN}           ${BOX_V}${NC}"
    echo -e "${CYAN}${BOX_V}                                                          ${BOX_V}${NC}"
    echo -e "${CYAN}${BOX_BL}$(printf '%.0s═' {1..58})${BOX_BR}${NC}"
    echo ""
    echo -e "  ${CYAN}${BOLD}What would you like to do?${NC}\n"

    echo -e "  ${GREEN}Service Management${NC}"
    echo -e "    ${CYAN}1${NC})  Start services"
    echo -e "    ${CYAN}2${NC})  Stop services"
    echo -e "    ${CYAN}3${NC})  Restart services"
    echo -e "    ${CYAN}4${NC})  Show status"
    echo ""

    echo -e "  ${MAGENTA}Development Tools${NC}"
    echo -e "    ${CYAN}5${NC})  View logs"
    echo -e "    ${CYAN}6${NC})  Access database"
    echo -e "    ${CYAN}7${NC})  Access Redis"
    echo -e "    ${CYAN}8${NC})  Run all tests"
    echo ""

    echo -e "  ${YELLOW}Utilities${NC}"
    echo -e "    ${CYAN}9${NC})  Start pgAdmin"
    echo -e "    ${CYAN}10${NC}) Clean everything"
    echo ""

    echo -e "  ${GRAY}0${NC})  Exit"
    echo ""

    printf "  ${BOLD}Enter choice [0-10]:${NC} "
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
        0) echo -e "${CYAN}👋 Goodbye!${NC}" && exit 0 ;;
        *) print_status error "Invalid choice" && sleep 2 && show_menu ;;
    esac

    echo ""
    printf "${DIM}Press Enter to continue...${NC}"
    read
    show_menu
}

# Run main function
main "$@"