import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
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

    iosX64()
    iosArm64()
    iosSimulatorArm64()

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
            implementation(projects.features.cashflow.domain)
            implementation(projects.features.auth.domain)
            implementation(projects.features.contacts.domain)
            implementation(projects.features.contacts.presentation)
            implementation(projects.foundation.domain)

            implementation(projects.foundation.navigation)
            implementation(projects.foundation.appCommon)
            implementation(projects.foundation.aura)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.koin.compose)

            implementation(compose.preview)

            implementation(libs.calf.core)
            implementation(libs.calf.filePicker)

            // FlowMVI for state management
            implementation(libs.flowmvi.core)
            implementation(libs.flowmvi.compose)

            // DateTime
            implementation(libs.kotlinx.datetime)
        }
        desktopMain.dependencies {
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
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
    namespace = "tech.dokus.features.cashflow"
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

@OptIn(com.github.takahirom.roborazzi.ExperimentalRoborazziApi::class)
roborazzi {
    outputDir.set(file("src/androidUnitTest/snapshots"))
    generateComposePreviewRobolectricTests {
        enable = true
        packages = listOf("tech.dokus.features.cashflow.presentation")
        includePrivatePreviews = true
        testerQualifiedClassName = "tech.dokus.testing.DokusComposePreviewTester"
        useScanOptionParametersInTester = true
        robolectricConfig = mapOf(
            "sdk" to "[34]",
            "qualifiers" to "RobolectricDeviceQualifiers.Pixel5"
        )
    }
}
