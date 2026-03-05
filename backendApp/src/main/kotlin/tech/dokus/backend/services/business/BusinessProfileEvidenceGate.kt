package tech.dokus.backend.services.business

import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.BusinessProfileEvidenceCheck
import tech.dokus.domain.enums.BusinessProfileVerificationState
import java.net.URI
import java.util.Locale

/** Domains that are aggregators/social sites, not real company websites. Shared between evidence gate and worker. */
val BLOCKED_AGGREGATOR_DOMAINS = listOf(
    "linkedin.com",
    "facebook.com",
    "instagram.com",
    "x.com",
    "twitter.com",
    "wikipedia.org",
    "yellowpages",
    "yelp.",
    "trustpilot.",
    "crunchbase.com",
    "opencorporates.com",
    "kompass.com",
    "bloomberg.com",
    "dnb.com",
)

fun isAggregatorOrSocialHost(host: String): Boolean =
    host.isBlank() || BLOCKED_AGGREGATOR_DOMAINS.any { token -> host.contains(token) }

@Serializable
data class EvidenceCheckDecision(
    val check: BusinessProfileEvidenceCheck,
    val passed: Boolean,
    val score: Int,
    val details: String? = null,
)

enum class EvidenceGateOutcome {
    AUTO_PERSIST,
    PERSIST_AS_SUGGESTED,
    SKIP,
}

data class BusinessProfileEvidenceInput(
    val companyName: String,
    val companyVatNumber: String? = null,
    val companyCountry: String? = null,
    val companyCity: String? = null,
    val companyPostalCode: String? = null,
    val companyEmail: String? = null,
    val companyPhone: String? = null,
    val candidateWebsiteUrl: String,
    val llmConfidence: Double,
    val searchResultUrls: List<String>,
    val crawledText: String,
    val structuredDataSnippets: List<String>,
)

data class BusinessProfileEvidenceResult(
    val outcome: EvidenceGateOutcome,
    val verificationState: BusinessProfileVerificationState,
    val evidenceScore: Int,
    val checks: List<EvidenceCheckDecision>,
)

class BusinessProfileEvidenceGate {
    fun evaluate(input: BusinessProfileEvidenceInput): BusinessProfileEvidenceResult {
        val host = extractHost(input.candidateWebsiteUrl).orEmpty().lowercase(Locale.US)
        val rootHost = host.removePrefix("www.")
        val normalizedText = normalize(input.crawledText)
        val normalizedStructured = input.structuredDataSnippets.joinToString("\n").let(::normalize)
        val normalizedCompanyName = normalize(input.companyName)
        val normalizedVat = normalizeVat(input.companyVatNumber)
        val companyTokens = companyNameTokens(input.companyName)
        val normalizedEmail = input.companyEmail?.trim()?.lowercase(Locale.US)
        val normalizedPhone = normalizePhone(input.companyPhone)
        val countryCode = input.companyCountry?.trim()?.lowercase(Locale.US)

        val checks = mutableListOf<EvidenceCheckDecision>()
        fun add(check: BusinessProfileEvidenceCheck, passed: Boolean, score: Int, details: String? = null) {
            checks += EvidenceCheckDecision(
                check = check,
                passed = passed,
                score = if (passed) score else 0,
                details = details
            )
        }

        val vatFoundOnSite = !normalizedVat.isNullOrBlank() &&
            (normalizedText.contains(normalizedVat) || normalizedStructured.contains(normalizedVat))
        add(
            check = BusinessProfileEvidenceCheck.VatFoundOnSite,
            passed = vatFoundOnSite,
            score = 30,
            details = if (vatFoundOnSite) "VAT was found on crawled pages." else null
        )

        val domainContainsCompanyName = companyTokens.any { token ->
            token.length >= 4 && rootHost.contains(token)
        }
        add(
            check = BusinessProfileEvidenceCheck.DomainContainsCompanyName,
            passed = domainContainsCompanyName,
            score = 30,
            details = if (domainContainsCompanyName) "Domain contains company token." else null
        )

        val structuredDataMatch = normalizedStructured.isNotBlank() && (
            (!normalizedVat.isNullOrBlank() && normalizedStructured.contains(normalizedVat)) ||
                companyTokens.count { token -> token.length >= 4 && normalizedStructured.contains(token) } >= 2 ||
                (normalizedCompanyName.isNotBlank() && normalizedStructured.contains(normalizedCompanyName))
            )
        add(
            check = BusinessProfileEvidenceCheck.StructuredDataMatch,
            passed = structuredDataMatch,
            score = 30,
            details = if (structuredDataMatch) "Structured data matches identity markers." else null
        )

        val addressMatch = run {
            val city = input.companyCity?.let(::normalize).orEmpty()
            val postal = input.companyPostalCode?.let(::normalize).orEmpty()
            when {
                city.isNotBlank() && postal.isNotBlank() -> normalizedText.contains(city) && normalizedText.contains(postal)
                city.isNotBlank() -> normalizedText.contains(city)
                postal.isNotBlank() -> normalizedText.contains(postal)
                else -> false
            }
        }
        add(
            check = BusinessProfileEvidenceCheck.AddressMatch,
            passed = addressMatch,
            score = 15,
            details = if (addressMatch) "Address markers matched crawled content." else null
        )

        val phoneOrEmailMatch = run {
            val emailMatch = !normalizedEmail.isNullOrBlank() && normalizedText.contains(normalizedEmail)
            val phoneMatch = !normalizedPhone.isNullOrBlank() && normalizedText.contains(normalizedPhone)
            emailMatch || phoneMatch
        }
        add(
            check = BusinessProfileEvidenceCheck.PhoneOrEmailMatch,
            passed = phoneOrEmailMatch,
            score = 15,
            details = if (phoneOrEmailMatch) "Email or phone from source appears on site." else null
        )

        val tldCountryMatch = run {
            val tld = rootHost.substringAfterLast('.', "")
            !countryCode.isNullOrBlank() && tld.equals(countryCode, ignoreCase = true)
        }
        add(
            check = BusinessProfileEvidenceCheck.DomainTldCountryMatch,
            passed = tldCountryMatch,
            score = 15,
            details = if (tldCountryMatch) "Domain TLD aligns with company country code." else null
        )

        val notAggregator = isNonAggregator(rootHost)
        add(
            check = BusinessProfileEvidenceCheck.NotAggregator,
            passed = notAggregator,
            score = 15,
            details = if (notAggregator) "Domain is not in known aggregator/social list." else null
        )

        val llmConfidenceHigh = input.llmConfidence >= 0.85
        add(
            check = BusinessProfileEvidenceCheck.LlmConfidenceHigh,
            passed = llmConfidenceHigh,
            score = 5,
            details = if (llmConfidenceHigh) "LLM confidence >= 0.85." else null
        )

        val singleDominant = isSingleDominantResult(
            candidateWebsiteUrl = input.candidateWebsiteUrl,
            searchResultUrls = input.searchResultUrls
        )
        add(
            check = BusinessProfileEvidenceCheck.SingleDominantResult,
            passed = singleDominant,
            score = 5,
            details = if (singleDominant) "Search results contain a dominant candidate domain." else null
        )

        val score = checks.sumOf { it.score }
        val hasHardCheck = checks.any {
            it.passed && (
                it.check == BusinessProfileEvidenceCheck.VatFoundOnSite ||
                    it.check == BusinessProfileEvidenceCheck.DomainContainsCompanyName ||
                    it.check == BusinessProfileEvidenceCheck.StructuredDataMatch
                )
        }

        val outcome = when {
            score >= 60 && hasHardCheck -> EvidenceGateOutcome.AUTO_PERSIST
            score in 30..59 -> EvidenceGateOutcome.PERSIST_AS_SUGGESTED
            else -> EvidenceGateOutcome.SKIP
        }
        val state = when (outcome) {
            EvidenceGateOutcome.AUTO_PERSIST -> BusinessProfileVerificationState.Verified
            EvidenceGateOutcome.PERSIST_AS_SUGGESTED -> BusinessProfileVerificationState.Suggested
            EvidenceGateOutcome.SKIP -> BusinessProfileVerificationState.Unset
        }

        return BusinessProfileEvidenceResult(
            outcome = outcome,
            verificationState = state,
            evidenceScore = score,
            checks = checks
        )
    }

    private fun normalize(value: String?): String {
        return value
            .orEmpty()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun normalizeVat(vat: String?): String? {
        val normalized = normalize(vat)
        return normalized.takeIf { it.isNotBlank() }
    }

    private fun normalizePhone(phone: String?): String? {
        val normalized = phone
            .orEmpty()
            .replace(Regex("[^0-9+]"), "")
            .trim()
        return normalized.takeIf { it.length >= 7 }
    }

    private fun companyNameTokens(name: String): List<String> {
        val stopWords = setOf(
            "the", "and", "for", "group", "holding", "company", "co", "inc", "ltd",
            "llc", "bv", "nv", "sa", "srl", "gmbh", "sas", "sprl"
        )
        return name.lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .filter { it !in stopWords }
            .distinct()
    }

    private fun extractHost(url: String): String? {
        return runCatching { URI(url).host }.getOrNull()
    }

    private fun isSingleDominantResult(candidateWebsiteUrl: String, searchResultUrls: List<String>): Boolean {
        if (searchResultUrls.isEmpty()) return false
        val candidateHost = extractHost(candidateWebsiteUrl)?.removePrefix("www.") ?: return false
        val hosts = searchResultUrls.mapNotNull { extractHost(it)?.removePrefix("www.") }
        if (hosts.isEmpty()) return false
        val candidateCount = hosts.count { it == candidateHost }
        val isTop1 = hosts.firstOrNull() == candidateHost
        return candidateCount >= 2 || (candidateCount == 1 && isTop1 && hosts.distinct().size <= 2)
    }

    private fun isNonAggregator(host: String): Boolean = !isAggregatorOrSocialHost(host)
}
