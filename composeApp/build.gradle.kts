import ai.dokus.utils.WebCacheBuster
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeHotReload)
    id("dokus.versioning")
}

// Access version from convention plugin
val appVersion: ai.dokus.convention.AppVersionExtension by project.extensions
val bundleIds: ai.dokus.convention.BundleIdsExtension by project.extensions

// Version configuration from build-logic/Versions.kt
// CI sets BUILD to git commit count before building
val versionCode = appVersion.code
val versionName = appVersion.name

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
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            binaryOption("bundleId", bundleIds.ios)
            binaryOption("bundleVersion", versionCode.toString())
            binaryOption("bundleShortVersionString", versionName)

            linkerOpts("-lsqlite3")
        }

        iosTarget.binaries.all {
            linkerOpts("-lsqlite3")
        }
    }

    jvm("desktop")

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName = "composeApp"
        browser {
            val rootDirPath = project.rootDir.path
            val projectDirPath = project.projectDir.path
            commonWebpackConfig {
                outputFileName = "composeApp.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(rootDirPath)
                        add(projectDirPath)
                    }
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(projects.foundation.appCommon)
            implementation(projects.foundation.designSystem)
            implementation(projects.foundation.platform)
            implementation(projects.foundation.navigation)

            implementation(projects.features.auth.data)
            implementation(projects.features.auth.presentation)
            implementation(projects.features.auth.domain)

            implementation(projects.features.cashflow.data)
            implementation(projects.features.cashflow.presentation)

            implementation(projects.features.contacts.presentation)

            implementation(projects.foundation.domain)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(compose.materialIconsExtended)

            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.viewmodel)
            implementation(libs.koin.navigation)

            implementation(libs.navigation.compose)
            implementation(libs.lifecycle.viewmodel.compose)

            implementation(libs.materialKolor)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.sqldelight.jvm)  // Required for packaged DMG
        }
    }
}

android {
    namespace = "ai.dokus.app"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = bundleIds.android
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = this@Build_gradle.versionCode
        versionName = this@Build_gradle.versionName
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

compose.desktop {

    application {
        mainClass = "ai.dokus.app.MainKt"
        val macAppStore = (project.findProperty("compose.desktop.mac.appStore") as String?)?.toBoolean() ?: false
        val macSigningEnabled = (project.findProperty("mac.signing.enabled") as String?)?.toBoolean() ?: false
        val macSigningIdentity = project.findProperty("mac.signing.identity") as String?

        buildTypes {
            release {
                proguard {
                    obfuscate = false
                    optimize = true
                    isEnabled = false
                }
            }
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Pkg, TargetFormat.Msi, TargetFormat.Deb)

            modules("java.sql")  // Required for SQLDelight/JDBC

            packageName = "Dokus"
            packageVersion = versionName
            vendor = "Invoid Vision"

            macOS {
                dockName = "D[#]kus"
                appStore = macAppStore
                bundleID = bundleIds.macos
                copyright = "Invoid Vision 2025"
                description = "Dokus financial document management"
                signing {
                    sign.set(macSigningEnabled)
                    if (macSigningIdentity != null) {
                        identity.set(macSigningIdentity)
                    }
                }
            }

            windows {
                perUserInstall = true
            }
        }
    }
}

tasks.named("wasmJsBrowserDistribution") {
    doLast {
        WebCacheBuster.updateIndexFileForDistribution(project)
    }
}
