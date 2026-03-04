package tech.dokus.backend.services.business

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private const val ProbeUserAgent = "DokusBusinessProfileBot/1.0 (+https://dokus.io)"
private const val MaxHtmlChars = 200_000
private const val MaxTextChars = 20_000
private val StripScriptStyleRegex = Regex("(?is)<(script|style|noscript)[^>]*>.*?</\\1>")
private val StripTagRegex = Regex("(?is)<[^>]+>")
private val CollapseWhitespaceRegex = Regex("\\s+")
private val TitleRegex = Regex("(?is)<title[^>]*>(.*?)</title>")
private val MetaDescriptionRegex = Regex("(?is)<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']")
private val JsonLdRegex = Regex("(?is)<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>")
private val HrefRegex = Regex("(?is)<a[^>]+href=[\"']([^\"']+)[\"']")
private val IconHrefRegex = Regex("(?is)<link[^>]+rel=[\"'][^\"']*icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']")
private val OgImageRegex = Regex("(?is)<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']")
private val ImgSrcRegex = Regex("(?is)<img[^>]+src=[\"']([^\"']+)[\"']")
private val EmailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private val PhoneRegex = Regex("(?<!\\w)(\\+?\\d[\\d\\s()./-]{6,}\\d)(?!\\w)")

data class CrawledBusinessPage(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val textContent: String,
    val structuredDataSnippets: List<String>,
    val emails: List<String>,
    val phones: List<String>,
    val links: List<String>,
    val logoCandidates: List<String>,
)

data class BusinessWebsiteCrawlResult(
    val pages: List<CrawledBusinessPage>,
    val blockedByRobots: Boolean,
)

data class DownloadedBusinessImage(
    val bytes: ByteArray,
    val contentType: String,
)

class BusinessWebsiteProbe(
    private val httpClient: HttpClient,
    private val config: BusinessProfileEnrichmentConfig,
) {
    private val robotsCache = ConcurrentHashMap<String, RobotsTxtPolicy>()

    suspend fun searchWebsiteCandidates(
        companyName: String,
        country: String? = null,
        maxResults: Int = 3
    ): List<String> {
        if (config.serperApiKey.isBlank()) return emptyList()
        val query = buildStrictSearchQuery(companyName, country) ?: return emptyList()
        val limit = maxResults.coerceIn(1, 3)
        return fetchSerperUrls(query, limit).take(limit)
    }

    internal fun buildStrictSearchQuery(companyName: String, country: String?): String? {
        val normalizedName = companyName.trim()
        if (normalizedName.isBlank()) return null
        val countryPart = country?.trim().orEmpty()
        return if (countryPart.isBlank()) normalizedName else "$normalizedName $countryPart"
    }

    suspend fun crawl(startUrl: String, maxPages: Int = config.maxPages): BusinessWebsiteCrawlResult {
        val normalizedStart = normalizeUrl(startUrl)
            ?: return BusinessWebsiteCrawlResult(pages = emptyList(), blockedByRobots = false)

        val queue = ArrayDeque<String>()
        val seen = linkedSetOf<String>()
        val pages = mutableListOf<CrawledBusinessPage>()
        var blockedByRobots = false

        queue.add(normalizedStart)
        while (queue.isNotEmpty() && pages.size < maxPages.coerceAtLeast(1)) {
            val next = queue.removeFirst()
            if (!seen.add(next)) continue

            if (!config.ignoreRobots && !isAllowedByRobots(next)) {
                blockedByRobots = true
                continue
            }

            val page = fetchPage(next) ?: continue
            pages += page
            if (pages.size >= maxPages) break

            prioritizeLinks(page.links).forEach { candidate ->
                if (!seen.contains(candidate) && !queue.contains(candidate) && queue.size < maxPages * 5) {
                    queue.addLast(candidate)
                }
            }
        }

        return BusinessWebsiteCrawlResult(
            pages = pages,
            blockedByRobots = blockedByRobots
        )
    }

    suspend fun downloadImage(url: String): DownloadedBusinessImage? {
        val normalized = normalizeUrl(url) ?: return null
        return runSuspendCatching {
            val response = httpClient.get(normalized) {
                header(HttpHeaders.UserAgent, ProbeUserAgent)
            }
            if (!response.status.isSuccess()) return null
            val contentType = response.contentType()?.toString()
            if (contentType == null || !contentType.startsWith("image/")) return null
            val bytes: ByteArray = response.body()
            if (bytes.isEmpty()) return null
            DownloadedBusinessImage(bytes = bytes, contentType = contentType)
        }.getOrNull()
    }

    private suspend fun fetchPage(url: String): CrawledBusinessPage? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host ?: return null

        return runSuspendCatching {
            val response = httpClient.get(url) {
                header(HttpHeaders.UserAgent, ProbeUserAgent)
            }
            if (!response.status.isSuccess()) return@runSuspendCatching null

            val contentType = response.contentType()?.toString()?.lowercase()
            if (contentType != null && "text/html" !in contentType) return@runSuspendCatching null

            val html = response.bodyAsText().take(MaxHtmlChars)
            val cleanedText = StripScriptStyleRegex.replace(html, " ")
                .let { StripTagRegex.replace(it, " ") }
                .let { CollapseWhitespaceRegex.replace(it, " ").trim() }
                .take(MaxTextChars)

            val links = HrefRegex.findAll(html)
                .mapNotNull { normalizeUrl(it.groupValues[1], base = url) }
                .filter { candidate -> runCatching { URI(candidate).host?.equals(host, ignoreCase = true) == true }.getOrDefault(false) }
                .distinct()
                .take(40)
                .toList()

            val logoCandidates = buildList {
                addAll(IconHrefRegex.findAll(html).mapNotNull { normalizeUrl(it.groupValues[1], base = url) })
                addAll(OgImageRegex.findAll(html).mapNotNull { normalizeUrl(it.groupValues[1], base = url) })
                addAll(
                    ImgSrcRegex.findAll(html)
                        .map { it.groupValues[1] }
                        .filter { src -> src.contains("logo", true) || src.contains("icon", true) }
                        .mapNotNull { normalizeUrl(it, base = url) }
                )
            }.distinct().take(20)

            val structuredData = JsonLdRegex.findAll(html)
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

            CrawledBusinessPage(
                url = url,
                title = TitleRegex.find(html)?.groupValues?.getOrNull(1)?.trim(),
                description = MetaDescriptionRegex.find(html)?.groupValues?.getOrNull(1)?.trim(),
                textContent = cleanedText,
                structuredDataSnippets = structuredData,
                emails = emails,
                phones = phones,
                links = links,
                logoCandidates = logoCandidates
            )
        }.getOrNull()
    }

    private suspend fun fetchSerperUrls(query: String, maxResults: Int): List<String> {
        return runSuspendCatching {
            val requestBody = buildJsonObject {
                put("q", JsonPrimitive(query))
                put("num", JsonPrimitive(maxResults.coerceIn(1, 10)))
            }
            val response = httpClient.post(config.serperBaseUrl) {
                contentType(ContentType.Application.Json)
                header("X-API-KEY", config.serperApiKey)
                header(HttpHeaders.UserAgent, ProbeUserAgent)
                setBody(requestBody.toString())
            }
            if (!response.status.isSuccess()) return@runSuspendCatching emptyList()
            val parsed = json.parseToJsonElement(response.bodyAsText()).jsonObject
            val organic = parsed["organic"] as? JsonArray ?: JsonArray(emptyList())
            organic.mapNotNull { element ->
                val obj = element as? JsonObject ?: return@mapNotNull null
                val rawUrl = obj["link"]?.jsonPrimitive?.content ?: return@mapNotNull null
                normalizeUrl(rawUrl)
            }
        }.getOrDefault(emptyList())
    }

    private fun prioritizeLinks(links: List<String>): List<String> {
        val highPriority = listOf("about", "contact", "impressum", "legal", "company", "team")
        return links.sortedBy { link ->
            val lower = link.lowercase()
            val priorityIndex = highPriority.indexOfFirst { keyword -> keyword in lower }
            if (priorityIndex == -1) highPriority.size else priorityIndex
        }
    }

    private suspend fun isAllowedByRobots(url: String): Boolean {
        val uri = runCatching { URI(url) }.getOrNull() ?: return true
        val host = uri.host ?: return true
        val robotsUrl = "${uri.scheme ?: "https"}://$host/robots.txt"
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

    private suspend fun loadRobots(robotsUrl: String): RobotsTxtPolicy {
        return runSuspendCatching {
            val response = httpClient.get(robotsUrl) { header(HttpHeaders.UserAgent, ProbeUserAgent) }
            if (!response.status.isSuccess()) return@runSuspendCatching RobotsTxtPolicy.ALLOW_ALL
            RobotsTxtParser.parse(response.bodyAsText())
        }.getOrElse { RobotsTxtPolicy.ALLOW_ALL }
    }

    private fun normalizeUrl(value: String, base: String? = null): String? {
        return runCatching {
            val raw = value.trim()
            if (raw.isBlank()) return@runCatching null
            val resolved = if (base != null) URI(base).resolve(raw) else URI(raw)
            val scheme = resolved.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return@runCatching null
            val host = resolved.host ?: return@runCatching null
            buildString {
                append(scheme)
                append("://")
                append(host)
                if (resolved.port != -1) append(":${resolved.port}")
                append(resolved.path?.ifBlank { "/" } ?: "/")
                resolved.query?.let { append("?$it") }
            }
        }.getOrNull()
    }
}
