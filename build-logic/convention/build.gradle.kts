plugins {
    `kotlin-dsl`
}

group = "ai.dokus.buildlogic"

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("versioning") {
            id = "dokus.versioning"
            implementationClass = "ai.dokus.convention.VersioningPlugin"
        }
    }
}
