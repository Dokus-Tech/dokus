package tech.dokus.backend.services.business

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BusinessWebsiteRankerTest {
    private val ranker = BusinessWebsiteRanker()

    @Test
    fun `score above 70 is verified`() {
        val context = WebsiteRankingContext(
            companyName = "Invoid Vision",
            vatNumber = "BE0123456789",
            city = "Antwerp",
            postalCode = "2000",
            email = "hello@invoid.vision",
            phone = "+32 3 123 45 67"
        )

        val strongCandidate = WebsiteCandidateInput(
            url = "https://invoid.vision",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://invoid.vision",
                    title = "Invoid Vision - Finance Automation",
                    description = "Invoid Vision helps accountants automate invoice workflows.",
                    textContent = "Invoid Vision BE0123456789 Antwerp 2000 +32 3 123 45 67 hello@invoid.vision",
                    structuredDataSnippets = listOf("{\"@type\":\"Organization\",\"name\":\"Invoid Vision\"}"),
                    emails = listOf("hello@invoid.vision"),
                    phones = listOf("+32 3 123 45 67"),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Invoid Vision BE", listOf(strongCandidate))

        assertEquals(WebsiteRankingDecision.VERIFIED, result.decision)
        assertTrue(result.accepted)
        assertNotNull(result.bestCandidate)
        assertTrue((result.bestCandidate?.score ?: 0) > 70)
    }

    @Test
    fun `score of 70 is suggested`() {
        val context = WebsiteRankingContext(
            companyName = "Acme Logistics",
            vatNumber = null,
            city = "Brussels",
            postalCode = "1000",
            email = "info@acme.io",
            phone = null
        )

        val candidate = WebsiteCandidateInput(
            url = "https://acme.io",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://acme.io",
                    title = "Acme Logistics",
                    description = null,
                    textContent = "Acme Logistics based in Brussels 1000. Reach us at info@acme.io",
                    structuredDataSnippets = emptyList(),
                    emails = listOf("info@acme.io"),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Acme Logistics BE", listOf(candidate))

        assertEquals(70, result.bestCandidate?.score)
        assertEquals(WebsiteRankingDecision.SUGGESTED, result.decision)
        assertTrue(result.accepted)
    }

    @Test
    fun `tie break prefers more hard identity hits before path depth and rank`() {
        val context = WebsiteRankingContext(
            companyName = "Invoid Vision",
            vatNumber = "BE0123456789",
            city = "Antwerp",
            postalCode = "2000",
            email = "hello@invoid.vision",
            phone = null
        )

        val candidateA = WebsiteCandidateInput(
            url = "https://invoid-vision.io/deep/page",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://invoid-vision.io/deep/page",
                    title = "Invoid Vision",
                    description = "Automation software",
                    textContent = "Invoid Vision builds automation software.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val candidateB = WebsiteCandidateInput(
            url = "https://official-example.com",
            searchRank = 2,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://official-example.com",
                    title = "Solutions",
                    description = null,
                    textContent = "BE0123456789 Antwerp 2000",
                    structuredDataSnippets = listOf("{\"@type\":\"Organization\",\"vatID\":\"BE0123456789\"}"),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Invoid Vision BE", listOf(candidateA, candidateB))

        assertEquals(50, result.bestCandidate?.score)
        assertEquals("https://official-example.com", result.bestCandidate?.url)
        assertEquals(2, result.bestCandidate?.hardIdentityHits)
        assertEquals(WebsiteRankingDecision.SUGGESTED, result.decision)
    }

    @Test
    fun `kbc acronym matches domain token`() {
        val context = WebsiteRankingContext(companyName = "KBC Bank NV")
        val candidate = WebsiteCandidateInput(
            url = "https://www.kbc.com/en.html",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://www.kbc.com/en.html",
                    title = "KBC Bank",
                    description = "KBC financial services",
                    textContent = "KBC provides banking services.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "KBC Bank BE", listOf(candidate))

        val domainSignal = result.bestCandidate?.signals?.firstOrNull {
            it.signal == WebsiteRankingSignal.DOMAIN_COMPANY_MATCH
        } ?: error("Expected DOMAIN_COMPANY_MATCH signal")
        assertTrue(domainSignal.passed)
    }

    @Test
    fun `brand matching uses alias and search metadata`() {
        val context = WebsiteRankingContext(companyName = "Coolblue België N.V.")
        val candidate = WebsiteCandidateInput(
            url = "https://www.coolblue.be",
            searchRank = 1,
            searchTitle = "Coolblue België | Elektronica",
            searchSnippet = "Coolblue België webshop",
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://www.coolblue.be",
                    title = "Welcome",
                    description = null,
                    textContent = "Best products and delivery.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Coolblue België BE", listOf(candidate))

        val brandSignal = result.bestCandidate?.signals?.firstOrNull {
            it.signal == WebsiteRankingSignal.BRAND_TEXT_MATCH
        } ?: error("Expected BRAND_TEXT_MATCH signal")
        assertTrue(brandSignal.passed)
    }

    @Test
    fun `contact corroboration adapts when only one signal is available`() {
        val context = WebsiteRankingContext(
            companyName = "Acme",
            city = "Brussels"
        )
        val candidate = WebsiteCandidateInput(
            url = "https://acme.example",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://acme.example",
                    title = "Acme",
                    description = null,
                    textContent = "Acme has offices in Brussels.",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Acme Brussels", listOf(candidate))

        val corroborationSignal = result.bestCandidate?.signals?.firstOrNull {
            it.signal == WebsiteRankingSignal.CONTACT_CORROBORATION
        } ?: error("Expected CONTACT_CORROBORATION signal")
        assertTrue(corroborationSignal.passed)
    }

    @Test
    fun `vat digits fallback matches even when country prefix is omitted`() {
        val context = WebsiteRankingContext(
            companyName = "Invoid Vision",
            vatNumber = "BE0777887045"
        )
        val candidate = WebsiteCandidateInput(
            url = "https://invoid.vision",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://invoid.vision",
                    title = "Invoid Vision",
                    description = null,
                    textContent = "Company number 0777887045",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Invoid Vision BE", listOf(candidate))

        val vatSignal = result.bestCandidate?.signals?.firstOrNull {
            it.signal == WebsiteRankingSignal.VAT_MATCH
        } ?: error("Expected VAT_MATCH signal")
        assertTrue(vatSignal.passed)
    }

    @Test
    fun `score below 50 is rejected`() {
        val context = WebsiteRankingContext(companyName = "Acme")
        val weakCandidate = WebsiteCandidateInput(
            url = "https://example.org",
            searchRank = 1,
            pages = listOf(
                CrawledBusinessPage(
                    url = "https://example.org",
                    title = "Example",
                    description = null,
                    textContent = "Generic content",
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    links = emptyList(),
                    logoCandidates = emptyList()
                )
            )
        )

        val result = ranker.rank(context, "Acme BE", listOf(weakCandidate))

        assertFalse(result.accepted)
        assertEquals(WebsiteRankingDecision.REJECTED, result.decision)
    }
}
