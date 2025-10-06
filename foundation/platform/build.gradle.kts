import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.BOOLEAN
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.STRING
import com.codingfeline.buildkonfig.compiler.FieldSpec.Type.INT
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.buildkonfig)
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
            implementation(libs.persistence.settings)
            api(libs.kermit)
        }
    }
}

android {
    namespace = "ai.dokus.foundation.platform"
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
    packageName = "ai.dokus.foundation.platform"

    // Make BuildKonfig public so it can be accessed from other modules
    exposeObjectWithName = "BuildConfig"

    defaultConfigs {
        // Debug configuration
        val isDebug = findProperty("DEBUG")?.toString()?.toBoolean() ?: false
        buildConfigField(BOOLEAN, "DEBUG", isDebug.toString())

        // API Endpoint configuration
        // Supports environment presets or custom values:
        // - Production (default): ./gradlew build
        // - Local development: ./gradlew build -PENV=local
        // - Android emulator: ./gradlew build -PENV=localAndroid
        // - Custom: ./gradlew build -PAPI_HOST=staging.example.com -PAPI_PORT=8080

        val env = findProperty("ENV")?.toString() ?: "production"

        val (apiHost, apiPort, isLocal) = when (env) {
            "local" -> Triple("127.0.0.1", 8000, true)
            "localAndroid" -> Triple("10.0.2.2", 8000, true)
            else -> Triple("api.dokus.ai", 443, false) // HTTPS default port
        }

        // Allow explicit overrides
        val finalHost = findProperty("API_HOST")?.toString() ?: apiHost
        val finalPort = findProperty("API_PORT")?.toString()?.toIntOrNull() ?: apiPort
        val finalIsLocal = findProperty("API_IS_LOCAL")?.toString()?.toBoolean()
            ?: (env != "production")

        buildConfigField(STRING, "API_HOST", finalHost)
        buildConfigField(INT, "API_PORT", finalPort.toString())
        buildConfigField(BOOLEAN, "API_IS_LOCAL", finalIsLocal.toString())
    }
}
