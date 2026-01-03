package tech.dokus.utils

import org.gradle.api.Project
import java.io.File
import java.time.Instant

/**
 * Utility to add cache-busting version parameter to JavaScript files in HTML.
 * This ensures browsers fetch the latest version after deployments.
 */
object WebCacheBuster {
    fun updateIndexFileForDistribution(project: Project) {
        val indexFile = project.file("build/dist/wasmJs/productionExecutable/index.html")
        updateIndexFileForDistribution(indexFile.absolutePath)
    }

    fun updateIndexFileForDistribution(path: String) {
        val indexFile = File(path)

        if (!indexFile.exists()) {
            println("Warning: index.html not found at ${indexFile.absolutePath}")
            return
        }

        val version = Instant.now().epochSecond
        val content = indexFile.readText()

        // Update the script src to include version parameter
        val updatedContent = content.replace(
            """src="composeApp.js"""",
            """src="composeApp.js?v=$version""""
        )

        // Only write if content changed
        if (content != updatedContent) {
            indexFile.writeText(updatedContent)
            println("Updated ${indexFile.path} with version: $version")
            println("""Script tag now: <script type="application/javascript" src="composeApp.js?v=$version"></script>""")
        } else {
            println("index.html already has version parameter")
        }
    }
}
