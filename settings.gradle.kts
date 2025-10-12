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

include(":application:onboarding")
include(":application:dashboard")
include(":application:contacts")
include(":application:cashflow")
include(":application:simulation")
include(":application:inventory")
include(":application:banking")
include(":foundation:ui")

include(":application:core")
include(":foundation:platform")
include(":application:repository")
include(":foundation:navigation")
include(":foundation:ktor-common")

include(":features:auth:backend")
include(":features:auth:presentation")

include(":foundation:domain")
include(":foundation:apispec")
include(":foundation:database")
