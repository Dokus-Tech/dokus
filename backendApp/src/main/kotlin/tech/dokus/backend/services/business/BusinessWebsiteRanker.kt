package tech.dokus.backend.services.business

import kotlinx.serialization.Serializable
import tech.dokus.foundation.backend.utils.loggerFor
import java.net.URI
import java.text.Normalizer
import java.util.Locale

private const val WebsiteVerifiedThreshold = 70
private const val WebsiteSuggestedThreshold = 50

@Serializable
enum class WebsiteRankingSignal(
    val points: Int,
    val hardIdentity: Boolean,
) {
    DOMAIN_COMPANY_MATCH(points = 30, hardIdentity = true),
    BRAND_TEXT_MATCH(points = 20, hardIdentity = false),
    STRUCTURED_ORG_MATCH(points = 15, hardIdentity = true),
    CONTACT_CORROBORATION(points = 20, hardIdentity = false),
    VAT_MATCH(points = 15, hardIdentity = true),
}

@Serializable
data class WebsiteRankingSignalDecision(
    val signal: WebsiteRankingSignal,
    val passed: Boolean,
    val points: Int,
    val details: String? = null,
)

@Serializable
data class RankedWebsiteCandidateEvidence(
    val url: String,
    val score: Int,
    val searchRank: Int,
    val pathDepth: Int,
    val hardIdentityHits: Int,
    val signals: List<WebsiteRankingSignalDecision>,
)

@Serializable
data class WebsiteRankingEvidence(
    val query: String,
    val decision: WebsiteRankingDecision,
    val verifiedThreshold: Int,
    val suggestedThreshold: Int,
    val threshold: Int,
    val accepted: Boolean,
    val selectedUrl: String? = null,
    val selectedScore: Int = 0,
    val candidates: List<RankedWebsiteCandidateEvidence> = emptyList(),
)

@Serializable
enum class WebsiteRankingDecision {
    VERIFIED,
    SUGGESTED,
    REJECTED,
}

data class WebsiteRankingContext(
    val companyName: String,
    val vatNumber: String? = null,
    val country: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
)

data class WebsiteCandidateInput(
    val url: String,
    val searchRank: Int,
    val pages: List<CrawledBusinessPage>,
    val searchTitle: String? = null,
    val searchSnippet: String? = null,
)

data class RankedWebsiteCandidate(
    val url: String,
    val searchRank: Int,
    val pathDepth: Int,
    val score: Int,
    val hardIdentityHits: Int,
    val signals: List<WebsiteRankingSignalDecision>,
)

data class WebsiteRankingResult(
    val decision: WebsiteRankingDecision,
    val accepted: Boolean,
    val bestCandidate: RankedWebsiteCandidate?,
    val allCandidates: List<RankedWebsiteCandidate>,
    val evidence: WebsiteRankingEvidence,
)

class BusinessWebsiteRanker {
    private val logger = loggerFor()

    fun rank(
        context: WebsiteRankingContext,
        searchQuery: String,
        candidates: List<WebsiteCandidateInput>
    ): WebsiteRankingResult {
        val ranked = candidates.map { candidate -> rankCandidate(context, candidate) }
            .sortedWith(
                compareByDescending<RankedWebsiteCandidate> { it.score }
                    .thenByDescending { it.hardIdentityHits }
                    .thenBy { it.pathDepth }
                    .thenBy { it.searchRank }
            )

        val best = ranked.firstOrNull()
        val bestScore = best?.score ?: 0
        val decision = when {
            bestScore > WebsiteVerifiedThreshold -> WebsiteRankingDecision.VERIFIED
            bestScore >= WebsiteSuggestedThreshold -> WebsiteRankingDecision.SUGGESTED
            else -> WebsiteRankingDecision.REJECTED
        }
        val accepted = decision != WebsiteRankingDecision.REJECTED

        val evidence = WebsiteRankingEvidence(
            query = searchQuery,
            decision = decision,
            verifiedThreshold = WebsiteVerifiedThreshold,
            suggestedThreshold = WebsiteSuggestedThreshold,
            threshold = WebsiteVerifiedThreshold,
            accepted = accepted,
            selectedUrl = if (accepted) best?.url else null,
            selectedScore = bestScore,
            candidates = ranked
                .sortedBy { it.searchRank }
                .map { candidate ->
                    RankedWebsiteCandidateEvidence(
                        url = candidate.url,
                        score = candidate.score,
                        searchRank = candidate.searchRank,
                        pathDepth = candidate.pathDepth,
                        hardIdentityHits = candidate.hardIdentityHits,
                        signals = candidate.signals
                    )
                }
        )

        logger.debug(
            "Website ranking completed query='{}', decision={}, selectedScore={}, candidateCount={}",
            searchQuery,
            decision,
            bestScore,
            ranked.size
        )

        return WebsiteRankingResult(
            decision = decision,
            accepted = accepted,
            bestCandidate = best,
            allCandidates = ranked,
            evidence = evidence
        )
    }

    private fun rankCandidate(
        context: WebsiteRankingContext,
        candidate: WebsiteCandidateInput
    ): RankedWebsiteCandidate {
        val host = extractHost(candidate.url).orEmpty().removePrefix("www.")
        val rootHost = host.substringBefore(':')
        val pathDepth = pathDepth(candidate.url)

        val companyTokens = companyNameTokens(context.companyName)
        val acronymTokens = companyAcronymTokens(context.companyName)
        val normalizedCompany = normalize(context.companyName)
        val normalizedCompanyAliases = companyNameAliases(context.companyName)
        val normalizedVat = normalizeVat(context.vatNumber)
        val normalizedVatDigits = context.vatNumber?.filter { it.isDigit() }?.takeIf { it.length >= 8 }

        val plainText = candidate.pages
            .joinToString("\n") { page ->
                listOfNotNull(page.title, page.description, page.textContent)
                    .joinToString(" ")
            }
            .lowercase(Locale.US)
        val normalizedText = normalize(plainText)
        val normalizedSearchMetadata = normalize(
            listOfNotNull(candidate.searchTitle, candidate.searchSnippet).joinToString(" ")
        )
        val searchableNormalized = listOf(normalizedText, normalizedSearchMetadata)
            .filter { it.isNotBlank() }
            .joinToString(" ")

        val structuredRaw = candidate.pages.flatMap { it.structuredDataSnippets }
            .joinToString("\n")
        val structuredNormalized = normalize(structuredRaw)
        val digitsCombinedText = (plainText + " " + structuredRaw).replace(Regex("[^0-9]"), "")

        val pageEmails = candidate.pages.flatMap { it.emails }.map { it.lowercase(Locale.US) }
        val pagePhonesNormalized = candidate.pages
            .flatMap { it.phones }
            .mapNotNull(::normalizePhone)
        val hostLabels = rootHost
            .split(Regex("[.-]"))
            .filter { it.isNotBlank() }
        val searchableTokens = (companyTokens + acronymTokens)
            .filter { it.length >= 3 }
            .distinct()

        val signalDecisions = buildList {
            val domainCompanyMatchStrongToken = companyTokens.any { token ->
                token.length >= 4 && rootHost.contains(token)
            }
            val domainCompanyMatchAcronym = acronymTokens.any { token ->
                token.length in 2..3 && hostLabels.any { label ->
                    label == token || label.contains(token)
                }
            }
            val domainCompanyMatch = domainCompanyMatchStrongToken || domainCompanyMatchAcronym
            addDecision(
                signal = WebsiteRankingSignal.DOMAIN_COMPANY_MATCH,
                passed = domainCompanyMatch,
                details = if (domainCompanyMatch) "Domain contains strong company token." else null
            )

            val tokenHitsInText = searchableTokens.count { token ->
                token.length >= 3 && searchableNormalized.contains(token)
            }
            val aliasFullMatch = normalizedCompanyAliases.any { alias ->
                alias.length >= 4 && searchableNormalized.contains(alias)
            }
            val brandTextMatch = (
                normalizedCompany.isNotBlank() && normalizedCompany.length >= 6 && searchableNormalized.contains(normalizedCompany)
                ) || aliasFullMatch || tokenHitsInText >= 2 || (domainCompanyMatch && tokenHitsInText >= 1)
            addDecision(
                signal = WebsiteRankingSignal.BRAND_TEXT_MATCH,
                passed = brandTextMatch,
                details = if (brandTextMatch) "Brand tokens present in titles/meta/body text." else null
            )

            val tokenHitsInStructured = searchableTokens.count { token ->
                token.length >= 3 && structuredNormalized.contains(token)
            }
            val aliasStructuredMatch = normalizedCompanyAliases.any { alias ->
                alias.length >= 4 && structuredNormalized.contains(alias)
            }
            val structuredOrgMatch = (
                normalizedCompany.isNotBlank() && structuredNormalized.contains(normalizedCompany)
                ) || aliasStructuredMatch || tokenHitsInStructured >= 2 ||
                (!normalizedVat.isNullOrBlank() && structuredNormalized.contains(normalizedVat))
            addDecision(
                signal = WebsiteRankingSignal.STRUCTURED_ORG_MATCH,
                passed = structuredOrgMatch,
                details = if (structuredOrgMatch) "Structured data matches company identity." else null
            )

            val emailDomain = context.email
                ?.substringAfter('@', missingDelimiterValue = "")
                ?.lowercase(Locale.US)
                ?.removePrefix("www.")
                ?.takeIf { it.isNotBlank() }
            val companyEmailHost = emailDomain?.substringBefore(':')
            val companyEmailDomainRoot = companyEmailHost?.let(::rootDomain)
            val candidateDomainRoot = rootDomain(rootHost)
            val emailDomainMatch = !companyEmailDomainRoot.isNullOrBlank() &&
                companyEmailDomainRoot == candidateDomainRoot

            val pageDomainEmailMatch = pageEmails.any { email ->
                val domain = email.substringAfter('@', missingDelimiterValue = "")
                !domain.isBlank() && rootDomain(domain) == candidateDomainRoot
            }

            val normalizedPhone = normalizePhone(context.phone)
            val digitsText = plainText.replace(Regex("[^0-9+]"), "")
            val phoneMatch = !normalizedPhone.isNullOrBlank() &&
                (
                    digitsText.contains(normalizedPhone) ||
                        pagePhonesNormalized.any { it.contains(normalizedPhone) || normalizedPhone.contains(it) }
                    )

            val cityMatch = context.city
                ?.trim()
                ?.lowercase(Locale.US)
                ?.takeIf { it.isNotBlank() }
                ?.let { city -> plainText.contains(city) }
                ?: false
            val postalMatch = context.postalCode
                ?.trim()
                ?.lowercase(Locale.US)
                ?.takeIf { it.isNotBlank() }
                ?.let { postal -> plainText.contains(postal) }
                ?: false

            val availableCorroborationSignals = listOf(
                !context.email.isNullOrBlank(),
                !context.phone.isNullOrBlank(),
                !context.city.isNullOrBlank(),
                !context.postalCode.isNullOrBlank()
            ).count { it }
            val corroborationCount = listOf(
                emailDomainMatch || pageDomainEmailMatch,
                phoneMatch,
                cityMatch,
                postalMatch
            ).count { it }
            val requiredCorroborationSignals = when {
                availableCorroborationSignals >= 3 -> 2
                availableCorroborationSignals in 1..2 -> 1
                else -> Int.MAX_VALUE
            }
            val contactCorroboration = corroborationCount >= requiredCorroborationSignals
            addDecision(
                signal = WebsiteRankingSignal.CONTACT_CORROBORATION,
                passed = contactCorroboration,
                details = if (contactCorroboration) "Contact/location signals corroborate candidate domain." else null
            )

            val vatMatch = !normalizedVat.isNullOrBlank() &&
                (
                    normalizedText.contains(normalizedVat) ||
                        structuredNormalized.contains(normalizedVat) ||
                        (!normalizedVatDigits.isNullOrBlank() && digitsCombinedText.contains(normalizedVatDigits))
                    )
            addDecision(
                signal = WebsiteRankingSignal.VAT_MATCH,
                passed = vatMatch,
                details = if (vatMatch) "VAT found in crawled content." else null
            )
        }

        val score = signalDecisions.sumOf { it.points }
        val hardIdentityHits = signalDecisions.count { it.passed && it.signal.hardIdentity }

        return RankedWebsiteCandidate(
            url = candidate.url,
            searchRank = candidate.searchRank,
            pathDepth = pathDepth,
            score = score,
            hardIdentityHits = hardIdentityHits,
            signals = signalDecisions
        )
    }

    private fun MutableList<WebsiteRankingSignalDecision>.addDecision(
        signal: WebsiteRankingSignal,
        passed: Boolean,
        details: String?
    ) {
        add(
            WebsiteRankingSignalDecision(
                signal = signal,
                passed = passed,
                points = if (passed) signal.points else 0,
                details = details
            )
        )
    }

    private fun companyNameTokens(name: String): List<String> {
        val stopWords = setOf(
            "the", "and", "for", "group", "holding", "company", "co", "inc", "ltd",
            "llc", "bv", "nv", "sa", "srl", "gmbh", "sas", "sprl"
        )
        return foldDiacritics(name).lowercase(Locale.US)
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split(Regex("\\s+"))
            .map { it.trim() }
            .filter { it.length >= 3 }
            .filter { it !in stopWords }
            .distinct()
    }

    private fun companyAcronymTokens(name: String): List<String> {
        return Regex("[A-Za-z0-9]+")
            .findAll(name)
            .map { it.value.trim() }
            .filter { token -> token.length in 2..3 && token == token.uppercase(Locale.US) }
            .map { token -> token.lowercase(Locale.US) }
            .distinct()
            .toList()
    }

    private fun companyNameAliases(name: String): List<String> {
        val raw = normalize(name)
        val stripped = normalize(stripLegalSuffixes(name))
        return listOf(raw, stripped)
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun stripLegalSuffixes(name: String): String {
        val suffixes = setOf(
            "nv", "bv", "bvba", "srl", "sprl", "sa", "sas", "ag", "gmbh", "llc", "ltd", "inc", "corp"
        )
        val tokens = foldDiacritics(name)
            .trim()
            .split(Regex("\\s+"))
            .map { it.trim().trim(',', '.', ';', ':', '-', '_') }
            .filter { it.isNotBlank() }
            .toMutableList()
        while (tokens.isNotEmpty()) {
            val last = tokens.last()
                .lowercase(Locale.US)
                .replace(Regex("[^a-z0-9]"), "")
            if (last !in suffixes) break
            tokens.removeAt(tokens.lastIndex)
        }
        return tokens.joinToString(" ").ifBlank { name.trim() }
    }

    private fun foldDiacritics(value: String?): String {
        return Normalizer.normalize(value.orEmpty(), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
    }

    private fun normalize(value: String?): String {
        return foldDiacritics(value)
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9]"), "")
    }

    private fun normalizeVat(vat: String?): String? = normalize(vat).takeIf { it.isNotBlank() }

    private fun normalizePhone(phone: String?): String? {
        val normalized = phone.orEmpty().replace(Regex("[^0-9+]"), "").trim()
        return normalized.takeIf { it.length >= 7 }
    }

    private fun extractHost(url: String): String? = runCatching { URI(url).host }.getOrNull()

    private fun pathDepth(url: String): Int {
        val path = runCatching { URI(url).path }.getOrNull().orEmpty().trim('/').trim()
        if (path.isBlank()) return 0
        return path.split('/').count { it.isNotBlank() }
    }

    private fun rootDomain(host: String): String {
        val normalized = host.removePrefix("www.").lowercase(Locale.US)
        val parts = normalized.split('.').filter { it.isNotBlank() }
        if (parts.size <= 2) return normalized
        return parts.takeLast(2).joinToString(".")
    }
}
