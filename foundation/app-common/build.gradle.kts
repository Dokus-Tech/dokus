import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.sqldelight)
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

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.ktor.client.cio)
            implementation(libs.sqldelight.android)
        }
        commonMain.dependencies {
            api(projects.foundation.platform)
            api(projects.foundation.domain)
            api(projects.foundation.navigation)
            api(projects.foundation.designSystem)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)

            implementation(libs.lifecycle.viewmodel.compose)

            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)

            api(project.dependencies.platform(libs.koin.bom))
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.viewmodel)
            api(libs.koin.navigation)

            implementation(libs.calf.permissions)
            implementation(libs.calf.filePicker)

            implementation(libs.bundles.sqldelight)
        }
        desktopMain.dependencies {
            implementation(libs.ktor.client.cio)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.jvm)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.native)
        }
        wasmJsMain.dependencies {
            implementation(libs.sqldelight.wasm)
            implementation(npm("@cashapp/sqldelight-sqljs-worker", "2.1.0"))
            implementation(npm("sql.js", "1.8.0"))
            implementation(libs.ktor.client.wasm)
        }
    }
}

android {
    namespace = "ai.dokus.app.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    lint {
        targetSdk = libs.versions.android.targetSdk.get().toInt()
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {

    application {
        buildTypes {
            release {
                proguard {
                    obfuscate = false
                    optimize = false
                    isEnabled = false
                }
            }
        }
    }
}
