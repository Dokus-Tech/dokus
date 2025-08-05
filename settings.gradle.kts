rootProject.name = "ThePredict"
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
include(":application:ui")
include(":application:ui-preview")

include(":application:core")
include(":application:platform")
include(":application:repository")
include(":application:navigation")

include(":shared:configuration")
include(":shared:domain")
include(":shared:apispec")
//
//include(":server:common")
//include(":server:gateway")
//include(":server:contacts")
//include(":server:identity")
//include(":server:documents")
//include(":server:prediction")
//include(":server:simulation")
//include(":server:database")
