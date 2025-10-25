# Setup Guide

**Last Updated:** October 2025
**Target Audience:** Developers setting up Dokus for local development or deployment

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Quick Start](#quick-start)
3. [Environment Configuration](#environment-configuration)
4. [Client Application Setup](#client-application-setup)
5. [Backend Services Setup](#backend-services-setup)
6. [Database Setup](#database-setup)
7. [Running Tests](#running-tests)
8. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

**JDK 17 or Higher**
```bash
# Verify installation
java -version
# Should show: openjdk version "17" or higher
```

**Gradle** (Wrapper included, no separate installation needed)
```bash
./gradlew --version
```

**For Backend Services:**
- Docker & Docker Compose (for PostgreSQL and Redis)
- PostgreSQL 17+ (if not using Docker)
- Redis 8+ (if not using Docker)

**For iOS Development:**
- macOS with Xcode 14+
- CocoaPods

**Optional:**
- Android Studio (for Android development)
- IntelliJ IDEA (recommended for backend development)

---

## Quick Start

### Option 1: Client App Only (No Backend)

Perfect for UI development and testing:

```bash
# Clone the repository
git clone https://github.com/dokus/dokus.git
cd dokus

# Run on Web (with hot reload)
./gradlew wasmJsBrowserRun -t

# Access at http://localhost:8080
```

**Other platforms:**
```bash
# Desktop (macOS/Windows/Linux)
./gradlew :composeApp:run

# Android (requires Android device/emulator)
./gradlew :composeApp:assembleDebug
adb install composeApp/build/outputs/apk/debug/composeApp-debug.apk

# iOS (requires Mac with Xcode)
# Open the project in Android Studio
# Select iOS target and run
```

### Option 2: Full Stack (Client + Backend Services)

For complete local development with all services:

```bash
# Clone the repository
git clone https://github.com/dokus/dokus.git
cd dokus

# Start infrastructure services (PostgreSQL + Redis)
docker-compose -f docker-compose.dev.yml up postgres-dev redis-dev -d

# Build all backend services
./gradlew build

# Start all backend services
docker-compose -f docker-compose.dev.yml up -d

# Run the client application
./gradlew wasmJsBrowserRun -t  # Web
# OR
./gradlew :composeApp:run      # Desktop
```

**Access Points:**
- Web App: http://localhost:8080
- Auth Service: http://localhost:9091
- Database Service: http://localhost:9071
- Invoicing Service: http://localhost:9092
- Expense Service: http://localhost:9093
- Payment Service: http://localhost:9094
- Reporting Service: http://localhost:9095
- PgAdmin: http://localhost:5050 (with `--profile tools`)

**Default Credentials:**
```
PostgreSQL:
  Host: localhost
  Port: 5543
  Database: dokus
  User: dev
  Password: devpassword

Redis:
  Host: localhost
  Port: 6380
  Password: devredispass

PgAdmin:
  Email: admin@dokus.ai
  Password: admin
```

---

## Environment Configuration

Dokus uses **BuildKonfig** to manage environment-specific configuration at compile time.

### Available Environments

#### Production (Default)
```bash
./gradlew build

# Configuration:
# API_HOST: api.dokus.ai
# API_PORT: 443 (HTTPS)
# API_IS_LOCAL: false
# DEBUG: false
```

#### Local Development
```bash
./gradlew build -PENV=local

# Configuration:
# API_HOST: 127.0.0.1
# API_PORT: 8000
# API_IS_LOCAL: true
# DEBUG: true
```

#### Android Emulator
```bash
./gradlew build -PENV=localAndroid

# Configuration:
# API_HOST: 10.0.2.2  # Emulator's localhost
# API_PORT: 8000
# API_IS_LOCAL: true
# DEBUG: true
```

#### Custom Configuration
```bash
# Custom host and port
./gradlew build -PAPI_HOST=staging.dokus.ai -PAPI_PORT=8080

# Enable debug logging in production
./gradlew build -PDEBUG=true
```

### Accessing Configuration in Code

```kotlin
import ai.dokus.foundation.platform.BuildConfig

val apiHost = BuildConfig.API_HOST        // String
val apiPort = BuildConfig.API_PORT        // Int
val isLocal = BuildConfig.API_IS_LOCAL    // Boolean
val isDebug = BuildConfig.DEBUG           // Boolean
```

---

## Client Application Setup

### Platform-Specific Setup

#### Web (WASM)

```bash
# Development with hot reload
./gradlew wasmJsBrowserRun -t

# Production build
./gradlew wasmJsBrowserDistribution

# Output: composeApp/build/dist/wasmJs/productionExecutable/
```

#### Desktop (JVM)

```bash
# Run in development mode
./gradlew :composeApp:run

# Package for distribution
./gradlew :composeApp:packageReleaseDmg      # macOS
./gradlew :composeApp:packageReleaseMsi      # Windows
./gradlew :composeApp:packageReleaseDeb      # Linux

# Output: composeApp/build/compose/binaries/main-release/
```

#### Android

```bash
# Debug build
./gradlew :composeApp:assembleDebug

# Release build (requires signing configuration)
./gradlew :composeApp:assembleRelease

# Install on connected device
./gradlew :composeApp:installDebug

# Output: composeApp/build/outputs/apk/
```

**Minimum SDK:** 24 (Android 7.0)
**Target SDK:** 36 (Android 15)

#### iOS

**Requirements:**
- Mac with macOS 12+
- Xcode 14+
- CocoaPods

**Setup:**
```bash
# Install CocoaPods dependencies (if needed)
cd iosApp
pod install
cd ..

# Open in Android Studio
# Select iOS target from run configurations
# Select target device/simulator
# Click Run
```

Alternatively, open `iosApp/iosApp.xcworkspace` in Xcode.

---

## Backend Services Setup

### Starting Individual Services

Each service can be run independently:

```bash
# Auth Service
./gradlew :features:auth:backend:run

# Invoicing Service
./gradlew :features:invoicing:backend:run

# Expense Service
./gradlew :features:expense:backend:run

# Payment Service
./gradlew :features:payment:backend:run

# Reporting Service
./gradlew :features:reporting:backend:run
```

### Using Docker Compose

**Development (includes all services + infra):**
```bash
docker-compose -f docker-compose.dev.yml up -d

# View logs
docker-compose -f docker-compose.dev.yml logs -f

# Stop all services
docker-compose -f docker-compose.dev.yml down
```

**Infrastructure Only (PostgreSQL + Redis):**
```bash
docker-compose -f docker-compose.dev.yml up postgres-dev redis-dev -d
```

**With PgAdmin (database management):**
```bash
docker-compose -f docker-compose.dev.yml --profile tools up -d
```

### Service Health Checks

```bash
# Check if services are running
curl http://localhost:9091/metrics  # Auth Service
curl http://localhost:9071/metrics  # Database Service
curl http://localhost:9092/health   # Invoicing Service
curl http://localhost:9093/health   # Expense Service
curl http://localhost:9094/health   # Payment Service
curl http://localhost:9095/health   # Reporting Service
```

---

## Database Setup

### Using Docker (Recommended for Development)

```bash
# Start PostgreSQL
docker-compose -f docker-compose.dev.yml up postgres-dev -d

# Connect via psql
docker exec -it dokus-postgres-dev psql -U dev -d dokus
```

### Manual PostgreSQL Installation

**macOS (Homebrew):**
```bash
brew install postgresql@17
brew services start postgresql@17
createdb dokus
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt install postgresql-17
sudo systemctl start postgresql
sudo -u postgres createdb dokus
```

### Database Migrations

Migrations are run automatically when backend services start. To run manually:

```bash
# Using Flyway CLI (if installed)
flyway -url=jdbc:postgresql://localhost:5543/dokus \
       -user=dev \
       -password=devpassword \
       migrate

# Or use the migration script (if available)
./scripts/migrate.sh
```

**Migration Location:**
```
foundation/database/src/main/resources/db/migration/
├── V1__initial_schema.sql
├── V2__add_peppol_fields.sql
└── V3__add_audit_logs.sql
```

### Seed Test Data

```bash
# Seed database with test data (if script exists)
./scripts/seed.sh
```

---

## Running Tests

### All Tests Across Platforms

```bash
# Run all tests
./gradlew allTests

# Run with detailed output
./gradlew allTests --info
```

### Platform-Specific Tests

```bash
# Android tests
./gradlew testDebugUnitTest

# Desktop/JVM tests
./gradlew desktopTest

# iOS Simulator tests (ARM Macs)
./gradlew iosSimulatorArm64Test

# iOS Simulator tests (Intel Macs)
./gradlew iosX64Test
```

### Backend Service Tests

```bash
# Test specific backend service
./gradlew :features:auth:backend:test

# Test all backend services
./gradlew :features:auth:backend:test \
          :features:invoicing:backend:test \
          :features:expense:backend:test
```

### Integration Tests

```bash
# Run integration tests (requires running database)
./gradlew integrationTest
```

### Full Verification

```bash
# Build + test + check everything
./gradlew check

# Clean before verification
./gradlew clean check
```

---

## Troubleshooting

### Build Issues

**"Could not resolve dependency"**
```bash
# Clear Gradle cache
./gradlew clean
rm -rf ~/.gradle/caches/

# Rebuild
./gradlew build --refresh-dependencies
```

**"Execution failed for task compileKotlin"**
```bash
# Check Kotlin version
./gradlew :composeApp:dependencies

# Update Gradle wrapper
./gradlew wrapper --gradle-version=8.5
```

### Database Issues

**Cannot connect to PostgreSQL**
```bash
# Check if container is running
docker ps | grep postgres

# Check logs
docker logs dokus-postgres-dev

# Restart container
docker-compose -f docker-compose.dev.yml restart postgres-dev
```

**Migration failed**
```bash
# Check migration history
docker exec -it dokus-postgres-dev psql -U dev -d dokus \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# Repair failed migration (use carefully!)
flyway -url=jdbc:postgresql://localhost:5543/dokus repair
```

### Backend Service Issues

**Port already in use**
```bash
# Find process using the port
lsof -i :9091  # Replace with actual port

# Kill the process
kill -9 <PID>
```

**Service health check fails**
```bash
# Check service logs
docker-compose -f docker-compose.dev.yml logs auth-service-dev

# Restart service
docker-compose -f docker-compose.dev.yml restart auth-service-dev
```

### Platform-Specific Issues

**iOS: Pod install fails**
```bash
cd iosApp
pod repo update
pod install --repo-update
cd ..
```

**Android: SDK not found**
```bash
# Set ANDROID_HOME environment variable
export ANDROID_HOME=$HOME/Library/Android/sdk  # macOS
export ANDROID_HOME=$HOME/Android/Sdk          # Linux

# Add to ~/.zshrc or ~/.bashrc for persistence
```

**Web: WASM build fails**
```bash
# Clear Kotlin/JS cache
rm -rf composeApp/build/compileSync/
./gradlew :composeApp:clean
./gradlew wasmJsBrowserRun -t
```

### Common Error Messages

**"No tenant context"**
- Ensure you're authenticated
- Check JWT token includes `tenantId` claim

**"BuildConfig not found"**
```bash
# Regenerate BuildKonfig
./gradlew :foundation:platform:generateBuildKonfig
```

**"Database connection pool exhausted"**
- Restart backend services
- Check for connection leaks
- Increase `maximumPoolSize` in HikariCP config

---

## Next Steps

After successful setup:

1. **Read the Architecture Guide**: [docs/ARCHITECTURE.md](./ARCHITECTURE.md)
2. **Understand the Database Schema**: [docs/DATABASE.md](./DATABASE.md)
3. **Explore the API**: [docs/API.md](./API.md)
4. **Review Security Best Practices**: [docs/SECURITY.md](./SECURITY.md)

---

## Getting Help

- **Documentation**: [docs/](./README.md)
- **GitHub Issues**: Report bugs and request features
- **GitHub Discussions**: Ask questions and share ideas
- **Project Chat**: Join our community (link in main README)

---

**Happy Coding! 🚀**
