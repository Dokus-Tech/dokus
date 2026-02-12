import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinPluginSerialization) apply false
    alias(libs.plugins.composeHotReload) apply false
    alias(libs.plugins.buildKonfig) apply false
    alias(libs.plugins.detekt) apply false
}

subprojects {
    val detektConfig = files("$rootDir/config/detekt/detekt.yml")
    fun org.gradle.api.Project.configureDetekt() {
        extensions.configure<DetektExtension> {
            buildUponDefaultConfig = true
            config.setFrom(detektConfig)
            baseline = file("$rootDir/config/detekt/baseline${path.replace(":", "_")}.xml")
            autoCorrect = true
            source.setFrom(
                "src/commonMain/kotlin",
                "src/androidMain/kotlin",
                "src/iosMain/kotlin",
                "src/desktopMain/kotlin",
                "src/wasmJsMain/kotlin",
                "src/jvmMain/kotlin",
                "src/main/kotlin"
            )
        }
        dependencies {
            add("detektPlugins", libs.detekt.formatting)
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        configureDetekt()
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        configureDetekt()

        // Exclude generated code from KMP detekt tasks
        tasks.matching { it.name.startsWith("detekt") }.configureEach {
            if (this is Detekt) {
                exclude { it.file.path.contains("/build/") }
            }
        }
    }
    pluginManager.withPlugin("org.jetbrains.kotlin.android") {
        apply(plugin = "io.gitlab.arturbosch.detekt")
        configureDetekt()
    }
}

tasks.register("checkKotlinFileSize") {
    group = "verification"
    description = "Fails if a Kotlin source file exceeds 450 lines (excluding generated/build output)."

    doLast {
        val maxLines = 450
        val offenders = mutableListOf<String>()
        val allowlistFile = file("$rootDir/config/quality/line-length-allowlist.txt")
        val allowlist = if (allowlistFile.exists()) {
            allowlistFile.readLines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()
        } else {
            emptySet()
        }
        fileTree(rootDir) {
            include("**/*.kt")
            exclude("**/build/**", "**/generated/**", "**/out/**", "**/.gradle/**")
        }.files.forEach { file ->
            val count = file.readLines().size
            if (count > maxLines) {
                val relativePath = file.relativeTo(rootDir).invariantSeparatorsPath
                if (!allowlist.contains(relativePath)) {
                    offenders.add("$relativePath ($count)")
                }
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Kotlin files exceed $maxLines lines:\\n" + offenders.joinToString("\\n")
            )
        }
    }
}

tasks.register("checkNoNavInComponents") {
    group = "verification"
    description = "Fails if presentation/components or presentation/screen imports navigation APIs."

    val forbidden = listOf(
        Regex("import\\s+androidx\\.navigation"),
        Regex("import\\s+androidx\\.navigation\\.compose"),
        Regex("import\\s+tech\\.dokus\\.navigation\\.local\\.LocalNavController"),
        Regex("import\\s+tech\\.dokus\\.navigation\\.navigateTo"),
    )

    doLast {
        val offenders = mutableListOf<String>()
        fileTree(rootDir) {
            include("features/**/presentation/**/components/**/*.kt")
            include("features/**/presentation/**/screen/**/*.kt")
            exclude("**/build/**", "**/generated/**", "**/out/**", "**/.gradle/**")
        }.files.forEach { file ->
            val text = file.readText()
            if (forbidden.any { it.containsMatchIn(text) }) {
                offenders.add(file.relativeTo(rootDir).toString())
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                "Navigation APIs are forbidden in presentation/components:\\n" +
                    offenders.joinToString("\\n")
            )
        }
    }
}

tasks.register("detektAll") {
    group = "verification"
    description = "Runs detekt on all subprojects."
    dependsOn(subprojects.mapNotNull { it.tasks.findByName("detekt") })
}

tasks.register("detektKmp") {
    group = "verification"
    description = "Runs detekt on all KMP commonMain source sets."
    subprojects.forEach { subproject ->
        subproject.tasks.matching {
            it.name == "detektMetadataCommonMain"
        }.forEach { dependsOn(it) }
    }
}

tasks.register("checkAll") {
    group = "verification"
    description = "Runs detekt and custom guardrails."
    dependsOn("detektAll", "detektKmp", "checkKotlinFileSize", "checkNoNavInComponents")
}

// Screenshot Testing Tasks (Paparazzi)
tasks.register("clearScreenshots") {
    group = "verification"
    description = "Delete all screenshot baseline images."
    doLast {
        val snapshotDirs = listOf(
            "composeApp/src/test/snapshots/images",
            "foundation/aura/src/test/snapshots/images",
            "features/auth/presentation/src/test/snapshots/images",
            "features/cashflow/presentation/src/test/snapshots/images",
            "features/contacts/presentation/src/test/snapshots/images"
        )
        snapshotDirs.forEach { dir ->
            val folder = file(dir)
            if (folder.exists()) {
                val deleted = folder.listFiles()?.filter { it.extension == "png" }?.count { it.delete() } ?: 0
                println("Deleted $deleted screenshots from $dir")
            }
        }
    }
}

tasks.register("recordScreenshots") {
    group = "verification"
    description = "Record new baseline screenshots for all modules."
    dependsOn(
        ":composeApp:recordPaparazziDebug",
        ":foundation:aura:recordPaparazziDebug",
        ":features:auth:presentation:recordPaparazziDebug",
        ":features:cashflow:presentation:recordPaparazziDebug",
        ":features:contacts:presentation:recordPaparazziDebug"
    )
}

tasks.register("verifyScreenshots") {
    group = "verification"
    description = "Verify screenshots against baselines for all modules."
    dependsOn(
        ":composeApp:verifyPaparazziDebug",
        ":foundation:aura:verifyPaparazziDebug",
        ":features:auth:presentation:verifyPaparazziDebug",
        ":features:cashflow:presentation:verifyPaparazziDebug",
        ":features:contacts:presentation:verifyPaparazziDebug"
    )
}

tasks.register("screenshotTests") {
    group = "verification"
    description = "Run all Paparazzi screenshot tests across modules."
    dependsOn(
        ":composeApp:testDebugUnitTest",
        ":foundation:aura:testDebugUnitTest",
        ":features:auth:presentation:testDebugUnitTest",
        ":features:cashflow:presentation:testDebugUnitTest",
        ":features:contacts:presentation:testDebugUnitTest"
    )
}
