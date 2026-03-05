package tech.dokus.backend.services.business

internal data class RobotsTxtPolicy(
    val allow: List<String>,
    val disallow: List<String>,
) {
    fun isAllowed(path: String): Boolean {
        val allowMatch = allow.maxByOrNull { matchLength(path, it) }.orEmpty()
        val disallowMatch = disallow.maxByOrNull { matchLength(path, it) }.orEmpty()
        val allowLen = matchLength(path, allowMatch)
        val disallowLen = matchLength(path, disallowMatch)
        if (allowLen == 0 && disallowLen == 0) return true
        return allowLen >= disallowLen
    }

    private fun matchLength(path: String, rule: String): Int {
        if (rule.isBlank()) return 0
        return if (path.startsWith(rule)) rule.length else 0
    }

    companion object {
        val ALLOW_ALL = RobotsTxtPolicy(emptyList(), emptyList())
    }
}

internal object RobotsTxtParser {
    fun parse(
        content: String,
        userAgent: String = "dokusbusinessprofilebot",
    ): RobotsTxtPolicy {
        val allow = mutableListOf<String>()
        val disallow = mutableListOf<String>()
        var collect = false

        content.lineSequence().forEach { raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isBlank()) return@forEach
            val parts = line.split(":", limit = 2)
            if (parts.size != 2) return@forEach
            val key = parts[0].trim().lowercase()
            val value = parts[1].trim()
            when (key) {
                "user-agent" -> collect = value == "*" || value.equals(userAgent, ignoreCase = true)
                "allow" -> if (collect && value.isNotBlank()) allow += normalizePath(value)
                "disallow" -> if (collect && value.isNotBlank()) disallow += normalizePath(value)
            }
        }

        return RobotsTxtPolicy(allow = allow, disallow = disallow)
    }

    private fun normalizePath(path: String): String {
        val withoutWildcards = path.substringBefore('*').trim()
        return if (withoutWildcards.startsWith("/")) withoutWildcards else "/$withoutWildcards"
    }
}
