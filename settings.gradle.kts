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
include(":application:home")
include(":application:dashboard")
include(":application:contacts")
include(":application:cashflow")
include(":application:simulation")
include(":application:inventory")
include(":application:banking")
include(":application:profile")
include(":foundation:ui")

include(":application:core")
include(":foundation:platform")
include(":application:repository")
include(":application:navigation")

include(":foundation:domain")
include(":foundation:apispec")
