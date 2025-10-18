#!/bin/bash

###############################################################################
# Create Feature Backend Modules Script
#
# This script creates the missing backend feature modules following the
# same structure as features/auth/backend
#
# Features to create:
# - features/invoicing/backend (port 9092)
# - features/expense/backend (port 9093)
# - features/payment/backend (port 9094)
# - features/reporting/backend (port 9095)
###############################################################################

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "${SCRIPT_DIR}")"

GREEN='\033[0;32m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Creating feature backend modules...${NC}\n"

# Function to create directory structure
create_dir_structure() {
    local feature=$1
    local base_dir="${PROJECT_ROOT}/features/${feature}/backend"

    mkdir -p "${base_dir}/src/main/kotlin/ai/dokus/${feature}/backend"/{config,routes,services}
    mkdir -p "${base_dir}/src/main/resources/META-INF/resources"
    mkdir -p "${base_dir}/src/main/resources/openapi"
    mkdir -p "${base_dir}/src/test/kotlin/ai/dokus/${feature}/backend"
    mkdir -p "${base_dir}/src/test/resources"

    echo -e "${GREEN}✓${NC} Created directory structure for ${feature}"
}

# Function to create build.gradle.kts
create_build_gradle() {
    local feature=$1
    local port=$2
    local base_dir="${PROJECT_ROOT}/features/${feature}/backend"
    local package="ai.dokus.${feature}"

    cat > "${base_dir}/build.gradle.kts" << 'EOF'
plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinxRpcPlugin)
    id("com.github.johnrengelman.shadow") version "8.1.1"
    application
}

group = "PACKAGE_NAME"
version = "1.0.0"

application {
    mainClass.set("MAIN_CLASS")
}

kotlin {
    compilerOptions {
        suppressWarnings.set(true)
    }
}

dependencies {
    implementation(projects.foundation.domain)
    implementation(projects.foundation.ktorCommon)
    implementation(projects.foundation.apispec)

    implementation(libs.kotlinx.serialization)

    // KotlinX RPC Client & Server
    implementation(libs.kotlinx.rpc.core)
    implementation(libs.kotlinx.rpc.krpc.serialization.json)
    implementation(libs.kotlinx.rpc.krpc.ktor.client)
    implementation(libs.kotlinx.rpc.krpc.ktor.server)

    // Ktor Server
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.openapi)
    implementation(libs.ktor.server.swagger)

    // Ktor Client (for RPC)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)

    // Database - Exposed
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.kotlin.datetime)

    // Database - PostgreSQL
    implementation(libs.postgresql)
    implementation(libs.hikaricp)

    // Dependency Injection
    implementation(project.dependencies.platform(libs.koin.bom))
    implementation(libs.koin.ktor)

    // Security
    implementation(libs.java.jwt)

    // Logging
    implementation(libs.logback)

    // Metrics
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.micrometer.core)
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    manifest {
        attributes["Main-Class"] = "MAIN_CLASS"
    }
    mergeServiceFiles()
    archiveClassifier.set("")
    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
}

tasks.named<Tar>("distTar") {
    dependsOn("shadowJar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named<Zip>("distZip") {
    dependsOn("shadowJar")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("startScripts") {
    dependsOn("shadowJar")
}

tasks.named("startShadowScripts") {
    enabled = false
}

tasks.named("shadowDistTar") {
    enabled = false
}

tasks.named("shadowDistZip") {
    enabled = false
}

tasks.named("build") {
    dependsOn("shadowJar")
}
EOF

    sed -i '' "s|PACKAGE_NAME|${package}|g" "${base_dir}/build.gradle.kts"
    sed -i '' "s|MAIN_CLASS|ai.dokus.${feature}.backend.ApplicationKt|g" "${base_dir}/build.gradle.kts"

    echo -e "${GREEN}✓${NC} Created build.gradle.kts for ${feature}"
}

# Create all modules
for feature in invoicing expense payment reporting; do
    case $feature in
        invoicing) port=9092 ;;
        expense) port=9093 ;;
        payment) port=9094 ;;
        reporting) port=9095 ;;
    esac

    create_dir_structure "${feature}"
    create_build_gradle "${feature}" "${port}"
done

echo -e "\n${GREEN}All feature modules created successfully!${NC}"
echo -e "Next steps:"
echo -e "  1. Update settings.gradle.kts"
echo -e "  2. Create Application.kt for each module"
echo -e "  3. Create RpcClientConfig.kt for each module"
echo -e "  4. Create routes and services"
echo -e "  5. Create Dockerfiles"
