#!/bin/bash
#
# Dokus Cloud Management Script — Neon Onboarding Edition
# Supports: macOS, Linux
# Usage: ./dokus.sh [--profile=<pro|lite>] [command]
#
# Profiles:
#   pro  - High performance for Mac/servers (docker-compose.pro.yml)
#   lite - Low resource for Raspberry Pi/edge (docker-compose.lite.yml) [default]
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
DB_CONTAINER="postgres"
DB_PORT="15432"
DB_NAME="dokus"

# Profile configuration
# Available profiles:
#   - pro:  High performance for Mac/servers (docker-compose.pro.yml)
#   - lite: Low resource for Raspberry Pi/edge (docker-compose.lite.yml) [default]
PROFILE_FILE=".dokus-profile"
DEBUG_MODE_FILE=".dokus-debug"
DEBUG_MODE=false

# Load saved profile or prompt user to select
load_profile() {
    if [ -f "$PROFILE_FILE" ]; then
        DOKUS_PROFILE=$(cat "$PROFILE_FILE")
    elif [ -n "${DOKUS_PROFILE:-}" ]; then
        # Use environment variable if set
        :
    else
        # No saved profile - will prompt on first interactive use
        DOKUS_PROFILE=""
    fi

    set_compose_file
}

# Set compose file based on profile
set_compose_file() {
    case "${DOKUS_PROFILE:-}" in
        cloud)
            COMPOSE_FILE="docker-compose.cloud.yml"
            ;;
        pro)
            COMPOSE_FILE="docker-compose.pro.yml"
            ;;
        lite)
            COMPOSE_FILE="docker-compose.lite.yml"
            ;;
        *)
            # Default to lite if no profile set yet
            COMPOSE_FILE="docker-compose.lite.yml"
            ;;
    esac
}

# Prompt user to select profile (first-run experience)
prompt_profile_selection() {
    print_gradient_header "Welcome to Dokus" "First-time setup - choose your profile"

    echo_e "  ${SOFT_CYAN}Select a deployment profile:${NC}"
    echo ""
    echo_e "  ${SOFT_MAGENTA}1${NC}   ${BOLD}Cloud${NC} ${DIM_WHITE}(Production HTTPS)${NC}"
    echo_e "      ${DIM_WHITE}HTTPS with Let's Encrypt, domain-based routing${NC}"
    echo_e "      ${DIM_WHITE}Best for: VPS, cloud servers with public domain${NC}"
    echo ""
    echo_e "  ${SOFT_GREEN}2${NC}   ${BOLD}Pro${NC} ${DIM_WHITE}(Self-Host High Performance)${NC}"
    echo_e "      ${DIM_WHITE}HTTP:8000, G1GC, 1GB heap, more connections${NC}"
    echo_e "      ${DIM_WHITE}Best for: Mac (Apple Silicon), Linux servers${NC}"
    echo ""
    echo_e "  ${SOFT_YELLOW}3${NC}   ${BOLD}Lite${NC} ${DIM_WHITE}(Self-Host Low Resource)${NC}"
    echo_e "      ${DIM_WHITE}HTTP:8000, SerialGC, 256MB heap, minimal memory${NC}"
    echo_e "      ${DIM_WHITE}Best for: Raspberry Pi 4/5, low-end devices${NC}"
    echo ""

    printf "  ${BOLD}Select profile ${DIM_WHITE}[1-3]:${NC} "
    read choice
    echo ""

    case $choice in
        1)
            DOKUS_PROFILE="cloud"
            ;;
        2)
            DOKUS_PROFILE="pro"
            ;;
        3|*)
            DOKUS_PROFILE="lite"
            ;;
    esac

    save_profile
    set_compose_file
    print_status success "Profile set to $(get_profile_display)"
    print_status info "Using: $COMPOSE_FILE"
    echo ""
}

# Save profile choice
save_profile() {
    echo "$DOKUS_PROFILE" > "$PROFILE_FILE"
}

# Get profile display name
get_profile_display() {
    case "${DOKUS_PROFILE:-lite}" in
        cloud)
            echo "Cloud (Production HTTPS)"
            ;;
        pro)
            echo "Pro (High Performance)"
            ;;
        *)
            echo "Lite (Low Resource)"
            ;;
    esac
}

# Load debug mode state
load_debug_mode() {
    if [ -f "$DEBUG_MODE_FILE" ]; then
        DEBUG_MODE=$(cat "$DEBUG_MODE_FILE")
    else
        DEBUG_MODE=false
    fi
}

# Save debug mode state
save_debug_mode() {
    echo "$DEBUG_MODE" > "$DEBUG_MODE_FILE"
}

# Toggle debug mode - restarts services with/without debug override
toggle_debug_mode() {
    if ! check_env; then
        return 1
    fi

    if [ "$DEBUG_MODE" = "true" ]; then
        print_gradient_header "Disabling Debug Mode" "Restarting without JDWP"

        print_status info "Stopping services..."
        docker compose -f "$COMPOSE_FILE" down

        print_status info "Starting services (normal mode)..."
        docker compose --compatibility -f "$COMPOSE_FILE" up -d

        DEBUG_MODE=false
        save_debug_mode

        print_status success "Debug mode disabled"
    else
        print_gradient_header "Enabling Debug Mode" "Restarting with JDWP on port 5005"

        print_status info "Stopping services..."
        docker compose -f "$COMPOSE_FILE" down

        print_status info "Starting services with debug override..."
        docker compose --compatibility -f "$COMPOSE_FILE" -f docker-compose.debug.yml up -d

        DEBUG_MODE=true
        save_debug_mode

        print_status success "Debug mode enabled"
        echo ""
        print_status info "JVM remote debugging available on port 5005"
        print_status info "Connect your debugger to: $(get_server_ip):5005"
    fi

    echo ""
    print_status info "Waiting for services to stabilize..."
    sleep 5
}

# Initialize profile
load_profile
load_debug_mode

# Gateway configuration (HTTP for self-hosting)
GATEWAY_PORT="8000"
GATEWAY_DASHBOARD_PORT="8080"
DEFAULT_DOMAIN="localhost"

# Cloud gateway domain (used for Host-based Traefik routing)
get_cloud_domain() {
    local domain="app.dokus.tech"
    if [ -f .env ]; then
        local line
        line=$(grep -E '^DOMAIN=' .env 2>/dev/null | head -n 1 || true)
        if [ -n "$line" ]; then
            domain="${line#DOMAIN=}"
        fi
    fi
    echo "$domain"
}

# Get HTTP status code via the gateway for a given endpoint.
# For cloud, Traefik routes by Host header, so we probe https://localhost with the domain header.
gateway_http_code() {
    local endpoint="$1"

    if [ "${DOKUS_PROFILE:-}" = "cloud" ]; then
        local domain
        domain="$(get_cloud_domain)"
        curl -s -o /dev/null -w "%{http_code}" -k -H "Host: ${domain}" "https://localhost${endpoint}" 2>/dev/null || echo "000"
    else
        curl -s -o /dev/null -w "%{http_code}" "http://localhost:${GATEWAY_PORT}${endpoint}" 2>/dev/null || echo "000"
    fi
}

gateway_is_reachable() {
    local endpoint="$1"
    local code
    code="$(gateway_http_code "$endpoint")"
    [ -n "$code" ] && [ "$code" != "000" ]
}

# Function to get server IP address
get_server_ip() {
    # First try to get public IP
    local PUBLIC_IP=$(curl -s --connect-timeout 2 https://api.ipify.org 2>/dev/null || \
                     curl -s --connect-timeout 2 https://ifconfig.me 2>/dev/null || \
                     echo "")

    if [ -n "$PUBLIC_IP" ]; then
        echo "$PUBLIC_IP"
        return
    fi

    # Fall back to local IP
    if [[ "$OSTYPE" == "darwin"* ]]; then
        ipconfig getifaddr en0 2>/dev/null || \
        ipconfig getifaddr en1 2>/dev/null || \
        echo "localhost"
    else
        hostname -I 2>/dev/null | awk '{print $1}' || echo "localhost"
    fi
}

# Credentials
if [ -f .env ]; then
    export $(grep -v '^#' .env | grep -E 'DB_PASSWORD|REDIS_PASSWORD|MINIO_PASSWORD|JWT_SECRET|DOMAIN|ACME_EMAIL' | xargs)
    DB_USER="dokus"
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
    echo_e "  ${DIM_WHITE}Synth-styled orchestrator for PostgreSQL, Redis, MinIO, Ollama, and services.${NC}\n"
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

    docker compose -f "$COMPOSE_FILE" ps
    echo ""

    print_separator
    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}Health Monitor${NC}\n"

    echo_e "  ${SOFT_GRAY}┌─────────────────────────┬──────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}                 ${SOFT_GRAY}│${NC} ${BOLD}Status${NC}           ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    printf "  ${SOFT_GRAY}│${NC} PostgreSQL (${DB_NAME})     ${SOFT_GRAY}│${NC} "
    if docker compose -f "$COMPOSE_FILE" exec -T $DB_CONTAINER pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} Redis Cache             ${SOFT_GRAY}│${NC} "
    if docker compose -f "$COMPOSE_FILE" exec -T redis redis-cli --no-auth-warning -a "${REDIS_PASSWORD}" ping &>/dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} MinIO Storage           ${SOFT_GRAY}│${NC} "
    if docker compose -f "$COMPOSE_FILE" exec -T minio curl -fs http://localhost:9000/minio/health/live &>/dev/null 2>&1; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        if docker compose -f "$COMPOSE_FILE" ps --status running -q minio 2>/dev/null | grep -q .; then
            echo_e "${SOFT_YELLOW}◒ RUNNING${NC}      ${SOFT_GRAY}│${NC}"
        else
            echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
        fi
    fi

    printf "  ${SOFT_GRAY}│${NC} Ollama AI               ${SOFT_GRAY}│${NC} "
    if curl -f -s http://localhost:11434/api/tags &>/dev/null; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_YELLOW}◒ OPTIONAL${NC}     ${SOFT_GRAY}│${NC}"
    fi

    printf "  ${SOFT_GRAY}│${NC} Traefik Gateway         ${SOFT_GRAY}│${NC} "
    if gateway_is_reachable "/api/v1/server/info"; then
        echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
    else
        echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
    fi

    echo_e "  ${SOFT_GRAY}├─────────────────────────┼──────────────────┤${NC}"

    # Check services via gateway (services don't expose ports directly)
    local services=(
        "Dokus Server:/api/v1/server/info"
        "Cashflow API:/api/v1/invoices"
        "Payments API:/api/v1/payments"
        "Banking API:/api/v1/banking"
        "Contacts API:/api/v1/contacts"
        "Web Frontend:/"
    )

    for service_info in "${services[@]}"; do
        IFS=':' read -r service_name check_type check_target <<< "$service_info"
        printf "  ${SOFT_GRAY}│${NC} %-23s ${SOFT_GRAY}│${NC} " "$service_name"

        if [ "$check_type" == "container" ]; then
            # Container-based health check for background workers
            if docker compose -f "$COMPOSE_FILE" ps --status running -q "$check_target" 2>/dev/null | grep -q .; then
                echo_e "${SOFT_GREEN}◎ RUNNING${NC}       ${SOFT_GRAY}│${NC}"
            else
                echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
            fi
        else
            # Gateway-based health check (check_type is actually the endpoint)
            local endpoint="$check_type"
            if gateway_is_reachable "$endpoint"; then
                echo_e "${SOFT_GREEN}◎ HEALTHY${NC}       ${SOFT_GRAY}│${NC}"
            else
                echo_e "${SOFT_RED}⨯ DOWN${NC}          ${SOFT_GRAY}│${NC}"
            fi
        fi
    done

    echo_e "  ${SOFT_GRAY}└─────────────────────────┴──────────────────┘${NC}"
    echo ""
}

print_services_info() {
    print_separator
    echo ""
    echo_e "  ${SOFT_GREEN}${BOLD}API Gateway${NC}\n"

    # Cloud profile uses HTTPS with domain
    if [ "${DOKUS_PROFILE:-}" = "cloud" ]; then
        # Load domain from .env if available
        local DOMAIN="app.dokus.tech"
        if [ -f .env ]; then
            source <(grep -E '^DOMAIN=' .env 2>/dev/null || true)
            DOMAIN="${DOMAIN:-app.dokus.tech}"
        fi

        echo_e "  ${SOFT_GRAY}┌──────────────────────────────────────────────────────────────────┐${NC}"
        echo_e "  ${SOFT_GRAY}│${NC}  ${BOLD}${SOFT_CYAN}https://${DOMAIN}${NC}   ${DIM_WHITE}← Unified API Gateway (HTTPS)${NC}        ${SOFT_GRAY}│${NC}"
        echo_e "  ${SOFT_GRAY}│${NC}  ${DIM_WHITE}Dashboard: ${SOFT_ORANGE}https://traefik.${DOMAIN}${NC}                        ${SOFT_GRAY}│${NC}"
        echo_e "  ${SOFT_GRAY}└──────────────────────────────────────────────────────────────────┘${NC}"
    else
        # Self-hosting uses HTTP with IP
        local SERVER_IP=$(get_server_ip)
        if [ "$SERVER_IP" = "localhost" ]; then
            SERVER_IP="127.0.0.1"
        fi

        echo_e "  ${SOFT_GRAY}┌──────────────────────────────────────────────────────────────────┐${NC}"
        echo_e "  ${SOFT_GRAY}│${NC}  ${BOLD}${SOFT_CYAN}http://${SERVER_IP}:${GATEWAY_PORT}${NC}   ${DIM_WHITE}← Unified API Gateway${NC}               ${SOFT_GRAY}│${NC}"
        echo_e "  ${SOFT_GRAY}│${NC}  ${DIM_WHITE}Dashboard: ${SOFT_ORANGE}http://${SERVER_IP}:${GATEWAY_DASHBOARD_PORT}${NC}                          ${SOFT_GRAY}│${NC}"
        echo_e "  ${SOFT_GRAY}└──────────────────────────────────────────────────────────────────┘${NC}"
    fi

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
    echo_e "  ${SOFT_GRAY}├─────────────────────────────┼────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${DIM_WHITE}/*${NC}                          ${SOFT_GRAY}│${NC} ${SOFT_CYAN}Web Frontend (WASM)${NC}              ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}└─────────────────────────────┴────────────────────────────────────┘${NC}"

    echo ""
    echo_e "  ${SOFT_CYAN}${BOLD}💾 Infrastructure Services${NC}\n"

    echo_e "  ${SOFT_GRAY}┌──────────────────────┬─────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Service${NC}              ${SOFT_GRAY}│${NC} ${BOLD}Connection${NC}                              ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├──────────────────────┼─────────────────────────────────────────┤${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_CYAN}PostgreSQL${NC}           ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:${DB_PORT}${NC} • ${SOFT_GRAY}${DB_NAME}${NC}            ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${SOFT_ORANGE}Redis Cache${NC}          ${SOFT_GRAY}│${NC} ${DIM_WHITE}localhost:16379${NC}                     ${SOFT_GRAY}│${NC}"
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
    docker compose -f "$COMPOSE_FILE" pull -q

    print_status info "Starting services..."
    docker compose --compatibility -f "$COMPOSE_FILE" up -d

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
    docker compose -f "$COMPOSE_FILE" down
    echo ""
    print_status success "All services stopped"
    echo ""
}

restart_services() {
    stop_services
    echo ""
    start_services
}

update_services() {
    if ! check_env; then
        return 1
    fi

    print_gradient_header "⬆️ Updating Dokus" "Pulling new images + applying updates"

    print_status info "Checking registry and pulling images..."
    docker compose -f "$COMPOSE_FILE" pull

    echo ""
    print_status info "Applying updates (restart/recreate as needed)..."
    docker compose --compatibility -f "$COMPOSE_FILE" up -d --remove-orphans

    echo ""
    print_status success "Update complete"
    docker compose -f "$COMPOSE_FILE" ps
}

show_logs() {
    service=$1
    if [ -z "$service" ]; then
        docker compose -f "$COMPOSE_FILE" logs -f
    else
        docker compose -f "$COMPOSE_FILE" logs -f $service
    fi
}

access_db() {
    print_gradient_header "🗄️  Database CLI"
    print_status info "Connecting to PostgreSQL (${DB_NAME})..."
    echo ""
    docker compose -f "$COMPOSE_FILE" exec $DB_CONTAINER psql -U $DB_USER -d $DB_NAME
}

# Function to show mobile app connection info with QR code
show_mobile_connection() {
    print_gradient_header "Mobile App Connection" "Connect your mobile app to this server"

    local SERVER_PROTOCOL
    local SERVER_PORT
    local SERVER_HOST

    # Cloud profile uses HTTPS with domain
    if [ "${DOKUS_PROFILE:-}" = "cloud" ]; then
        SERVER_PROTOCOL="https"
        SERVER_PORT="443"
        # Load domain from .env if available
        SERVER_HOST="app.dokus.tech"
        if [ -f .env ]; then
            source <(grep -E '^DOMAIN=' .env 2>/dev/null || true)
            SERVER_HOST="${DOMAIN:-app.dokus.tech}"
        fi
        print_status info "Cloud domain: ${SERVER_HOST}"
    else
        # Self-hosting uses HTTP
        SERVER_PROTOCOL="http"
        SERVER_PORT="${GATEWAY_PORT}"

        # Get server IP
        local SERVER_IP=$(get_server_ip)
        SERVER_HOST="${SERVER_IP}"

        if [ "$SERVER_IP" = "localhost" ]; then
            SERVER_HOST="127.0.0.1"
        fi

        print_status info "Detected IP: ${SERVER_HOST}"
    fi

    # Generate deep link URL
    local CONNECT_URL="dokus://connect?host=${SERVER_HOST}&port=${SERVER_PORT}&protocol=${SERVER_PROTOCOL}"

    echo_e "  ${SOFT_CYAN}${BOLD}Server Connection Details${NC}\n"

    echo_e "  ${SOFT_GRAY}┌───────────────────────────────────────────────────────────────┐${NC}"
    echo_e "  ${SOFT_GRAY}│${NC} ${BOLD}Manual Entry${NC}                                                  ${SOFT_GRAY}│${NC}"
    echo_e "  ${SOFT_GRAY}├───────────────────────────────────────────────────────────────┤${NC}"
    printf "  ${SOFT_GRAY}│${NC}  Protocol:  ${SOFT_CYAN}%-47s${NC} ${SOFT_GRAY}│${NC}\n" "${SERVER_PROTOCOL}"
    printf "  ${SOFT_GRAY}│${NC}  Host:      ${SOFT_CYAN}%-47s${NC} ${SOFT_GRAY}│${NC}\n" "${SERVER_HOST}"
    printf "  ${SOFT_GRAY}│${NC}  Port:      ${SOFT_CYAN}%-47s${NC} ${SOFT_GRAY}│${NC}\n" "${SERVER_PORT}"
    echo_e "  ${SOFT_GRAY}└───────────────────────────────────────────────────────────────┘${NC}"

    echo ""
    echo_e "  ${SOFT_GRAY}Deep Link URL:${NC}"
    echo_e "  ${DIM_WHITE}${CONNECT_URL}${NC}"
    echo ""

    # Generate QR code (requires qrencode)
    if command -v qrencode &> /dev/null; then
        echo_e "  ${SOFT_CYAN}${BOLD}Scan with Mobile App${NC}\n"
        qrencode -t ANSIUTF8 -m 2 "$CONNECT_URL"
    else
        echo_e "  ${SOFT_YELLOW}${SYMBOL_WARN}${NC}  Install 'qrencode' for QR code display:"
        echo_e "     ${DIM_WHITE}brew install qrencode${NC}  (macOS)"
        echo_e "     ${DIM_WHITE}apt install qrencode${NC}   (Linux)"
    fi

    echo ""
    echo_e "  ${SOFT_GRAY}${DIM}In the Dokus app, tap 'Connect to Server' and scan this QR code${NC}"
    echo_e "  ${SOFT_GRAY}${DIM}or enter the connection details manually.${NC}"
    echo ""
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
    echo_e "  ${SOFT_BLUE}This wizard will generate secure credentials for your deployment.${NC}"
    echo ""

    # Generate secure passwords
    DB_PASS=$(generate_password)
    REDIS_PASS=$(generate_password)
    MINIO_PASS=$(generate_password)
    JWT_SECRET=$(openssl rand -base64 64 | tr -d "=+/" | cut -c1-64 2>/dev/null || LC_ALL=C tr -dc 'A-Za-z0-9' </dev/urandom | head -c 64)

    # Core credentials (minimal prompts - use defaults for most)
    print_status task "Core Credentials"
    DB_PASSWORD=$(prompt_with_default "Database password:" "$DB_PASS" "DB_PASSWORD" "true")
    REDIS_PASSWORD=$(prompt_with_default "Redis password:" "$REDIS_PASS" "REDIS_PASSWORD" "true")
    MINIO_PASSWORD=$(prompt_with_default "MinIO password:" "$MINIO_PASS" "MINIO_PASSWORD" "true")
    JWT_SECRET_VAL=$(prompt_with_default "JWT secret (64+ chars):" "$JWT_SECRET" "JWT_SECRET" "true")

    # Create logs directory
    mkdir -p logs/traefik logs/server
    print_status success "Created logs/traefik/ and logs/server/ directories"

    # Cloud profile needs additional gateway config
    if [ "${DOKUS_PROFILE:-}" = "cloud" ]; then
        echo ""
        print_status task "Cloud Gateway Configuration (Traefik + Let's Encrypt)"

        DOMAIN=$(prompt_with_default "Domain for your Dokus instance:" "app.dokus.tech" "DOMAIN")
        ACME_EMAIL=$(prompt_with_default "Email for Let's Encrypt certificates:" "contact@dokus.tech" "ACME_EMAIL")

        cat > .env << EOF
# Dokus Cloud Environment Configuration
# Generated on $(date)
# Profile: Cloud (HTTPS with Let's Encrypt)

# ============================================================================
# REQUIRED - Core Credentials
# ============================================================================
DB_PASSWORD=$DB_PASSWORD
REDIS_PASSWORD=$REDIS_PASSWORD
MINIO_PASSWORD=$MINIO_PASSWORD
JWT_SECRET=$JWT_SECRET_VAL

# ============================================================================
# CLOUD GATEWAY (Traefik + Let's Encrypt)
# ============================================================================
DOMAIN=$DOMAIN
ACME_EMAIL=$ACME_EMAIL

# ============================================================================
# STORAGE - Public URL for presigned URLs (MinIO via Traefik)
# ============================================================================
STORAGE_PUBLIC_URL=https://$DOMAIN/storage
EOF

    else
        # Self-hosting profile (Pro/Lite) - simpler config
        local SERVER_IP=$(get_server_ip)
        if [ "$SERVER_IP" = "localhost" ]; then
            SERVER_IP="127.0.0.1"
        fi

        cat > .env << EOF
# Dokus Self-Hosting Environment Configuration
# Generated on $(date)
# Profile: ${DOKUS_PROFILE:-lite}

# ============================================================================
# REQUIRED - Core Credentials
# ============================================================================
DB_PASSWORD=$DB_PASSWORD
REDIS_PASSWORD=$REDIS_PASSWORD
MINIO_PASSWORD=$MINIO_PASSWORD
JWT_SECRET=$JWT_SECRET_VAL

# ============================================================================
# STORAGE - Public URL for presigned URLs (MinIO via Traefik)
# ============================================================================
# Update this if your server IP changes or you use a custom hostname
STORAGE_PUBLIC_URL=http://${SERVER_IP}:${GATEWAY_PORT}/storage
EOF
    fi

    echo ""
    print_status success ".env created with secure credentials"

    echo ""
    print_status task "Configuring Docker registry"
    configure_registry

    echo ""
    print_status task "Pulling latest Docker images"
    docker compose -f "$COMPOSE_FILE" pull
    print_status success "Images pulled"

    echo ""
    print_status task "Starting Dokus services"
    docker compose --compatibility -f "$COMPOSE_FILE" up -d
    print_status success "Services started"

    echo ""
    print_status task "Waiting for services to become ready"
    sleep 5

    local MAX_RETRIES=40
    local RETRY_COUNT=0
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        local pg_ok=0
        local redis_ok=0
        local minio_ok=0
        local api_ok=0

        if docker compose -f "$COMPOSE_FILE" exec -T postgres pg_isready -U $DB_USER -d $DB_NAME &>/dev/null; then
            pg_ok=1
        fi

        if docker compose -f "$COMPOSE_FILE" exec -T redis redis-cli --no-auth-warning -a "${REDIS_PASSWORD}" ping &>/dev/null 2>&1; then
            redis_ok=1
        fi

        if docker compose -f "$COMPOSE_FILE" exec -T minio curl -fs http://localhost:9000/minio/health/live &>/dev/null 2>&1; then
            minio_ok=1
        fi

        if gateway_is_reachable "/api/v1/server/info"; then
            api_ok=1
        fi

        if [ $pg_ok -eq 1 ] && [ $redis_ok -eq 1 ] && [ $minio_ok -eq 1 ] && [ $api_ok -eq 1 ]; then
            print_status success "Core services are ready"
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

select_profile() {
    print_gradient_header "Profile Selection" "Choose deployment profile"

    echo_e "  ${SOFT_CYAN}Current profile: ${SOFT_GREEN}$(get_profile_display)${NC}"
    echo_e "  ${SOFT_CYAN}Using: ${DIM_WHITE}$COMPOSE_FILE${NC}"
    echo ""

    echo_e "  ${SOFT_GRAY}Available profiles:${NC}"
    echo ""
    echo_e "  ${SOFT_MAGENTA}1${NC}   ${BOLD}Cloud${NC} ${DIM_WHITE}(Production HTTPS)${NC}"
    echo_e "      ${DIM_WHITE}HTTPS with Let's Encrypt, domain-based routing${NC}"
    echo_e "      ${DIM_WHITE}File: docker-compose.cloud.yml${NC}"
    echo ""
    echo_e "  ${SOFT_GREEN}2${NC}   ${BOLD}Pro${NC} ${DIM_WHITE}(Self-Host High Performance)${NC}"
    echo_e "      ${DIM_WHITE}HTTP:8000, G1GC, 1GB heap, more connections${NC}"
    echo_e "      ${DIM_WHITE}File: docker-compose.pro.yml${NC}"
    echo ""
    echo_e "  ${SOFT_YELLOW}3${NC}   ${BOLD}Lite${NC} ${DIM_WHITE}(Self-Host Low Resource)${NC}"
    echo_e "      ${DIM_WHITE}HTTP:8000, SerialGC, 256MB heap, minimal memory${NC}"
    echo_e "      ${DIM_WHITE}File: docker-compose.lite.yml${NC}"
    echo ""
    echo_e "  ${SOFT_GRAY}0${NC}   Cancel"
    echo ""

    printf "  ${BOLD}Select profile ${DIM_WHITE}[0-3]:${NC} "
    read choice
    echo ""

    case $choice in
        1)
            DOKUS_PROFILE="cloud"
            COMPOSE_FILE="docker-compose.cloud.yml"
            save_profile
            print_status success "Profile set to Cloud"
            ;;
        2)
            DOKUS_PROFILE="pro"
            COMPOSE_FILE="docker-compose.pro.yml"
            save_profile
            print_status success "Profile set to Pro"
            ;;
        3)
            DOKUS_PROFILE="lite"
            COMPOSE_FILE="docker-compose.lite.yml"
            save_profile
            print_status success "Profile set to Lite"
            ;;
        0)
            print_status info "Cancelled"
            return
            ;;
        *)
            print_status error "Invalid choice"
            return
            ;;
    esac

    echo ""
    print_status info "Using compose file: $COMPOSE_FILE"
    echo ""
    print_status warning "Restart services for changes to take effect"
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
        <string>cd $SCRIPT_PATH && docker compose --compatibility -f $COMPOSE_FILE up -d</string>
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
ExecStart=/usr/bin/docker compose --compatibility -f $COMPOSE_FILE up -d
ExecStop=/usr/bin/docker compose -f $COMPOSE_FILE down
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
    # Prompt for profile selection on first run
    if [ -z "${DOKUS_PROFILE:-}" ]; then
        splash_screen
        prompt_profile_selection
    fi

    splash_screen

    # Show current profile
    echo_e "  ${SOFT_GRAY}Profile: ${SOFT_GREEN}$(get_profile_display)${NC} ${DIM_WHITE}($COMPOSE_FILE)${NC}\n"

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

    echo_e "  ${SOFT_BLUE}${BOLD}Mobile App${NC}"
    echo_e "    ${SOFT_CYAN}8${NC}   Show mobile connection (QR code)"
    echo ""

    echo_e "  ${SOFT_ORANGE}${BOLD}Configuration${NC}"
    echo_e "    ${SOFT_CYAN}9${NC}   Select profile (pro/lite)"
    echo ""

    echo_e "  ${SOFT_YELLOW}${BOLD}Maintenance${NC}"
    echo_e "    ${SOFT_CYAN}10${NC}  Check + pull updates (restart)"
    echo ""

    echo_e "  ${SOFT_RED}${BOLD}Developer${NC}"
    if [ "$DEBUG_MODE" = "true" ]; then
        echo_e "    ${SOFT_CYAN}11${NC}  Toggle debug mode ${SOFT_GREEN}[ON]${NC} - port 5005"
    else
        echo_e "    ${SOFT_CYAN}11${NC}  Toggle debug mode ${SOFT_GRAY}[OFF]${NC}"
    fi
    echo ""

    echo_e "  ${SOFT_GRAY}0${NC}    Exit"
    echo ""

    printf "  ${BOLD}Select channel ${DIM_WHITE}[0-11]:${NC} "
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
        8) show_mobile_connection ;;
        9) select_profile ;;
        10) check_docker && update_services ;;
        11) check_docker && toggle_debug_mode ;;
        0) echo "  ${SOFT_CYAN}👋 Goodbye!${NC}\n" && exit 0 ;;
        *) print_status error "Invalid choice" && sleep 1 && show_menu ;;
    esac

    echo ""
    printf "  ${DIM}Press Enter to continue...${NC}"
    read
    show_menu
}

main() {
    # Parse --profile option first
    local cmd=""
    local args=()

    for arg in "$@"; do
        case $arg in
            --profile=*)
                DOKUS_PROFILE="${arg#*=}"
                case "$DOKUS_PROFILE" in
                    cloud)
                        COMPOSE_FILE="docker-compose.cloud.yml"
                        ;;
                    pro)
                        COMPOSE_FILE="docker-compose.pro.yml"
                        ;;
                    lite)
                        COMPOSE_FILE="docker-compose.lite.yml"
                        ;;
                    *)
                        print_status error "Unknown profile: $DOKUS_PROFILE"
                        print_status info "Available profiles: cloud, pro, lite"
                        exit 1
                        ;;
                esac
                ;;
            *)
                if [ -z "$cmd" ]; then
                    cmd="$arg"
                else
                    args+=("$arg")
                fi
                ;;
        esac
    done

    # For CLI commands that need a profile, prompt if not set
    if [ -z "${DOKUS_PROFILE:-}" ] && [ -n "${cmd:-}" ] && [[ "$cmd" =~ ^(start|stop|restart|status|logs|setup|update|upgrade)$ ]]; then
        prompt_profile_selection
    fi

    case ${cmd:-} in
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
        update|upgrade)
            check_docker
            update_services
            ;;
        restart)
            check_docker
            restart_services
            ;;
        status)
            show_status
            ;;
        logs)
            show_logs ${args[0]:-}
            ;;
        db)
            access_db
            ;;
        connect|qr)
            show_mobile_connection
            ;;
        debug)
            check_docker
            toggle_debug_mode
            ;;
        profile)
            if [ -n "${args[0]:-}" ]; then
                # Set profile from CLI
                case "${args[0]}" in
                    cloud|pro|lite)
                        DOKUS_PROFILE="${args[0]}"
                        case "$DOKUS_PROFILE" in
                            cloud) COMPOSE_FILE="docker-compose.cloud.yml" ;;
                            pro) COMPOSE_FILE="docker-compose.pro.yml" ;;
                            *) COMPOSE_FILE="docker-compose.lite.yml" ;;
                        esac
                        save_profile
                        print_status success "Profile set to $(get_profile_display)"
                        print_status info "Using: $COMPOSE_FILE"
                        ;;
                    *)
                        print_status error "Unknown profile: ${args[0]}"
                        print_status info "Available profiles: cloud, pro, lite"
                        exit 1
                        ;;
                esac
            else
                # Show current profile
                print_status info "Current profile: $(get_profile_display)"
                print_status info "Using: $COMPOSE_FILE"
                echo ""
                print_status info "Set profile with: ./dokus.sh profile <cloud|pro|lite>"
                print_status info "Or use --profile flag: ./dokus.sh --profile=cloud start"
            fi
            ;;
        help|--help|-h)
            echo ""
            echo_e "  ${BOLD}Dokus Management Script${NC}"
            echo ""
            echo_e "  ${SOFT_CYAN}Usage:${NC} ./dokus.sh [--profile=<cloud|pro|lite>] [command]"
            echo ""
            echo_e "  ${SOFT_GREEN}Commands:${NC}"
            echo_e "    setup      Guided initial setup"
            echo_e "    start      Start all services"
            echo_e "    stop       Stop all services"
            echo_e "    update     Pull new images and apply updates"
            echo_e "    restart    Restart all services"
            echo_e "    status     Show service status"
            echo_e "    logs       View logs (optionally: logs <service>)"
            echo_e "    db         Access PostgreSQL console"
            echo_e "    connect    Show mobile app connection info"
            echo_e "    profile    Show/set deployment profile"
            echo_e "    debug      Toggle debug mode (JDWP on port 5005)"
            echo ""
            echo_e "  ${SOFT_ORANGE}Profiles:${NC}"
            echo_e "    cloud      Production HTTPS with Let's Encrypt"
            echo_e "    pro        Self-host high performance (Mac/servers)"
            echo_e "    lite       Self-host low resource (Raspberry Pi) [default]"
            echo ""
            echo_e "  ${SOFT_MAGENTA}Examples:${NC}"
            echo_e "    ./dokus.sh                       Interactive menu"
            echo_e "    ./dokus.sh start                 Start with saved profile"
            echo_e "    ./dokus.sh --profile=cloud start Start with cloud profile"
            echo_e "    ./dokus.sh profile cloud         Set and save profile"
            echo ""
            ;;
        *)
            show_menu
            ;;
    esac
}

main "$@"
