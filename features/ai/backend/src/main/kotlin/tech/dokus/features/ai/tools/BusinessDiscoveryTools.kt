package tech.dokus.features.ai.tools

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import io.ktor.http.userAgent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import java.net.URI

private const val DefaultUserAgent = "DokusBusinessProfileBot/1.0 (+https://dokus.io)"
private const val MaxTextChars = 16_000
private const val MaxHtmlChars = 200_000
private val StripScriptStyleRegex = Regex("(?is)<(script|style|noscript)[^>]*>.*?</\\1>")
private val StripTagRegex = Regex("(?is)<[^>]+>")
private val CollapseWhitespaceRegex = Regex("\\s+")
private val MetaDescriptionRegex = Regex("(?is)<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']")
private val TitleRegex = Regex("(?is)<title[^>]*>(.*?)</title>")
private val HrefRegex = Regex("(?is)<a[^>]+href=[\"']([^\"']+)[\"']")
private val IconHrefRegex = Regex("(?is)<link[^>]+rel=[\"'][^\"']*icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']")
private val OgImageRegex = Regex("(?is)<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']")
private val ImgSrcRegex = Regex("(?is)<img[^>]+src=[\"']([^\"']+)[\"']")
private val JsonLdRegex = Regex("(?is)<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>")
private val EmailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private val PhoneRegex = Regex("(?<!\\w)(\\+?\\d[\\d\\s()./-]{6,}\\d)(?!\\w)")

class BusinessDiscoveryTools(
    private val httpClient: HttpClient,
    private val config: BusinessProfileEnrichmentConfig,
    private val maxPages: Int,
    private val ignoreRobots: Boolean,
) : ToolSet {
    private val visitedPages = linkedSetOf<String>()
    private val pageCache = mutableMapOf<String, FetchWebPageResult>()
    private val robotsCache = mutableMapOf<String, RobotsRules>()

    @Tool("search_google_serper")
    @LLMDescription(
        """
        Search Google through Serper and return top organic results.
        Use this first to find the likely official company website.
        """
    )
    suspend fun searchGoogleSerper(
        @LLMDescription("Search query, usually company legal name + country + VAT.")
        query: String,
        @LLMDescription("Number of results to return (1..10).")
        num: Int = 5,
    ): SearchGoogleSerperResult {
        if (config.serperApiKey.isBlank()) {
            return SearchGoogleSerperResult(
                success = false,
                results = emptyList(),
                error = "SERPER_API_KEY is not configured"
            )
        }
        val requestBody = buildJsonObject {
            put("q", JsonPrimitive(query))
            put("num", JsonPrimitive(num.coerceIn(1, 10)))
        }
        return runCatching {
            val response = httpClient.post(config.serperBaseUrl) {
                contentType(ContentType.Application.Json)
                header("X-API-KEY", config.serperApiKey)
                setBody(requestBody.toString())
                userAgent(DefaultUserAgent)
            }
            if (!response.status.isSuccess()) {
                return@runCatching SearchGoogleSerperResult(
                    success = false,
                    results = emptyList(),
                    error = "Serper returned ${response.status.value}"
                )
            }
            val body = response.bodyAsText()
            val parsed = json.parseToJsonElement(body).jsonObject
            val organic = parsed["organic"] as? JsonArray ?: JsonArray(emptyList())
            val results = organic.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val link = obj["link"]?.jsonPrimitive?.content ?: return@mapNotNull null
                SearchGoogleSerperResult.SearchResult(
                    title = obj["title"]?.jsonPrimitive?.content,
                    url = link,
                    snippet = obj["snippet"]?.jsonPrimitive?.content
                )
            }
            SearchGoogleSerperResult(
                success = true,
                results = results,
                error = null
            )
        }.getOrElse { error ->
            SearchGoogleSerperResult(
                success = false,
                results = emptyList(),
                error = error.message ?: "Serper request failed"
            )
        }
    }

    @Tool("fetch_web_page")
    @LLMDescription(
        """
        Fetch and parse an HTML web page.
        Returns cleaned text, metadata, links, logo candidates, and structured-data snippets.
        """
    )
    suspend fun fetchWebPage(
        @LLMDescription("Absolute URL to fetch (http/https only).")
        url: String,
    ): FetchWebPageResult {
        val normalizedUrl = normalizeUrl(url)
            ?: return FetchWebPageResult(
                success = false,
                blockedByRobots = false,
                url = url,
                title = null,
                description = null,
                textContent = "",
                internalLinks = emptyList(),
                logoCandidates = emptyList(),
                structuredDataSnippets = emptyList(),
                emails = emptyList(),
                phones = emptyList(),
                error = "Invalid URL"
            )

        pageCache[normalizedUrl]?.let { return it }

        if (visitedPages.size >= maxPages && normalizedUrl !in visitedPages) {
            return FetchWebPageResult(
                success = false,
                blockedByRobots = false,
                url = normalizedUrl,
                title = null,
                description = null,
                textContent = "",
                internalLinks = emptyList(),
                logoCandidates = emptyList(),
                structuredDataSnippets = emptyList(),
                emails = emptyList(),
                phones = emptyList(),
                error = "Max crawl pages reached ($maxPages)"
            )
        }

        if (!ignoreRobots && !isAllowedByRobots(normalizedUrl)) {
            val blocked = FetchWebPageResult(
                success = false,
                blockedByRobots = true,
                url = normalizedUrl,
                title = null,
                description = null,
                textContent = "",
                internalLinks = emptyList(),
                logoCandidates = emptyList(),
                structuredDataSnippets = emptyList(),
                emails = emptyList(),
                phones = emptyList(),
                error = "Blocked by robots.txt"
            )
            pageCache[normalizedUrl] = blocked
            return blocked
        }

        val parsedUri = runCatching { URI(normalizedUrl) }.getOrNull()
        val host = parsedUri?.host.orEmpty()

        val result = runCatching {
            val response = httpClient.get(normalizedUrl) {
                userAgent(DefaultUserAgent)
            }
            if (!response.status.isSuccess()) {
                return@runCatching FetchWebPageResult(
                    success = false,
                    blockedByRobots = false,
                    url = normalizedUrl,
                    title = null,
                    description = null,
                    textContent = "",
                    internalLinks = emptyList(),
                    logoCandidates = emptyList(),
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    error = "HTTP ${response.status.value}"
                )
            }

            val contentType = response.contentType()?.toString()?.lowercase()
            if (contentType != null && "text/html" !in contentType) {
                return@runCatching FetchWebPageResult(
                    success = false,
                    blockedByRobots = false,
                    url = normalizedUrl,
                    title = null,
                    description = null,
                    textContent = "",
                    internalLinks = emptyList(),
                    logoCandidates = emptyList(),
                    structuredDataSnippets = emptyList(),
                    emails = emptyList(),
                    phones = emptyList(),
                    error = "Unsupported content type: $contentType"
                )
            }

            val html = response.bodyAsText().take(MaxHtmlChars)
            val cleanedText = StripScriptStyleRegex.replace(html, " ")
                .let { StripTagRegex.replace(it, " ") }
                .let { CollapseWhitespaceRegex.replace(it, " ").trim() }
                .take(MaxTextChars)

            val internalLinks = HrefRegex.findAll(html)
                .mapNotNull { match -> normalizeUrl(match.groupValues[1], base = normalizedUrl) }
                .filter { link ->
                    runCatching { URI(link).host?.equals(host, ignoreCase = true) ?: false }.getOrDefault(false)
                }
                .distinct()
                .take(25)
                .toList()

            val logoCandidates = buildList {
                addAll(IconHrefRegex.findAll(html).mapNotNull { normalizeUrl(it.groupValues[1], base = normalizedUrl) })
                addAll(OgImageRegex.findAll(html).mapNotNull { normalizeUrl(it.groupValues[1], base = normalizedUrl) })
                addAll(
                    ImgSrcRegex.findAll(html)
                        .map { it.groupValues[1] }
                        .filter { src -> src.contains("logo", ignoreCase = true) || src.contains("icon", ignoreCase = true) }
                        .mapNotNull { normalizeUrl(it, base = normalizedUrl) }
                )
            }.distinct().take(20)

            val structuredDataSnippets = JsonLdRegex.findAll(html)
                .map { it.groupValues[1].trim().take(4_000) }
                .filter { it.isNotBlank() }
                .take(10)
                .toList()

            val emails = EmailRegex.findAll(cleanedText)
                .map { it.value.lowercase() }
                .distinct()
                .take(20)
                .toList()

            val phones = PhoneRegex.findAll(cleanedText)
                .map { it.value.trim() }
                .distinct()
                .take(20)
                .toList()

            FetchWebPageResult(
                success = true,
                blockedByRobots = false,
                url = normalizedUrl,
                title = TitleRegex.find(html)?.groupValues?.getOrNull(1)?.trim(),
                description = MetaDescriptionRegex.find(html)?.groupValues?.getOrNull(1)?.trim(),
                textContent = cleanedText,
                internalLinks = internalLinks,
                logoCandidates = logoCandidates,
                structuredDataSnippets = structuredDataSnippets,
                emails = emails,
                phones = phones,
                error = null
            )
        }.getOrElse { error ->
            FetchWebPageResult(
                success = false,
                blockedByRobots = false,
                url = normalizedUrl,
                title = null,
                description = null,
                textContent = "",
                internalLinks = emptyList(),
                logoCandidates = emptyList(),
                structuredDataSnippets = emptyList(),
                emails = emptyList(),
                phones = emptyList(),
                error = error.message ?: "Failed to fetch page"
            )
        }

        if (result.success) {
            visitedPages += normalizedUrl
        }
        pageCache[normalizedUrl] = result
        return result
    }

    @Tool("inspect_image_url")
    @LLMDescription(
        """
        Check whether an image URL looks valid and usable as a logo.
        """
    )
    suspend fun inspectImageUrl(
        @LLMDescription("Absolute image URL to inspect.")
        url: String,
    ): InspectImageUrlResult {
        val normalizedUrl = normalizeUrl(url)
            ?: return InspectImageUrlResult(
                success = false,
                url = url,
                contentType = null,
                contentLength = null,
                likelyLogo = false,
                error = "Invalid URL"
            )

        return runCatching {
            val response = httpClient.get(normalizedUrl) {
                userAgent(DefaultUserAgent)
            }
            if (response.status == HttpStatusCode.NotFound) {
                return@runCatching InspectImageUrlResult(
                    success = false,
                    url = normalizedUrl,
                    contentType = null,
                    contentLength = null,
                    likelyLogo = false,
                    error = "Image not found"
                )
            }
            val type = response.contentType()?.toString()
            val isImage = type?.startsWith("image/") == true
            val body = if (isImage) response.bodyAsText() else ""
            val contentLength = response.headers["Content-Length"]?.toLongOrNull()
                ?: body.length.toLong().takeIf { it > 0L }

            InspectImageUrlResult(
                success = response.status.isSuccess() && isImage,
                url = normalizedUrl,
                contentType = type,
                contentLength = contentLength,
                likelyLogo = isImage && isLikelyLogoUrl(normalizedUrl),
                error = if (response.status.isSuccess() && isImage) null else "Unsupported content type: $type"
            )
        }.getOrElse { error ->
            InspectImageUrlResult(
                success = false,
                url = normalizedUrl,
                contentType = null,
                contentLength = null,
                likelyLogo = false,
                error = error.message ?: "Image inspection failed"
            )
        }
    }

    private suspend fun isAllowedByRobots(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return true
        val host = uri.host ?: return true
        val robotsUrl = buildString {
            append(uri.scheme ?: "https")
            append("://")
            append(host)
            append("/robots.txt")
        }
        val cached = robotsCache[robotsUrl]
        val rules = if (cached != null) {
            cached
        } else {
            val loaded = loadRobots(robotsUrl)
            robotsCache[robotsUrl] = loaded
            loaded
        }
        val path = uri.rawPath.ifBlank { "/" }
        return rules.isAllowed(path)
    }

    private suspend fun loadRobots(robotsUrl: String): RobotsRules {
        return runCatching {
            val response = httpClient.get(robotsUrl) {
                userAgent(DefaultUserAgent)
            }
            if (!response.status.isSuccess()) return@runCatching RobotsRules.ALLOW_ALL
            parseRobots(response.bodyAsText())
        }.getOrElse { RobotsRules.ALLOW_ALL }
    }

    private fun parseRobots(content: String): RobotsRules {
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
                "user-agent" -> collect = value == "*" || value.equals("dokusbusinessprofilebot", ignoreCase = true)
                "allow" -> if (collect && value.isNotBlank()) allow += normalizeRobotsPath(value)
                "disallow" -> if (collect && value.isNotBlank()) disallow += normalizeRobotsPath(value)
            }
        }

        return RobotsRules(allow = allow, disallow = disallow)
    }

    private fun normalizeRobotsPath(path: String): String {
        val withoutWildcards = path.substringBefore('*').trim()
        return if (withoutWildcards.startsWith("/")) withoutWildcards else "/$withoutWildcards"
    }

    private fun normalizeUrl(value: String, base: String? = null): String? {
        return runCatching {
            val raw = value.trim()
            if (raw.isBlank()) return@runCatching null

            val absolute = if (base != null) {
                URI(base).resolve(raw)
            } else {
                URI(raw)
            }

            val scheme = absolute.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return@runCatching null

            val host = absolute.host ?: return@runCatching null
            val sanitizedPath = absolute.path?.ifBlank { "/" } ?: "/"
            buildString {
                append(scheme)
                append("://")
                append(host)
                if (absolute.port != -1) {
                    append(":")
                    append(absolute.port)
                }
                append(sanitizedPath)
                absolute.query?.let {
                    append("?")
                    append(it)
                }
            }
        }.getOrNull()
    }

    private fun isLikelyLogoUrl(url: String): Boolean {
        val lower = url.lowercase()
        return listOf("logo", "brand", "icon", "favicon").any { it in lower }
    }

    private data class RobotsRules(
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
            val ALLOW_ALL = RobotsRules(emptyList(), emptyList())
        }
    }
}

@Serializable
data class SearchGoogleSerperResult(
    val success: Boolean,
    val results: List<SearchResult>,
    val error: String? = null,
) {
    @Serializable
    data class SearchResult(
        val title: String? = null,
        val url: String,
        val snippet: String? = null,
    )
}

@Serializable
data class FetchWebPageResult(
    val success: Boolean,
    val blockedByRobots: Boolean,
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val textContent: String,
    val internalLinks: List<String>,
    val logoCandidates: List<String>,
    val structuredDataSnippets: List<String>,
    val emails: List<String>,
    val phones: List<String>,
    val error: String? = null,
)

@Serializable
data class InspectImageUrlResult(
    val success: Boolean,
    val url: String,
    val contentType: String? = null,
    val contentLength: Long? = null,
    val likelyLogo: Boolean,
    val error: String? = null,
)
