package tech.dokus.backend.services.documents

import kotlinx.datetime.LocalDate
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentDraftData
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData

internal object PurposePeriodHeuristics {
    private val monthLookup = mapOf(
        // English
        "jan" to 1,
        "january" to 1,
        "feb" to 2,
        "february" to 2,
        "mar" to 3,
        "march" to 3,
        "apr" to 4,
        "april" to 4,
        "may" to 5,
        "jun" to 6,
        "june" to 6,
        "jul" to 7,
        "july" to 7,
        "aug" to 8,
        "august" to 8,
        "sep" to 9,
        "sept" to 9,
        "september" to 9,
        "oct" to 10,
        "october" to 10,
        "nov" to 11,
        "november" to 11,
        "dec" to 12,
        "december" to 12,
        // Dutch
        "januari" to 1,
        "februari" to 2,
        "maart" to 3,
        "mrt" to 3,
        "mei" to 5,
        "juni" to 6,
        "juli" to 7,
        "augustus" to 8,
        "okt" to 10,
        "oktober" to 10,
        "november" to 11,
        "december" to 12,
        // French
        "janvier" to 1,
        "fevrier" to 2,
        "février" to 2,
        "mars" to 3,
        "avril" to 4,
        "mai" to 5,
        "juin" to 6,
        "juillet" to 7,
        "aout" to 8,
        "août" to 8,
        "septembre" to 9,
        "octobre" to 10,
        "novembre" to 11,
        "decembre" to 12,
        "décembre" to 12
    )

    private val monthToken = monthLookup.keys
        .sortedByDescending { it.length }
        .joinToString("|") { Regex.escape(it) }

    private val rangeSep = "(?:-|–|—|to|tot|au|until)"

    private val yearMonthRangePattern = Regex(
        "\\b(20\\d{2})[-/](0[1-9]|1[0-2])\\s*$rangeSep\\s*(20\\d{2})[-/](0[1-9]|1[0-2])\\b",
        RegexOption.IGNORE_CASE
    )
    private val monthYearRangePattern = Regex(
        "\\b(0[1-9]|1[0-2])[/-](20\\d{2})\\s*$rangeSep\\s*(0[1-9]|1[0-2])[/-](20\\d{2})\\b",
        RegexOption.IGNORE_CASE
    )
    private val namedMonthRangeSameYearPattern = Regex(
        "(?i)\\b($monthToken)\\s*$rangeSep\\s*($monthToken)\\s*(20\\d{2})\\b"
    )
    private val namedMonthRangeWithYearsPattern = Regex(
        "(?i)\\b($monthToken)\\s*(20\\d{2})\\s*$rangeSep\\s*($monthToken)\\s*(20\\d{2})\\b"
    )
    private val yearMonthPattern = Regex(
        "\\b(20\\d{2})[-/](0[1-9]|1[0-2])\\b",
        RegexOption.IGNORE_CASE
    )
    private val monthYearPattern = Regex(
        "\\b(0[1-9]|1[0-2])[/-](20\\d{2})\\b",
        RegexOption.IGNORE_CASE
    )
    private val namedMonthPattern = Regex("(?i)\\b($monthToken)\\s*(20\\d{2})\\b")

    fun detectServicePeriodStart(draftData: DocumentDraftData): LocalDate? {
        val candidates = extractCandidateTexts(draftData)
        for (candidate in candidates) {
            parseStartDate(candidate)?.let { return it }
        }
        return null
    }

    private fun parseStartDate(text: String): LocalDate? {
        val normalized = text.trim()
        if (normalized.isBlank()) return null

        yearMonthRangePattern.find(normalized)?.let { match ->
            return toDate(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        }
        monthYearRangePattern.find(normalized)?.let { match ->
            return toDate(match.groupValues[2].toInt(), match.groupValues[1].toInt())
        }
        namedMonthRangeWithYearsPattern.find(normalized)?.let { match ->
            val month = monthLookup[match.groupValues[1].lowercase()]
            val year = match.groupValues[2].toIntOrNull()
            if (month != null && year != null) return toDate(year, month)
        }
        namedMonthRangeSameYearPattern.find(normalized)?.let { match ->
            val month = monthLookup[match.groupValues[1].lowercase()]
            val year = match.groupValues[3].toIntOrNull()
            if (month != null && year != null) return toDate(year, month)
        }
        yearMonthPattern.find(normalized)?.let { match ->
            return toDate(match.groupValues[1].toInt(), match.groupValues[2].toInt())
        }
        monthYearPattern.find(normalized)?.let { match ->
            return toDate(match.groupValues[2].toInt(), match.groupValues[1].toInt())
        }
        namedMonthPattern.find(normalized)?.let { match ->
            val month = monthLookup[match.groupValues[1].lowercase()]
            val year = match.groupValues[2].toIntOrNull()
            if (month != null && year != null) return toDate(year, month)
        }
        return null
    }

    private fun extractCandidateTexts(draftData: DocumentDraftData): List<String> {
        val texts = mutableListOf<String>()
        when (draftData) {
            is InvoiceDraftData -> {
                draftData.notes?.let(texts::add)
                texts += draftData.lineItems.map { it.description }
            }

            is CreditNoteDraftData -> {
                draftData.reason?.let(texts::add)
                draftData.notes?.let(texts::add)
                texts += draftData.lineItems.map { it.description }
            }

            is ReceiptDraftData -> {
                draftData.notes?.let(texts::add)
                texts += draftData.lineItems.map { it.description }
            }
        }
        return texts
            .map { it.trim() }
            .filter { it.isNotBlank() }
    }

    private fun toDate(year: Int, month: Int): LocalDate? = runCatching {
        LocalDate(year, month, 1)
    }.getOrNull()
}
