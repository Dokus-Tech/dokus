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
val appVersion: tech.dokus.convention.AppVersionExtension by project.extensions

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
    namespace = "tech.dokus.domain"
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
    packageName = "tech.dokus.domain.config"
    defaultConfigs {
        // Version info - name comes from git tag during releases (e.g., "1.2.3")
        buildConfigField(STRING, "appVersionName", appVersion.name)
        buildConfigField(INT, "appVersionCode", appVersion.code.toString())

        buildConfigField(STRING, "env", "cloud")
    }
    defaultConfigs("local") {
        buildConfigField(STRING, "env", "local")
    }
}
