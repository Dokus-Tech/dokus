import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinxRpcPlugin)
    alias(libs.plugins.buildKonfig)
}

kotlin {
    jvmToolchain(17)

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    iosX64()
    iosArm64()
    iosSimulatorArm64()

    jvm()

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }
    
    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.datetime)
            api(libs.kotlinx.serialization)

            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.rpc.core)
            implementation(libs.kotlinx.rpc.krpc.serialization.json)

            implementation(projects.foundation.platform)
        }
    }
}

android {
    namespace = "ai.dokus.foundation.domain"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

buildkonfig {
    packageName = "ai.dokus.foundation.domain.config"
    defaultConfigs {
        buildConfigField(STRING, "env", "cloud")

        // External endpoints (for clients outside Docker) - Cloud
        buildConfigField(STRING, "authHost", "94.111.226.82")
        buildConfigField(INT, "authPort", "6091")
        buildConfigField(STRING, "cashflowHost", "94.111.226.82")
        buildConfigField(INT, "cashflowPort", "6098")
        buildConfigField(STRING, "paymentHost", "94.111.226.82")
        buildConfigField(INT, "paymentPort", "6094")
        buildConfigField(STRING, "reportingHost", "94.111.226.82")
        buildConfigField(INT, "reportingPort", "6095")
        buildConfigField(STRING, "auditHost", "94.111.226.82")
        buildConfigField(INT, "auditPort", "6096")
        buildConfigField(STRING, "bankingHost", "94.111.226.82")
        buildConfigField(INT, "bankingPort", "6097")

        // RabbitMQ Configuration - Cloud (credentials from env vars at runtime)
        buildConfigField(STRING, "rabbitmqHost", "rabbitmq")
        buildConfigField(INT, "rabbitmqPort", "5672")
        buildConfigField(STRING, "rabbitmqVirtualHost", "/dokus")

        // Internal endpoints (for inter-service communication in Docker) - Cloud
        buildConfigField(STRING, "authInternalHost", "auth-service")
        buildConfigField(INT, "authInternalPort", "6091")
        buildConfigField(STRING, "cashflowInternalHost", "cashflow-service")
        buildConfigField(INT, "cashflowInternalPort", "6098")
        buildConfigField(STRING, "paymentInternalHost", "payment-service")
        buildConfigField(INT, "paymentInternalPort", "6094")
        buildConfigField(STRING, "reportingInternalHost", "reporting-service")
        buildConfigField(INT, "reportingInternalPort", "6095")
        buildConfigField(STRING, "auditInternalHost", "audit-service")
        buildConfigField(INT, "auditInternalPort", "6096")
        buildConfigField(STRING, "bankingInternalHost", "banking-service")
        buildConfigField(INT, "bankingInternalPort", "6097")
    }
    defaultConfigs("local") {
        buildConfigField(STRING, "env", "local")

        // External endpoints (same as internal for local development)
        buildConfigField(STRING, "authHost", "0.0.0.0")
        buildConfigField(INT, "authPort", "7091")
        buildConfigField(STRING, "cashflowHost", "0.0.0.0")
        buildConfigField(INT, "cashflowPort", "7098")
        buildConfigField(STRING, "paymentHost", "0.0.0.0")
        buildConfigField(INT, "paymentPort", "7094")
        buildConfigField(STRING, "reportingHost", "0.0.0.0")
        buildConfigField(INT, "reportingPort", "7095")
        buildConfigField(STRING, "auditHost", "0.0.0.0")
        buildConfigField(INT, "auditPort", "7096")
        buildConfigField(STRING, "bankingHost", "0.0.0.0")
        buildConfigField(INT, "bankingPort", "7097")

        // RabbitMQ Configuration - Local (credentials from env vars at runtime)
        buildConfigField(STRING, "rabbitmqHost", "localhost")
        buildConfigField(INT, "rabbitmqPort", "5672")
        buildConfigField(STRING, "rabbitmqVirtualHost", "/dokus")

        // Internal endpoints (for inter-service communication in Docker)
        buildConfigField(STRING, "authInternalHost", "auth-service-local")
        buildConfigField(INT, "authInternalPort", "7091")
        buildConfigField(STRING, "cashflowInternalHost", "cashflow-service-local")
        buildConfigField(INT, "cashflowInternalPort", "7098")
        buildConfigField(STRING, "paymentInternalHost", "payment-service-local")
        buildConfigField(INT, "paymentInternalPort", "7094")
        buildConfigField(STRING, "reportingInternalHost", "reporting-service-local")
        buildConfigField(INT, "reportingInternalPort", "7095")
        buildConfigField(STRING, "auditInternalHost", "audit-service-local")
        buildConfigField(INT, "auditInternalPort", "7096")
        buildConfigField(STRING, "bankingInternalHost", "banking-service-local")
        buildConfigField(INT, "bankingInternalPort", "7097")
    }
    targetConfigs("local") {
        create("wasmJs") {
            buildConfigField(STRING, "authHost", "localhost")
            buildConfigField(STRING, "cashflowHost", "localhost")
            buildConfigField(STRING, "paymentHost", "localhost")
            buildConfigField(STRING, "reportingHost", "localhost")
            buildConfigField(STRING, "auditHost", "localhost")
            buildConfigField(STRING, "bankingHost", "localhost")
        }
    }
}
