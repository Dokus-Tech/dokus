import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinPluginSerialization)
    alias(libs.plugins.roborazzi)
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
        browser {
            testTask {
                enabled = false
            }
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
        }
        commonMain.dependencies {
            implementation(projects.foundation.domain)
            implementation(projects.foundation.navigation)
            implementation(projects.foundation.appCommon)
            implementation(projects.foundation.aura)
            implementation(projects.foundation.platform)
            implementation(projects.features.auth.domain)
            implementation(projects.features.contacts.domain)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.koin.compose)

            implementation(compose.preview)

            implementation(libs.kotlinx.serialization)
            implementation(libs.kotlinx.coroutinesCore)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
            implementation(libs.flowmvi.test)
        }
        desktopMain.dependencies {
        }
        androidUnitTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlin.test.junit)
            implementation(libs.junit)
            implementation(libs.robolectric)
            implementation(libs.bundles.roborazzi)
            implementation(libs.bundles.roborazzi.scanner)
            implementation(libs.androidx.ui.test.junit4)
            implementation(projects.foundation.aura)
        }
    }
}

android {
    namespace = "tech.dokus.features.contacts"
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
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

@OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
roborazzi {
    outputDir.set(file("src/androidUnitTest/snapshots"))
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("tech.dokus.features.contacts.presentation")
        includePrivatePreviews = true
        testerQualifiedClassName = "tech.dokus.testing.DokusComposePreviewTester"
        useScanOptionParametersInTester = true
        robolectricConfig = mapOf(
            "sdk" to "[34]",
            "qualifiers" to "RobolectricDeviceQualifiers.Pixel5"
        )
    }
}
