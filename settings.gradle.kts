rootProject.name = "Dokus"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            @Suppress("UnstableApiUsage")
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

include(":composeApp")
include(":backendApp")

include(":foundation:design-system")

include(":foundation:app-common")
include(":foundation:platform")
include(":foundation:navigation")
include(":foundation:ktor-common")
include(":foundation:database")
include(":foundation:peppol-core")

// Backend library (shared by backendApp)
include(":features:ai:backend")
include(":foundation:ocr")

// Auth domain for shared types
include(":features:auth:presentation")
include(":features:auth:data")
include(":features:auth:domain")

// Frontend modules
include(":features:cashflow:data")
include(":features:cashflow:presentation")
include(":features:contacts:data")
include(":features:contacts:domain")
include(":features:contacts:presentation")

include(":foundation:domain")
include(":foundation:sstorage")
