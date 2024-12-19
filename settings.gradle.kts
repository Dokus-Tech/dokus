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

include(":shared:api")
include(":shared:configuration")
include(":shared:domain")

include(":server:gateway")
include(":server:contacts")
include(":server:contacts-api")
include(":server:identity")
include(":server:identity-api")
include(":server:documents")
include(":server:documents-api")
include(":server:prediction")
include(":server:prediction-api")
include(":server:simulation")
include(":server:simulation-api")
include(":server:database")
//include(":shared")
