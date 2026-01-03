plugins {
    `kotlin-dsl`
}

group = "tech.dokus.buildlogic"

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.android.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("versioning") {
            id = "dokus.versioning"
            implementationClass = "tech.dokus.convention.VersioningPlugin"
        }
    }
}
