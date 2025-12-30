package tech.dokus.peppol.util

data class ParsedCompanyAddress(
    val street: String?,
    val city: String?,
    val postalCode: String?,
    val country: String?,
)

object CompanyAddressParser {
    fun parse(address: String?): ParsedCompanyAddress {
        if (address.isNullOrBlank()) return ParsedCompanyAddress(null, null, null, null)

        val normalized = address.replace("\n", ", ").replace(Regex("\\s+"), " ").trim()
        val parts = normalized.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        return when {
            parts.size >= 3 -> {
                val (postalCode, city) = parsePostalCodeAndCity(parts[1])
                ParsedCompanyAddress(parts[0], city, postalCode, parseCountryCode(parts.getOrNull(2)))
            }
            parts.size == 2 -> {
                val (postalCode, city) = parsePostalCodeAndCity(parts[1])
                ParsedCompanyAddress(parts[0], city, postalCode, null)
            }
            else -> {
                val (postalCode, city) = parsePostalCodeAndCity(parts[0])
                if (postalCode != null) {
                    ParsedCompanyAddress(null, city, postalCode, null)
                } else {
                    ParsedCompanyAddress(parts[0], null, null, null)
                }
            }
        }
    }

    private fun parsePostalCodeAndCity(text: String): Pair<String?, String?> {
        val pattern = Regex("^(\\d{4,5}(?:\\s?[A-Z]{2})?)\\s+(.+)$")
        val match = pattern.find(text.trim())
        return if (match != null) {
            Pair(match.groupValues[1], match.groupValues[2])
        } else {
            Pair(null, text.takeIf { it.isNotBlank() })
        }
    }

    private fun parseCountryCode(country: String?): String? {
        if (country.isNullOrBlank()) return null
        val normalized = country.trim().uppercase()
        return when {
            normalized.length == 2 -> normalized
            normalized in listOf("BELGIUM", "BELGIQUE", "BELGIE") -> "BE"
            normalized in listOf("NETHERLANDS", "NEDERLAND", "PAYS-BAS") -> "NL"
            normalized in listOf("FRANCE", "FRANKRIJK") -> "FR"
            normalized in listOf("GERMANY", "DEUTSCHLAND", "ALLEMAGNE", "DUITSLAND") -> "DE"
            normalized in listOf("LUXEMBOURG", "LUXEMBURG") -> "LU"
            else -> country.take(2).uppercase()
        }
    }
}

