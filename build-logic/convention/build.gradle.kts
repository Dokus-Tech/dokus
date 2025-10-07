plugins {
    `kotlin-dsl`
}

group = "ai.dokus.buildlogic"

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.android.gradlePlugin)
}

gradlePlugin {}
