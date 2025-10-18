#!/bin/bash

###############################################################################
# Automated Database Backup Script for Dokus
#
# Features:
# - PostgreSQL backup with compression
# - S3 upload with encryption
# - Retention policy (30 days default)
# - Backup verification
# - Slack/Discord notifications
# - Error handling and logging
#
# Usage:
#   ./backup-database.sh [environment]
#
# Environment variables required:
#   DB_HOST, DB_PORT, DB_NAME, DB_USER, DB_PASSWORD
#   AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY
#   S3_BUCKET, S3_REGION
#   SLACK_WEBHOOK_URL (optional)
###############################################################################

set -euo pipefail  # Exit on error, undefined variables, and pipe failures

# Configuration
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
LOG_FILE="/var/log/dokus-backup.log"
BACKUP_DIR="/tmp/dokus-backups"
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
ENVIRONMENT="${1:-production}"

# Database configuration
DB_HOST="${DB_HOST:-postgres}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-dokus}"
DB_USER="${DB_USER:-produser}"
DB_PASSWORD="${DB_PASSWORD:-}"

# S3 configuration
S3_BUCKET="${S3_BUCKET:-dokus-backups}"
S3_REGION="${S3_REGION:-eu-central-1}"
S3_PREFIX="postgres-backups/${ENVIRONMENT}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"

# Notification configuration
SLACK_WEBHOOK_URL="${SLACK_WEBHOOK_URL:-}"
DISCORD_WEBHOOK_URL="${DISCORD_WEBHOOK_URL:-}"

# Backup file names
BACKUP_FILENAME="dokus-${ENVIRONMENT}-${TIMESTAMP}.sql.gz"
BACKUP_PATH="${BACKUP_DIR}/${BACKUP_FILENAME}"
S3_PATH="s3://${S3_BUCKET}/${S3_PREFIX}/${BACKUP_FILENAME}"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

###############################################################################
# Logging Functions
###############################################################################

log() {
    echo "[$(date +'%Y-%m-%d %H:%M:%S')] $*" | tee -a "${LOG_FILE}"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $*" | tee -a "${LOG_FILE}"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $*" | tee -a "${LOG_FILE}"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $*" | tee -a "${LOG_FILE}"
}

###############################################################################
# Notification Functions
###############################################################################

send_slack_notification() {
    local status="$1"
    local message="$2"

    if [[ -z "${SLACK_WEBHOOK_URL}" ]]; then
        return 0
    fi

    local color="good"
    if [[ "${status}" == "error" ]]; then
        color="danger"
    elif [[ "${status}" == "warning" ]]; then
        color="warning"
    fi

    local payload=$(cat <<EOF
{
    "attachments": [
        {
            "color": "${color}",
            "title": "Dokus Database Backup - ${ENVIRONMENT}",
            "text": "${message}",
            "footer": "Dokus Backup System",
            "ts": $(date +%s)
        }
    ]
}
EOF
)

    curl -X POST -H 'Content-type: application/json' \
        --data "${payload}" \
        "${SLACK_WEBHOOK_URL}" \
        --silent --output /dev/null || true
}

send_discord_notification() {
    local status="$1"
    local message="$2"

    if [[ -z "${DISCORD_WEBHOOK_URL}" ]]; then
        return 0
    fi

    local color="3066993"  # Green
    if [[ "${status}" == "error" ]]; then
        color="15158332"  # Red
    elif [[ "${status}" == "warning" ]]; then
        color="16776960"  # Yellow
    fi

    local payload=$(cat <<EOF
{
    "embeds": [
        {
            "title": "Dokus Database Backup - ${ENVIRONMENT}",
            "description": "${message}",
            "color": ${color},
            "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%S.000Z)"
        }
    ]
}
EOF
)

    curl -X POST -H 'Content-type: application/json' \
        --data "${payload}" \
        "${DISCORD_WEBHOOK_URL}" \
        --silent --output /dev/null || true
}

notify() {
    local status="$1"
    local message="$2"

    send_slack_notification "${status}" "${message}"
    send_discord_notification "${status}" "${message}"
}

###############################################################################
# Validation Functions
###############################################################################

check_prerequisites() {
    log "Checking prerequisites..."

    # Check required commands
    local required_commands=("pg_dump" "gzip" "aws" "curl")
    for cmd in "${required_commands[@]}"; do
        if ! command -v "${cmd}" &> /dev/null; then
            log_error "Required command not found: ${cmd}"
            exit 1
        fi
    done

    # Check environment variables
    if [[ -z "${DB_PASSWORD}" ]]; then
        log_error "DB_PASSWORD environment variable is not set"
        exit 1
    fi

    if [[ -z "${AWS_ACCESS_KEY_ID}" ]] || [[ -z "${AWS_SECRET_ACCESS_KEY}" ]]; then
        log_error "AWS credentials are not set"
        exit 1
    fi

    # Check S3 bucket exists
    if ! aws s3 ls "s3://${S3_BUCKET}" --region "${S3_REGION}" &> /dev/null; then
        log_error "S3 bucket does not exist or is not accessible: ${S3_BUCKET}"
        exit 1
    fi

    # Create backup directory
    mkdir -p "${BACKUP_DIR}"

    log_success "Prerequisites check passed"
}

###############################################################################
# Backup Functions
###############################################################################

create_backup() {
    log "Creating database backup..."
    log "Database: ${DB_HOST}:${DB_PORT}/${DB_NAME}"
    log "Backup file: ${BACKUP_PATH}"

    # Set PostgreSQL password
    export PGPASSWORD="${DB_PASSWORD}"

    # Create backup with compression
    if pg_dump \
        --host="${DB_HOST}" \
        --port="${DB_PORT}" \
        --username="${DB_USER}" \
        --dbname="${DB_NAME}" \
        --format=plain \
        --no-owner \
        --no-privileges \
        --verbose \
        2>> "${LOG_FILE}" | gzip -9 > "${BACKUP_PATH}"; then

        unset PGPASSWORD

        # Get backup size
        local backup_size=$(du -h "${BACKUP_PATH}" | cut -f1)
        log_success "Backup created successfully (${backup_size})"

        return 0
    else
        unset PGPASSWORD
        log_error "Failed to create backup"
        return 1
    fi
}

verify_backup() {
    log "Verifying backup integrity..."

    # Check if file exists and is not empty
    if [[ ! -f "${BACKUP_PATH}" ]]; then
        log_error "Backup file does not exist"
        return 1
    fi

    if [[ ! -s "${BACKUP_PATH}" ]]; then
        log_error "Backup file is empty"
        return 1
    fi

    # Verify gzip integrity
    if ! gzip -t "${BACKUP_PATH}" 2>> "${LOG_FILE}"; then
        log_error "Backup file is corrupted (gzip test failed)"
        return 1
    fi

    # Check minimum size (should be at least 1KB)
    local size=$(stat -f%z "${BACKUP_PATH}" 2>/dev/null || stat -c%s "${BACKUP_PATH}" 2>/dev/null)
    if [[ ${size} -lt 1024 ]]; then
        log_error "Backup file is too small (${size} bytes)"
        return 1
    fi

    log_success "Backup verification passed"
    return 0
}

upload_to_s3() {
    log "Uploading backup to S3..."
    log "Destination: ${S3_PATH}"

    # Upload with server-side encryption
    if aws s3 cp "${BACKUP_PATH}" "${S3_PATH}" \
        --region "${S3_REGION}" \
        --storage-class STANDARD_IA \
        --server-side-encryption AES256 \
        --metadata "environment=${ENVIRONMENT},timestamp=${TIMESTAMP}" \
        2>> "${LOG_FILE}"; then

        log_success "Backup uploaded to S3 successfully"

        # Verify upload
        if aws s3 ls "${S3_PATH}" --region "${S3_REGION}" &> /dev/null; then
            log_success "Upload verification passed"
            return 0
        else
            log_error "Upload verification failed"
            return 1
        fi
    else
        log_error "Failed to upload backup to S3"
        return 1
    fi
}

cleanup_old_backups() {
    log "Cleaning up old backups (retention: ${RETENTION_DAYS} days)..."

    # Calculate cutoff date
    local cutoff_date=$(date -d "${RETENTION_DAYS} days ago" +%Y%m%d 2>/dev/null || \
                        date -v-${RETENTION_DAYS}d +%Y%m%d)

    # List and delete old backups
    local deleted_count=0

    aws s3 ls "s3://${S3_BUCKET}/${S3_PREFIX}/" --region "${S3_REGION}" | \
    while read -r line; do
        local filename=$(echo "${line}" | awk '{print $4}')

        # Extract date from filename (format: dokus-ENV-YYYYMMDD_HHMMSS.sql.gz)
        if [[ "${filename}" =~ dokus-${ENVIRONMENT}-([0-9]{8})_ ]]; then
            local backup_date="${BASH_REMATCH[1]}"

            if [[ "${backup_date}" -lt "${cutoff_date}" ]]; then
                log "Deleting old backup: ${filename}"
                aws s3 rm "s3://${S3_BUCKET}/${S3_PREFIX}/${filename}" \
                    --region "${S3_REGION}" 2>> "${LOG_FILE}"
                ((deleted_count++))
            fi
        fi
    done

    log "Deleted ${deleted_count} old backups"
}

cleanup_local_backup() {
    log "Cleaning up local backup file..."

    if [[ -f "${BACKUP_PATH}" ]]; then
        rm -f "${BACKUP_PATH}"
        log "Local backup file removed"
    fi
}

###############################################################################
# Main Execution
###############################################################################

main() {
    log "========================================="
    log "Starting Dokus Database Backup"
    log "Environment: ${ENVIRONMENT}"
    log "Timestamp: ${TIMESTAMP}"
    log "========================================="

    local start_time=$(date +%s)
    local success=true

    # Check prerequisites
    if ! check_prerequisites; then
        notify "error" "Backup failed: Prerequisites check failed"
        exit 1
    fi

    # Create backup
    if ! create_backup; then
        notify "error" "Backup failed: Could not create database dump"
        cleanup_local_backup
        exit 1
    fi

    # Verify backup
    if ! verify_backup; then
        notify "error" "Backup failed: Backup verification failed"
        cleanup_local_backup
        exit 1
    fi

    # Upload to S3
    if ! upload_to_s3; then
        notify "error" "Backup failed: Could not upload to S3"
        cleanup_local_backup
        exit 1
    fi

    # Cleanup old backups
    cleanup_old_backups

    # Cleanup local backup
    cleanup_local_backup

    # Calculate duration
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))

    log_success "========================================="
    log_success "Backup completed successfully"
    log_success "Duration: ${duration} seconds"
    log_success "========================================="

    # Send success notification
    notify "good" "Database backup completed successfully in ${duration}s\\nFile: ${BACKUP_FILENAME}"

    exit 0
}

# Trap errors and send notifications
trap 'log_error "Backup script failed with error"; notify "error" "Backup script failed unexpectedly"; exit 1' ERR

# Run main function
main "$@"
