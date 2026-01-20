package tech.dokus.features.ai.utils

internal fun normalizeJson(response: String): String {
    // Remove markdown code blocks if present
    val cleaned = response
        .replace("```json", "")
        .replace("```", "")
        .trim()

    // Find JSON object in the response
    val startIndex = cleaned.indexOf('{')
    val endIndex = cleaned.lastIndexOf('}')

    return if (startIndex in 0..<endIndex) {
        cleaned.substring(startIndex, endIndex + 1)
    } else {
        cleaned
    }
}
