rootProject.name = "ThePredict"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
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
include(":application:ui")
include(":application:onboarding")
include(":application:platform")
include(":shared:configuration")
include(":shared:domain")
include(":server:contacts")
include(":server:gateway")
include(":server:identity")
include(":server:documents")
include(":server:prediction")
include(":server:simulation")
include(":server:database")
//include(":shared")
