package tech.dokus.backend.services.business

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BusinessProfileEvidenceGateTest {
    private val gate = BusinessProfileEvidenceGate()

    @Test
    fun `auto persist when score is high with hard evidence`() {
        val result = gate.evaluate(
            BusinessProfileEvidenceInput(
                companyName = "Acme Logistics",
                companyVatNumber = "BE0123456789",
                companyCountry = "BE",
                companyCity = "Brussels",
                companyPostalCode = "1000",
                candidateWebsiteUrl = "https://acme-logistics.be",
                llmConfidence = 0.92,
                searchResultUrls = listOf(
                    "https://acme-logistics.be",
                    "https://acme-logistics.be/about"
                ),
                crawledText = "Acme Logistics BE0123456789 Brussels 1000 contact@acme-logistics.be",
                structuredDataSnippets = listOf("""{"@type":"Organization","name":"Acme Logistics","vatID":"BE0123456789"}""")
            )
        )

        assertEquals(EvidenceGateOutcome.AUTO_PERSIST, result.outcome)
        assertTrue(result.evidenceScore >= 60)
    }

    @Test
    fun `persist as suggested for medium evidence`() {
        val result = gate.evaluate(
            BusinessProfileEvidenceInput(
                companyName = "Beta Consulting",
                companyCountry = "BE",
                candidateWebsiteUrl = "https://bcs.be",
                llmConfidence = 0.9,
                searchResultUrls = listOf("https://bcs.be"),
                crawledText = "Beta Consulting official page",
                structuredDataSnippets = emptyList()
            )
        )

        assertEquals(EvidenceGateOutcome.PERSIST_AS_SUGGESTED, result.outcome)
        assertEquals(40, result.evidenceScore)
    }

    @Test
    fun `skip when evidence is too low`() {
        val result = gate.evaluate(
            BusinessProfileEvidenceInput(
                companyName = "Gamma Studio",
                candidateWebsiteUrl = "https://directory.example.com/gamma",
                llmConfidence = 0.4,
                searchResultUrls = listOf("https://directory.example.com/gamma"),
                crawledText = "Directory listing",
                structuredDataSnippets = emptyList()
            )
        )

        assertEquals(EvidenceGateOutcome.SKIP, result.outcome)
    }
}
