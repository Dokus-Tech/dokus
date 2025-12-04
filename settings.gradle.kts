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

include(":foundation:design-system")

include(":foundation:app-common")
include(":foundation:platform")
include(":foundation:navigation")
include(":foundation:ktor-common")
include(":foundation:messaging")
include(":foundation:database")

include(":features:ai:backend")
include(":features:auth:backend")
include(":features:auth:presentation")
include(":features:auth:data")
include(":features:auth:domain")

include(":features:cashflow:backend")
include(":features:cashflow:data")
include(":features:cashflow:presentation")
include(":features:media:backend")
include(":features:media:data")
include(":features:media:presentation")
include(":features:media:domain")
include(":features:payment:backend")
include(":features:reporting:backend")
include(":features:audit:backend")
include(":features:banking:backend")
include(":features:peppol:backend")
include(":features:processor:backend")

include(":foundation:domain")
include(":foundation:sstorage")
