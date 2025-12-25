import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.buildKonfig)
    id("dokus.versioning")
}
val appVersion: ai.dokus.convention.AppVersionExtension by project.extensions

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
            api(libs.ktor.resources)  // Type-safe routing definitions

            implementation(libs.ktor.client.core)

            implementation(projects.foundation.platform)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
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
        // Version info - name comes from git tag during releases (e.g., "1.2.3")
        buildConfigField(STRING, "appVersionName", appVersion.name)
        buildConfigField(INT, "appVersionCode", appVersion.code.toString())

        buildConfigField(STRING, "env", "cloud")

        // Gateway endpoint (for external clients via Traefik) - Cloud
        buildConfigField(STRING, "gatewayHost", "app.dokus.tech")
        buildConfigField(INT, "gatewayPort", "443")
        buildConfigField(STRING, "gatewayProtocol", "https")

        // RabbitMQ Configuration - Cloud (credentials from env vars at runtime)
        buildConfigField(STRING, "rabbitmqHost", "rabbitmq")
        buildConfigField(INT, "rabbitmqPort", "5672")
        buildConfigField(STRING, "rabbitmqVirtualHost", "/dokus")

        // Internal endpoints (for inter-service communication in Docker) - Cloud
        buildConfigField(STRING, "authInternalHost", "auth-service")
        buildConfigField(INT, "authInternalPort", "8080")
        buildConfigField(STRING, "cashflowInternalHost", "cashflow-service")
        buildConfigField(INT, "cashflowInternalPort", "8080")
        buildConfigField(STRING, "paymentInternalHost", "payment-service")
        buildConfigField(INT, "paymentInternalPort", "8080")
        buildConfigField(STRING, "bankingInternalHost", "banking-service")
        buildConfigField(INT, "bankingInternalPort", "8080")
        buildConfigField(STRING, "contactsInternalHost", "contacts-service")
        buildConfigField(INT, "contactsInternalPort", "8080")
        buildConfigField(STRING, "mediaInternalHost", "media-service")
        buildConfigField(INT, "mediaInternalPort", "8080")
    }
    defaultConfigs("local") {
        buildConfigField(STRING, "env", "local")
    }
}
