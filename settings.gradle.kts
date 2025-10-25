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

include(":features:auth:backend")
include(":features:auth:presentation")
include(":features:auth:data")

include(":features:invoicing:backend")
include(":features:expense:backend")
include(":features:payment:backend")
include(":features:reporting:backend")

include(":foundation:domain")
include(":foundation:database")
include(":foundation:sstorage")
