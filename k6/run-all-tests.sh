#!/bin/bash

###############################################################################
# Run All K6 Load Tests for Dokus Backend
#
# This script runs the complete test suite in the recommended order:
# 1. Smoke test (quick verification)
# 2. Load test (normal and peak load)
# 3. Spike test (sudden traffic spikes)
# 4. Stress test (find breaking point)
# 5. Soak test (2-hour endurance) - OPTIONAL
#
# Usage:
#   ./run-all-tests.sh [--skip-soak] [--base-url URL]
#
# Options:
#   --skip-soak       Skip the 2-hour soak test
#   --base-url URL    Override BASE_URL (default: http://localhost:9070)
#   --auth-url URL    Override AUTH_SERVICE_URL (default: http://localhost:8091)
#   --output-dir DIR  Directory for test results (default: ./results)
#
# Example:
#   ./run-all-tests.sh --skip-soak --base-url https://api-staging.dokus.ai
###############################################################################

set -euo pipefail

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BASE_URL="${BASE_URL:-http://localhost:9070}"
AUTH_SERVICE_URL="${AUTH_SERVICE_URL:-http://localhost:8091}"
OUTPUT_DIR="${SCRIPT_DIR}/results"
RUN_SOAK=true
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-soak)
            RUN_SOAK=false
            shift
            ;;
        --base-url)
            BASE_URL="$2"
            shift 2
            ;;
        --auth-url)
            AUTH_SERVICE_URL="$2"
            shift 2
            ;;
        --output-dir)
            OUTPUT_DIR="$2"
            shift 2
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--skip-soak] [--base-url URL] [--auth-url URL] [--output-dir DIR]"
            exit 1
            ;;
    esac
done

# Create output directory
mkdir -p "${OUTPUT_DIR}"

# Export environment variables
export BASE_URL
export AUTH_SERVICE_URL

# Functions
print_header() {
    echo ""
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}✓ $1${NC}"
}

print_error() {
    echo -e "${RED}✗ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

run_test() {
    local test_name="$1"
    local test_file="$2"
    local output_file="${OUTPUT_DIR}/${test_name}_${TIMESTAMP}.json"

    print_header "Running ${test_name}"
    print_info "Test file: ${test_file}"
    print_info "Output: ${output_file}"

    local start_time=$(date +%s)

    if k6 run --out json="${output_file}" "${test_file}"; then
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        print_success "${test_name} completed successfully in ${duration}s"
        return 0
    else
        local end_time=$(date +%s)
        local duration=$((end_time - start_time))
        print_error "${test_name} failed after ${duration}s"
        return 1
    fi
}

check_k6() {
    if ! command -v k6 &> /dev/null; then
        print_error "K6 is not installed"
        echo ""
        echo "Install K6:"
        echo "  macOS: brew install k6"
        echo "  Linux: https://k6.io/docs/getting-started/installation/"
        echo "  Docker: docker pull grafana/k6"
        exit 1
    fi

    print_success "K6 is installed ($(k6 version | head -1))"
}

check_services() {
    print_header "Checking Services"

    # Check database service
    print_info "Checking database service at ${BASE_URL}..."
    if curl -f -s -o /dev/null "${BASE_URL}/health"; then
        print_success "Database service is reachable"
    else
        print_warning "Database service is not reachable at ${BASE_URL}"
        print_warning "Tests may fail if service is not available"
    fi

    # Check auth service
    print_info "Checking auth service at ${AUTH_SERVICE_URL}..."
    if curl -f -s -o /dev/null "${AUTH_SERVICE_URL}/health"; then
        print_success "Auth service is reachable"
    else
        print_warning "Auth service is not reachable at ${AUTH_SERVICE_URL}"
        print_warning "Authentication tests will fail"
    fi
}

generate_report() {
    local report_file="${OUTPUT_DIR}/summary_${TIMESTAMP}.txt"

    print_header "Test Summary"

    {
        echo "Dokus Backend Load Test Summary"
        echo "========================================"
        echo "Date: $(date)"
        echo "Base URL: ${BASE_URL}"
        echo "Auth URL: ${AUTH_SERVICE_URL}"
        echo ""
        echo "Tests Run:"
        for result_file in "${OUTPUT_DIR}"/*_"${TIMESTAMP}".json; do
            if [[ -f "${result_file}" ]]; then
                local test_name=$(basename "${result_file}" | sed "s/_${TIMESTAMP}.json//")
                echo "  - ${test_name}"
            fi
        done
        echo ""
        echo "Results stored in: ${OUTPUT_DIR}"
    } | tee "${report_file}"

    print_success "Summary saved to ${report_file}"
}

###############################################################################
# Main Execution
###############################################################################

main() {
    print_header "Dokus Backend Load Test Suite"
    print_info "Base URL: ${BASE_URL}"
    print_info "Auth URL: ${AUTH_SERVICE_URL}"
    print_info "Output directory: ${OUTPUT_DIR}"
    print_info "Soak test: ${RUN_SOAK}"
    echo ""

    # Pre-flight checks
    check_k6
    check_services

    # Confirmation
    echo ""
    print_warning "This will run multiple load tests that may take several hours"
    if [[ "${RUN_SOAK}" == "true" ]]; then
        print_warning "Including 2-hour soak test"
    fi
    print_warning "Press Ctrl+C to cancel, or Enter to continue..."
    read -r

    local overall_start=$(date +%s)
    local failed_tests=()

    # Run tests in order
    if ! run_test "smoke-test" "${SCRIPT_DIR}/load-tests/smoke-test.js"; then
        failed_tests+=("smoke-test")
        print_error "Smoke test failed - aborting test suite"
        exit 1
    fi

    sleep 10

    if ! run_test "load-test" "${SCRIPT_DIR}/load-tests/load-test.js"; then
        failed_tests+=("load-test")
        print_warning "Load test failed - continuing with remaining tests"
    fi

    sleep 10

    if ! run_test "spike-test" "${SCRIPT_DIR}/load-tests/spike-test.js"; then
        failed_tests+=("spike-test")
        print_warning "Spike test failed - continuing with remaining tests"
    fi

    sleep 10

    if ! run_test "stress-test" "${SCRIPT_DIR}/load-tests/stress-test.js"; then
        failed_tests+=("stress-test")
        print_warning "Stress test failed - continuing with remaining tests"
    fi

    # Soak test (optional)
    if [[ "${RUN_SOAK}" == "true" ]]; then
        print_warning "Starting 2-hour soak test - you can cancel with Ctrl+C"
        sleep 5

        if ! run_test "soak-test" "${SCRIPT_DIR}/load-tests/soak-test.js"; then
            failed_tests+=("soak-test")
        fi
    else
        print_info "Skipping soak test (use --skip-soak to control)"
    fi

    # Generate report
    generate_report

    # Summary
    local overall_end=$(date +%s)
    local total_duration=$((overall_end - overall_start))
    local hours=$((total_duration / 3600))
    local minutes=$(((total_duration % 3600) / 60))

    print_header "Test Suite Complete"
    print_info "Total duration: ${hours}h ${minutes}m"

    if [[ ${#failed_tests[@]} -eq 0 ]]; then
        print_success "All tests passed!"
        exit 0
    else
        print_error "Some tests failed:"
        for test in "${failed_tests[@]}"; do
            echo "  - ${test}"
        done
        exit 1
    fi
}

# Trap Ctrl+C
trap 'echo ""; print_warning "Tests interrupted by user"; exit 130' INT

# Run main
main "$@"
