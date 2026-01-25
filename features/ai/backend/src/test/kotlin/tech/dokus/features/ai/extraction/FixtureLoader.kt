package tech.dokus.features.ai.extraction

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Loads test fixtures from the extraction-fixtures resource directory.
 */
class FixtureLoader {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val fixturesDir: File by lazy {
        val resourceUrl = javaClass.classLoader.getResource("extraction-fixtures")
            ?: throw IllegalStateException("extraction-fixtures directory not found in test resources")
        File(resourceUrl.toURI())
    }

    /**
     * Load the index of all fixtures.
     */
    fun loadIndex(): FixtureIndex {
        val indexFile = File(fixturesDir, "index.json")
        require(indexFile.exists()) { "index.json not found in extraction-fixtures" }
        return json.decodeFromString(indexFile.readText())
    }

    /**
     * Load all enabled fixtures.
     */
    fun loadAllFixtures(): List<FixtureReference> {
        return loadIndex().fixtures.filter { it.enabled }
    }

    /**
     * Find a fixture by its ID.
     */
    fun loadFixtureById(fixtureId: String): FixtureReference? {
        return loadIndex().fixtures.find { it.id == fixtureId }
    }

    /**
     * Load the configuration for a fixture.
     */
    fun loadConfig(fixture: FixtureReference): FixtureConfig {
        val configFile = File(fixturesDir, "${fixture.path}/config.json")
        return if (configFile.exists()) {
            json.decodeFromString(configFile.readText())
        } else {
            // Return defaults based on fixture metadata
            FixtureConfig(documentType = fixture.documentType)
        }
    }

    /**
     * Load the expected extraction result for a fixture.
     */
    fun loadExpected(fixture: FixtureReference): ExpectedExtraction {
        val expectedFile = File(fixturesDir, "${fixture.path}/expected.json")
        require(expectedFile.exists()) { "expected.json not found for fixture: ${fixture.id}" }
        return json.decodeFromString(expectedFile.readText())
    }

    /**
     * Load the document bytes for a fixture.
     */
    fun loadDocument(fixture: FixtureReference): ByteArray {
        val fixtureDir = File(fixturesDir, fixture.path)
        val documentFile = fixtureDir.listFiles()?.find { file ->
            file.name.startsWith("document.") && !file.name.endsWith(".json")
        } ?: throw IllegalStateException("No document file found for fixture: ${fixture.id}")
        return documentFile.readBytes()
    }

    /**
     * Get the document file for a fixture.
     */
    fun getDocumentFile(fixture: FixtureReference): File {
        val fixtureDir = File(fixturesDir, fixture.path)
        return fixtureDir.listFiles()?.find { file ->
            file.name.startsWith("document.") && !file.name.endsWith(".json")
        } ?: throw IllegalStateException("No document file found for fixture: ${fixture.id}")
    }

    /**
     * Get the expected.json file path for recording.
     */
    fun getExpectedFile(fixture: FixtureReference): File {
        return File(fixturesDir, "${fixture.path}/expected.json")
    }

    /**
     * Get the MIME type from document file extension.
     */
    fun getMimeType(fixture: FixtureReference): String {
        val documentFile = getDocumentFile(fixture)
        return when (documentFile.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "png" -> "image/png"
            "jpg", "jpeg" -> "image/jpeg"
            "webp" -> "image/webp"
            "gif" -> "image/gif"
            "bmp" -> "image/bmp"
            "tiff", "tif" -> "image/tiff"
            else -> "application/octet-stream"
        }
    }
}
