package tech.dokus.backend.services.business

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class BusinessWebsiteRankerTest {
    private val ranker = BusinessWebsiteRanker()

    @Test
    fun `accepts candidate only when score is strictly above 70`() {
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

        assertTrue(result.accepted)
        assertNotNull(result.bestCandidate)
        assertTrue((result.bestCandidate?.score ?: 0) > 70)
    }

    @Test
    fun `score of 70 is rejected`() {
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
        assertFalse(result.accepted)
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
    }
}
