package ai.thepredict.domain.model.old

import kotlin.jvm.JvmInline

@JvmInline
value class TaxNumber(private val value: String) {
    companion object {
        const val LENGTH = 12

        val formattedRegex = Regex("^[A-Z]{2}\\d{4}\\.\\d{3}\\.\\d{3}\$")

        private fun formatNumber(value: String): String {
            return buildString {
                append(value.substring(0, 2))
                append(value.substring(2, 6))
                append(".")
                append(value.substring(6, 9))
                append(".")
                append(value.substring(9, 12))
            }
        }

        fun canBeUsed(value: String): Boolean {
            return TaxNumber(value).raw.length == LENGTH
        }
    }

    val raw: String
        get() = value.replace(".", "").replace(" ", "")

    val country: String
        get() = formatted.substring(0, 2)

    val formatted: String
        get() = if (formattedRegex.matches(value)) value else formatNumber(value)
}