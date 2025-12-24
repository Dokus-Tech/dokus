import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
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
    jvmToolchain(17)

    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.all {
            linkerOpts("-lsqlite3")
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(projects.foundation.domain)
            implementation(projects.foundation.navigation)
            implementation(projects.foundation.appCommon)
            implementation(projects.foundation.designSystem)
            implementation(projects.foundation.platform)
            implementation(projects.features.auth.domain)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.koin.compose)

            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.bundles.sqldelight)
            implementation(libs.kotlinx.datetime)

            // Ktor client for repository API calls
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.resources)
        }
        desktopMain.dependencies {
        }
    }
}

android {
    namespace = "ai.dokus.app.contacts"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

sqldelight {
    databases {
        create("ContactsCacheDatabase") {
            packageName.set("ai.dokus.app.contacts.cache")
            generateAsync.set(true)
        }
    }
}
