package tech.dokus.features.ai.extraction

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import tech.dokus.features.ai.services.DocumentImageService

/**
 * Tool for recording extraction results as fixture baselines.
 *
 * Usage:
 *   ./gradlew :features:ai:backend:recordExtractionFixtures
 *   ./gradlew :features:ai:backend:recordExtractionFixtures -Pfixture=belgian-standard
 */
fun main(args: Array<String>) = runBlocking {
    val fixtureId = args.firstOrNull()
    val recorder = FixtureRecorder()

    if (fixtureId != null) {
        println("Recording fixture: $fixtureId")
        recorder.recordSingle(fixtureId)
    } else {
        println("Recording all fixtures...")
        recorder.recordAll()
    }
}

class FixtureRecorder {
    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
    }
    private val fixtureLoader = FixtureLoader()
    private val documentImageService = DocumentImageService()

    suspend fun recordAll() {
        val fixtures = fixtureLoader.loadAllFixtures()
        println("Found ${fixtures.size} fixture(s) to record")

        fixtures.forEach { fixture ->
            println("  Processing: ${fixture.id}")
            try {
                recordSingle(fixture.id)
                println("    SUCCESS")
            } catch (e: Exception) {
                println("    FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    suspend fun recordSingle(fixtureId: String) {
        val fixture = fixtureLoader.loadFixtureById(fixtureId)
            ?: throw IllegalArgumentException("Fixture not found: $fixtureId")

        val config = fixtureLoader.loadConfig(fixture)
        val documentBytes = fixtureLoader.loadDocument(fixture)
        val mimeType = config.mimeType.ifEmpty { fixtureLoader.getMimeType(fixture) }

        // Get document images
        val images = documentImageService.getDocumentImages(
            documentBytes = documentBytes,
            mimeType = mimeType,
            maxPages = config.maxPages ?: 10,
            dpi = config.dpi ?: 150
        )

        println("    Document has ${images.totalPages} pages, processed ${images.processedPages}")

        // Run extraction
        val extraction = runExtraction(images, config.documentType)

        // Build expected result
        val expected = ExpectedExtraction(
            documentType = config.documentType,
            extraction = extraction,
            minConfidence = 0.7
        )

        // Save to file
        val expectedFile = fixtureLoader.getExpectedFile(fixture)
        expectedFile.writeText(json.encodeToString(expected))

        println("    Saved: ${expectedFile.absolutePath}")
    }

    private suspend fun runExtraction(
        images: DocumentImageService.DocumentImages,
        documentType: String
    ): JsonElement {
        // TODO: Implement actual AI extraction
        // For now, create a placeholder result
        println("    [PLACEHOLDER] Real AI extraction not yet implemented")

        val placeholderJson = when (documentType.uppercase()) {
            "INVOICE" -> """
                {
                    "vendorName": "PLACEHOLDER - Run with AI service",
                    "invoiceNumber": "PLACEHOLDER",
                    "totalAmount": "0.00",
                    "confidence": 0.0
                }
            """.trimIndent()
            "BILL" -> """
                {
                    "vendorName": "PLACEHOLDER - Run with AI service",
                    "amount": "0.00",
                    "confidence": 0.0
                }
            """.trimIndent()
            "RECEIPT" -> """
                {
                    "merchantName": "PLACEHOLDER - Run with AI service",
                    "totalAmount": "0.00",
                    "confidence": 0.0
                }
            """.trimIndent()
            "EXPENSE" -> """
                {
                    "merchantName": "PLACEHOLDER - Run with AI service",
                    "totalAmount": "0.00",
                    "confidence": 0.0
                }
            """.trimIndent()
            else -> """
                {
                    "confidence": 0.0
                }
            """.trimIndent()
        }

        return Json.parseToJsonElement(placeholderJson)
    }
}
