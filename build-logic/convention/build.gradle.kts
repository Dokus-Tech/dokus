plugins {
    `kotlin-dsl`
}

group = "ai.thepredict.buildlogic"

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.android.gradlePlugin)
}

gradlePlugin {}
