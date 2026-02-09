package tech.dokus.domain.util

import kotlin.math.max
import kotlin.math.min

/**
 * Jaro-Winkler similarity implementation (0.0 - 1.0).
 * Pure Kotlin, no platform dependencies.
 */
object JaroWinkler {
    private const val PrefixLimit = 4
    private const val ScalingFactor = 0.1

    fun similarity(left: String?, right: String?): Double {
        if (left == null || right == null) return 0.0
        if (left == right) return 1.0
        if (left.isEmpty() || right.isEmpty()) return 0.0

        val s1 = left
        val s2 = right
        val s1Len = s1.length
        val s2Len = s2.length
        val matchDistance = max(s1Len, s2Len) / 2 - 1

        val s1Matches = BooleanArray(s1Len)
        val s2Matches = BooleanArray(s2Len)

        var matches = 0
        for (i in 0 until s1Len) {
            val start = max(0, i - matchDistance)
            val end = min(i + matchDistance + 1, s2Len)
            for (j in start until end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var transpositions = 0
        var k = 0
        for (i in 0 until s1Len) {
            if (!s1Matches[i]) continue
            while (k < s2Len && !s2Matches[k]) k++
            if (k < s2Len && s1[i] != s2[k]) {
                transpositions++
            }
            k++
        }

        val m = matches.toDouble()
        val jaro = (m / s1Len + m / s2Len + (m - transpositions / 2.0) / m) / 3.0

        var prefix = 0
        val maxPrefix = min(PrefixLimit, min(s1Len, s2Len))
        while (prefix < maxPrefix && s1[prefix] == s2[prefix]) {
            prefix++
        }

        return jaro + prefix * ScalingFactor * (1.0 - jaro)
    }
}
