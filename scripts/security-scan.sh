#!/bin/bash

###############################################################################
# Security Scanning Script for Dokus
#
# Features:
# - Container image vulnerability scanning (Trivy)
# - Kubernetes configuration security audit (kubeaudit)
# - Secret detection in codebase (gitleaks)
# - Dependency vulnerability scanning (Gradle)
# - TLS/SSL configuration testing (testssl.sh)
# - Network security testing
#
# Usage:
#   ./security-scan.sh [--all] [--images] [--k8s] [--secrets] [--deps] [--tls]
#
# Requirements:
#   - trivy
#   - kubeaudit
#   - gitleaks
#   - testssl.sh
#   - kubectl (with cluster access)
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
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"
REPORT_DIR="${PROJECT_ROOT}/security-reports"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)

# Scan flags
SCAN_ALL=false
SCAN_IMAGES=false
SCAN_K8S=false
SCAN_SECRETS=false
SCAN_DEPS=false
SCAN_TLS=false

# Parse arguments
if [[ $# -eq 0 ]]; then
    SCAN_ALL=true
fi

while [[ $# -gt 0 ]]; do
    case $1 in
        --all)
            SCAN_ALL=true
            shift
            ;;
        --images)
            SCAN_IMAGES=true
            shift
            ;;
        --k8s)
            SCAN_K8S=true
            shift
            ;;
        --secrets)
            SCAN_SECRETS=true
            shift
            ;;
        --deps)
            SCAN_DEPS=true
            shift
            ;;
        --tls)
            SCAN_TLS=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            echo "Usage: $0 [--all] [--images] [--k8s] [--secrets] [--deps] [--tls]"
            exit 1
            ;;
    esac
done

# If --all is set, enable all scans
if [[ "${SCAN_ALL}" == "true" ]]; then
    SCAN_IMAGES=true
    SCAN_K8S=true
    SCAN_SECRETS=true
    SCAN_DEPS=true
    SCAN_TLS=true
fi

# Create report directory
mkdir -p "${REPORT_DIR}"

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

check_tool() {
    local tool="$1"
    local install_cmd="$2"

    if command -v "${tool}" &> /dev/null; then
        print_success "${tool} is installed"
        return 0
    else
        print_error "${tool} is not installed"
        print_info "Install with: ${install_cmd}"
        return 1
    fi
}

###############################################################################
# Container Image Scanning
###############################################################################

scan_images() {
    print_header "Container Image Vulnerability Scanning"

    if ! check_tool "trivy" "brew install aquasecurity/trivy/trivy"; then
        return 1
    fi

    local images=(
        "${DOKUS_SERVER_IMAGE:-94.111.226.82:5000/dokus-server:latest}"
        "pgvector/pgvector:pg17"
        "redis:8-alpine"
        "minio/minio:latest"
        "traefik:v3.2"
    )

    local failed=0

    for image in "${images[@]}"; do
        print_info "Scanning ${image}..."

        local report_file="${REPORT_DIR}/trivy_$(echo ${image} | tr '/:' '_')_${TIMESTAMP}.json"

        if trivy image \
            --severity CRITICAL,HIGH \
            --format json \
            --output "${report_file}" \
            "${image}"; then

            # Check for vulnerabilities
            local critical=$(jq '.Results[].Vulnerabilities | map(select(.Severity == "CRITICAL")) | length' "${report_file}" | paste -sd+ - | bc)
            local high=$(jq '.Results[].Vulnerabilities | map(select(.Severity == "HIGH")) | length' "${report_file}" | paste -sd+ - | bc)

            if [[ ${critical} -eq 0 ]] && [[ ${high} -eq 0 ]]; then
                print_success "${image}: No critical or high vulnerabilities"
            else
                print_warning "${image}: Found ${critical} critical and ${high} high vulnerabilities"
                ((failed++))
            fi
        else
            print_error "Failed to scan ${image}"
            ((failed++))
        fi
    done

    if [[ ${failed} -eq 0 ]]; then
        print_success "All images passed vulnerability scanning"
    else
        print_error "${failed} image(s) have vulnerabilities"
    fi

    return ${failed}
}

###############################################################################
# Kubernetes Configuration Audit
###############################################################################

scan_k8s() {
    print_header "Kubernetes Configuration Security Audit"

    if ! check_tool "kubeaudit" "brew install kubeaudit"; then
        print_warning "Kubeaudit not available - using manual checks"
        manual_k8s_audit
        return $?
    fi

    local k8s_dir="${PROJECT_ROOT}/k8s"
    local report_file="${REPORT_DIR}/kubeaudit_${TIMESTAMP}.txt"

    print_info "Auditing Kubernetes manifests in ${k8s_dir}..."

    kubeaudit all -f "${k8s_dir}" > "${report_file}" 2>&1 || true

    # Check for high-priority issues
    local issues=$(grep -c "ERROR\|CRITICAL" "${report_file}" || true)

    if [[ ${issues} -eq 0 ]]; then
        print_success "No critical Kubernetes security issues found"
        return 0
    else
        print_warning "Found ${issues} potential security issues"
        print_info "Review report: ${report_file}"
        return 1
    fi
}

manual_k8s_audit() {
    print_info "Running manual Kubernetes security checks..."

    local failed=0

    # Check 1: RunAsNonRoot
    print_info "Checking for runAsNonRoot..."
    if grep -r "runAsNonRoot: true" "${PROJECT_ROOT}/k8s" > /dev/null; then
        print_success "RunAsNonRoot enabled"
    else
        print_warning "RunAsNonRoot not consistently enabled"
        ((failed++))
    fi

    # Check 2: ReadOnlyRootFilesystem
    print_info "Checking for readOnlyRootFilesystem..."
    if grep -r "readOnlyRootFilesystem: true" "${PROJECT_ROOT}/k8s" > /dev/null; then
        print_success "ReadOnlyRootFilesystem enabled"
    else
        print_warning "ReadOnlyRootFilesystem not consistently enabled"
        ((failed++))
    fi

    # Check 3: Capabilities dropped
    print_info "Checking for dropped capabilities..."
    if grep -r "drop:" "${PROJECT_ROOT}/k8s" | grep -q "ALL"; then
        print_success "Capabilities dropped"
    else
        print_warning "Capabilities not dropped"
        ((failed++))
    fi

    # Check 4: Resource limits
    print_info "Checking for resource limits..."
    if grep -r "limits:" "${PROJECT_ROOT}/k8s" | grep -q "memory\|cpu"; then
        print_success "Resource limits configured"
    else
        print_warning "Resource limits not configured"
        ((failed++))
    fi

    # Check 5: Network policies
    print_info "Checking for network policies..."
    if [[ -f "${PROJECT_ROOT}/k8s/security/network-policies.yml" ]]; then
        print_success "Network policies defined"
    else
        print_warning "Network policies not found"
        ((failed++))
    fi

    return ${failed}
}

###############################################################################
# Secret Detection
###############################################################################

scan_secrets() {
    print_header "Secret Detection in Codebase"

    if ! check_tool "gitleaks" "brew install gitleaks"; then
        print_warning "Gitleaks not available - using manual checks"
        manual_secret_check
        return $?
    fi

    local report_file="${REPORT_DIR}/gitleaks_${TIMESTAMP}.json"

    print_info "Scanning for secrets in ${PROJECT_ROOT}..."

    if gitleaks detect \
        --source="${PROJECT_ROOT}" \
        --report-format=json \
        --report-path="${report_file}" \
        --no-git; then
        print_success "No secrets detected in codebase"
        return 0
    else
        local leaks=$(jq '. | length' "${report_file}")
        print_error "Found ${leaks} potential secret(s) in codebase"
        print_info "Review report: ${report_file}"
        return 1
    fi
}

manual_secret_check() {
    print_info "Running manual secret detection..."

    local failed=0

    # Common secret patterns
    local patterns=(
        "password.*=.*['\"].*['\"]"
        "api[_-]?key.*=.*['\"].*['\"]"
        "secret.*=.*['\"].*['\"]"
        "token.*=.*['\"].*['\"]"
        "private[_-]?key"
        "BEGIN.*PRIVATE.*KEY"
    )

    for pattern in "${patterns[@]}"; do
        if grep -r -i -E "${pattern}" "${PROJECT_ROOT}" \
            --exclude-dir={.git,node_modules,build,target,.gradle} \
            --exclude="*.md" > /dev/null 2>&1; then
            print_warning "Found potential secrets matching pattern: ${pattern}"
            ((failed++))
        fi
    done

    if [[ ${failed} -eq 0 ]]; then
        print_success "No obvious secrets found"
    fi

    return ${failed}
}

###############################################################################
# Dependency Vulnerability Scanning
###############################################################################

scan_dependencies() {
    print_header "Dependency Vulnerability Scanning"

    print_info "Scanning Gradle dependencies..."

    cd "${PROJECT_ROOT}"

    local report_file="${REPORT_DIR}/dependency_check_${TIMESTAMP}.html"

    # Use Gradle dependency-check plugin if available
    if ./gradlew tasks | grep -q "dependencyCheckAnalyze"; then
        if ./gradlew dependencyCheckAnalyze --no-daemon; then
            # Move report to our report directory
            find build -name "dependency-check-report.html" -exec cp {} "${report_file}" \;
            print_success "Dependency scan complete"
            print_info "Review report: ${report_file}"
            return 0
        else
            print_error "Dependency scan failed"
            return 1
        fi
    else
        print_warning "dependency-check plugin not configured"
        print_info "Add to build.gradle.kts: id(\"org.owasp.dependencycheck\") version \"8.4.0\""
        return 1
    fi
}

###############################################################################
# TLS/SSL Configuration Testing
###############################################################################

scan_tls() {
    print_header "TLS/SSL Configuration Testing"

    local domain="${1:-api.dokus.ai}"

    if ! check_tool "testssl.sh" "brew install testssl"; then
        print_warning "testssl.sh not available - using manual checks"
        manual_tls_check "${domain}"
        return $?
    fi

    local report_file="${REPORT_DIR}/testssl_${TIMESTAMP}.html"

    print_info "Testing TLS configuration for ${domain}..."

    testssl.sh \
        --html \
        --outfile "${report_file}" \
        "${domain}:443" || true

    print_success "TLS scan complete"
    print_info "Review report: ${report_file}"

    return 0
}

manual_tls_check() {
    local domain="$1"

    print_info "Checking TLS configuration for ${domain}..."

    # Check if domain is reachable
    if ! nc -zv "${domain}" 443 2>&1 | grep -q "succeeded"; then
        print_warning "Cannot reach ${domain}:443"
        return 1
    fi

    # Check certificate
    local cert_info=$(echo | openssl s_client -connect "${domain}:443" -servername "${domain}" 2>/dev/null | openssl x509 -noout -dates 2>/dev/null)

    if [[ -n "${cert_info}" ]]; then
        print_success "TLS certificate is valid"
        echo "${cert_info}"
    else
        print_error "Cannot retrieve TLS certificate"
        return 1
    fi

    return 0
}

###############################################################################
# Main Execution
###############################################################################

main() {
    print_header "Dokus Security Scanning Suite"
    print_info "Report directory: ${REPORT_DIR}"
    echo ""

    local failed=0

    if [[ "${SCAN_IMAGES}" == "true" ]]; then
        scan_images || ((failed++))
    fi

    if [[ "${SCAN_K8S}" == "true" ]]; then
        scan_k8s || ((failed++))
    fi

    if [[ "${SCAN_SECRETS}" == "true" ]]; then
        scan_secrets || ((failed++))
    fi

    if [[ "${SCAN_DEPS}" == "true" ]]; then
        scan_dependencies || ((failed++))
    fi

    if [[ "${SCAN_TLS}" == "true" ]]; then
        scan_tls "api.dokus.ai" || ((failed++))
    fi

    # Summary
    print_header "Security Scan Summary"

    if [[ ${failed} -eq 0 ]]; then
        print_success "All security scans passed!"
        print_info "Reports saved to: ${REPORT_DIR}"
        exit 0
    else
        print_error "${failed} security scan(s) found issues"
        print_info "Review reports in: ${REPORT_DIR}"
        exit 1
    fi
}

# Run main
main "$@"
