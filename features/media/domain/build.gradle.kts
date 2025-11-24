import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.kotlinxRpcPlugin)
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
        browser {
            testTask {
                enabled = false
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.foundation.domain)

            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.datetime)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)

            implementation(libs.bundles.rpc.client)
        }
        androidMain.dependencies {
            implementation(libs.koin.android)
        }
        jvmMain.dependencies {
            implementation(libs.kotlinx.coroutines.swing)
        }
        wasmJsMain.dependencies {
            implementation("org.jetbrains.kotlinx:kotlinx-browser:0.2")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

android {
    namespace = "ai.dokus.app.media.domain"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
