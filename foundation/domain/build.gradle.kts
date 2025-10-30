import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
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
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}

buildkonfig {
    packageName = "ai.dokus.foundation.domain.config"
    defaultConfigs {
        buildConfigField(STRING, "env", "prod")

        // External endpoints (for clients outside Docker)
        buildConfigField(STRING, "authHost", "10.13.4.103")
        buildConfigField(INT, "authPort", "8091")

        // Internal endpoints (for inter-service communication in Docker)
        buildConfigField(STRING, "authInternalHost", "auth-service-dev")
        buildConfigField(INT, "authInternalPort", "8091")
    }
    defaultConfigs("prod") {}
    defaultConfigs("dev") {
        buildConfigField(STRING, "env", "dev")

        // External endpoints (for clients outside Docker)
        buildConfigField(STRING, "authHost", "10.13.4.103")
        buildConfigField(INT, "authPort", "9091")

        // Internal endpoints (for inter-service communication in Docker)
        buildConfigField(STRING, "authInternalHost", "auth-service-dev")
        buildConfigField(INT, "authInternalPort", "9091")
    }
    defaultConfigs("local") {
        buildConfigField(STRING, "env", "local")

        // External endpoints (same as internal for local development)
        buildConfigField(STRING, "authHost", "0.0.0.0")
        buildConfigField(INT, "authPort", "9091")

        // Internal endpoints (for inter-service communication in Docker)
        buildConfigField(STRING, "authInternalHost", "auth-service-dev")
        buildConfigField(INT, "authInternalPort", "9091")
    }
    targetConfigs("local") {
        create("wasmJs") {
            buildConfigField(STRING, "authHost", "localhost")
        }
    }
}
