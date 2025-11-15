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
        buildConfigField(STRING, "env", "prod")

        // External endpoints (for clients outside Docker) - Production
        buildConfigField(STRING, "authHost", "94.111.226.82")
        buildConfigField(INT, "authPort", "6091")
        buildConfigField(STRING, "invoicingHost", "94.111.226.82")
        buildConfigField(INT, "invoicingPort", "6092")
        buildConfigField(STRING, "expenseHost", "94.111.226.82")
        buildConfigField(INT, "expensePort", "6093")
        buildConfigField(STRING, "paymentHost", "94.111.226.82")
        buildConfigField(INT, "paymentPort", "6094")
        buildConfigField(STRING, "reportingHost", "94.111.226.82")
        buildConfigField(INT, "reportingPort", "6095")
        buildConfigField(STRING, "auditHost", "94.111.226.82")
        buildConfigField(INT, "auditPort", "6096")
        buildConfigField(STRING, "bankingHost", "94.111.226.82")
        buildConfigField(INT, "bankingPort", "6097")

        // RabbitMQ Configuration - Production
        buildConfigField(STRING, "rabbitmqHost", "rabbitmq-prod")
        buildConfigField(INT, "rabbitmqPort", "5672")
        buildConfigField(STRING, "rabbitmqUsername", "dokus")
        buildConfigField(STRING, "rabbitmqPassword", "prodrabbitpass")
        buildConfigField(STRING, "rabbitmqVirtualHost", "/dokus")

        // Internal endpoints (for inter-service communication in Docker) - Production
        buildConfigField(STRING, "authInternalHost", "auth-service-prod")
        buildConfigField(INT, "authInternalPort", "6091")
        buildConfigField(STRING, "invoicingInternalHost", "invoicing-service-prod")
        buildConfigField(INT, "invoicingInternalPort", "6092")
        buildConfigField(STRING, "expenseInternalHost", "expense-service-prod")
        buildConfigField(INT, "expenseInternalPort", "6093")
        buildConfigField(STRING, "paymentInternalHost", "payment-service-prod")
        buildConfigField(INT, "paymentInternalPort", "6094")
        buildConfigField(STRING, "reportingInternalHost", "reporting-service-prod")
        buildConfigField(INT, "reportingInternalPort", "6095")
        buildConfigField(STRING, "auditInternalHost", "audit-service-prod")
        buildConfigField(INT, "auditInternalPort", "6096")
        buildConfigField(STRING, "bankingInternalHost", "banking-service-prod")
        buildConfigField(INT, "bankingInternalPort", "6097")
    }
    defaultConfigs("prod") {}
    defaultConfigs("dev") {
        buildConfigField(STRING, "env", "dev")

        // External endpoints (for clients outside Docker) - Development
        buildConfigField(STRING, "authHost", "10.13.4.103")
        buildConfigField(INT, "authPort", "7091")
        buildConfigField(STRING, "invoicingHost", "10.13.4.103")
        buildConfigField(INT, "invoicingPort", "7092")
        buildConfigField(STRING, "expenseHost", "10.13.4.103")
        buildConfigField(INT, "expensePort", "7093")
        buildConfigField(STRING, "paymentHost", "10.13.4.103")
        buildConfigField(INT, "paymentPort", "7094")
        buildConfigField(STRING, "reportingHost", "10.13.4.103")
        buildConfigField(INT, "reportingPort", "7095")
        buildConfigField(STRING, "auditHost", "10.13.4.103")
        buildConfigField(INT, "auditPort", "7096")
        buildConfigField(STRING, "bankingHost", "10.13.4.103")
        buildConfigField(INT, "bankingPort", "7097")

        // RabbitMQ Configuration - Development
        buildConfigField(STRING, "rabbitmqHost", "rabbitmq-dev")
        buildConfigField(INT, "rabbitmqPort", "5672")
        buildConfigField(STRING, "rabbitmqUsername", "dokus")
        buildConfigField(STRING, "rabbitmqPassword", "devrabbitpass")
        buildConfigField(STRING, "rabbitmqVirtualHost", "/dokus")

        // Internal endpoints (for inter-service communication in Docker) - Development
        buildConfigField(STRING, "authInternalHost", "auth-service-dev")
        buildConfigField(INT, "authInternalPort", "7091")
        buildConfigField(STRING, "invoicingInternalHost", "invoicing-service-dev")
        buildConfigField(INT, "invoicingInternalPort", "7092")
        buildConfigField(STRING, "expenseInternalHost", "expense-service-dev")
        buildConfigField(INT, "expenseInternalPort", "7093")
        buildConfigField(STRING, "paymentInternalHost", "payment-service-dev")
        buildConfigField(INT, "paymentInternalPort", "7094")
        buildConfigField(STRING, "reportingInternalHost", "reporting-service-dev")
        buildConfigField(INT, "reportingInternalPort", "7095")
        buildConfigField(STRING, "auditInternalHost", "audit-service-dev")
        buildConfigField(INT, "auditInternalPort", "7096")
        buildConfigField(STRING, "bankingInternalHost", "banking-service-dev")
        buildConfigField(INT, "bankingInternalPort", "7097")
    }
    defaultConfigs("local") {
        buildConfigField(STRING, "env", "local")

        // External endpoints (same as internal for local development)
        buildConfigField(STRING, "authHost", "0.0.0.0")
        buildConfigField(INT, "authPort", "7091")
        buildConfigField(STRING, "invoicingHost", "0.0.0.0")
        buildConfigField(INT, "invoicingPort", "7092")
        buildConfigField(STRING, "expenseHost", "0.0.0.0")
        buildConfigField(INT, "expensePort", "7093")
        buildConfigField(STRING, "paymentHost", "0.0.0.0")
        buildConfigField(INT, "paymentPort", "7094")
        buildConfigField(STRING, "reportingHost", "0.0.0.0")
        buildConfigField(INT, "reportingPort", "7095")
        buildConfigField(STRING, "auditHost", "0.0.0.0")
        buildConfigField(INT, "auditPort", "7096")
        buildConfigField(STRING, "bankingHost", "0.0.0.0")
        buildConfigField(INT, "bankingPort", "7097")

        // RabbitMQ Configuration - Local
        buildConfigField(STRING, "rabbitmqHost", "localhost")
        buildConfigField(INT, "rabbitmqPort", "5672")
        buildConfigField(STRING, "rabbitmqUsername", "dokus")
        buildConfigField(STRING, "rabbitmqPassword", "devrabbitpass")
        buildConfigField(STRING, "rabbitmqVirtualHost", "/dokus")

        // Internal endpoints (for inter-service communication in Docker)
        buildConfigField(STRING, "authInternalHost", "auth-service-dev")
        buildConfigField(INT, "authInternalPort", "7091")
        buildConfigField(STRING, "invoicingInternalHost", "invoicing-service-dev")
        buildConfigField(INT, "invoicingInternalPort", "7092")
        buildConfigField(STRING, "expenseInternalHost", "expense-service-dev")
        buildConfigField(INT, "expenseInternalPort", "7093")
        buildConfigField(STRING, "paymentInternalHost", "payment-service-dev")
        buildConfigField(INT, "paymentInternalPort", "7094")
        buildConfigField(STRING, "reportingInternalHost", "reporting-service-dev")
        buildConfigField(INT, "reportingInternalPort", "7095")
        buildConfigField(STRING, "auditInternalHost", "audit-service-dev")
        buildConfigField(INT, "auditInternalPort", "7096")
        buildConfigField(STRING, "bankingInternalHost", "banking-service-dev")
        buildConfigField(INT, "bankingInternalPort", "7097")
    }
    targetConfigs("local") {
        create("wasmJs") {
            buildConfigField(STRING, "authHost", "localhost")
            buildConfigField(STRING, "invoicingHost", "localhost")
            buildConfigField(STRING, "expenseHost", "localhost")
            buildConfigField(STRING, "paymentHost", "localhost")
            buildConfigField(STRING, "reportingHost", "localhost")
            buildConfigField(STRING, "auditHost", "localhost")
            buildConfigField(STRING, "bankingHost", "localhost")
        }
    }
}
