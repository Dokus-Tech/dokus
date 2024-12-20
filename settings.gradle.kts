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
include(":application:repository")

include(":shared:configuration")
include(":shared:domain")
include(":shared:contacts-api")
include(":shared:identity-api")
include(":shared:documents-api")
include(":shared:prediction-api")
include(":shared:simulation-api")

include(":server:common")
include(":server:gateway")
include(":server:contacts")
include(":server:identity")
include(":server:documents")
include(":server:prediction")
include(":server:simulation")
include(":server:database")
//include(":shared")
