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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import tech.dokus.domain.utils.json
import tech.dokus.foundation.backend.config.BusinessProfileEnrichmentConfig
import tech.dokus.foundation.backend.utils.runSuspendCatching
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

private const val ProbeUserAgent = "DokusBusinessProfileBot/1.0 (+https://dokus.io)"
private const val BrowserLikeUserAgent =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36"
private const val HtmlAcceptHeader = "text/html,application/xhtml+xml,*/*;q=0.8"
private const val ImageAcceptHeader = "image/*,*/*;q=0.8"
private const val MaxHtmlChars = 200_000
private const val MaxTextChars = 20_000
private const val MaxHeadHtmlSnippetChars = 12_000
private const val MaxLogoRelevantHtmlSnippetChars = 18_000
private val StripScriptStyleRegex = Regex("(?is)<(script|style|noscript)[^>]*>.*?</\\1>")
private val StripTagRegex = Regex("(?is)<[^>]+>")
private val CollapseWhitespaceRegex = Regex("\\s+")
private val HeadRegex = Regex("(?is)<head[^>]*>(.*?)</head>")
private val TitleRegex = Regex("(?is)<title[^>]*>(.*?)</title>")
private val MetaDescriptionRegex = Regex("(?is)<meta[^>]+name=[\"']description[\"'][^>]+content=[\"']([^\"']+)[\"']")
private val JsonLdRegex = Regex("(?is)<script[^>]+type=[\"']application/ld\\+json[\"'][^>]*>(.*?)</script>")
private val HrefRegex = Regex("(?is)<a[^>]+href=[\"']([^\"']+)[\"']")
private val IconHrefRegex = Regex("(?is)<link[^>]+rel=[\"'][^\"']*icon[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']")
private val ManifestHrefRegex = Regex("(?is)<link[^>]+rel=[\"'][^\"']*manifest[^\"']*[\"'][^>]+href=[\"']([^\"']+)[\"']")
private val OgImageRegex = Regex("(?is)<meta[^>]+property=[\"']og:image[\"'][^>]+content=[\"']([^\"']+)[\"']")
private val ImgSrcRegex = Regex("(?is)<img[^>]+src=[\"']([^\"']+)[\"']")
private val LogoRelevantTagRegex = Regex(
    "(?is)<(?:link|meta|img|script)[^>]*(?:logo|icon|favicon|manifest|og:image|application/ld\\+json)[^>]*>(?:.*?</script>)?"
)
private val EmailRegex = Regex("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")
private val PhoneRegex = Regex("(?<!\\w)(\\+?\\d[\\d\\s()./-]{6,}\\d)(?!\\w)")
private val TrackingQueryParams = setOf("srsltid", "gclid", "fbclid", "msclkid")
private val TrackingQueryParamPrefixes = listOf("utm_")

data class CrawledBusinessPage(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val headHtmlSnippet: String? = null,
    val logoRelevantHtmlSnippet: String? = null,
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

data class WebsiteSearchResult(
    val url: String,
    val title: String? = null,
    val snippet: String? = null,
    val searchRank: Int,
)

enum class ImageDownloadFailureKind {
    InvalidUrl,
    Timeout,
    HttpStatus,
    NonImageResponse,
    EmptyBody,
    Exception,
}

data class ImageDownloadResult(
    val image: DownloadedBusinessImage? = null,
    val normalizedUrl: String? = null,
    val statusCode: Int? = null,
    val contentType: String? = null,
    val failureKind: ImageDownloadFailureKind? = null,
    val usedBrowserUserAgent: Boolean = false,
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
    ): List<WebsiteSearchResult> {
        if (config.serperApiKey.isBlank()) return emptyList()
        val query = buildStrictSearchQuery(companyName, country) ?: return emptyList()
        val limit = maxResults.coerceIn(1, 3)
        return fetchSerperResults(query, limit).take(limit)
    }

    internal fun buildStrictSearchQuery(companyName: String, country: String?): String? {
        val normalizedName = sanitizeCompanyNameForSearch(companyName)
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
        return downloadImageDetailed(url, requestTimeoutMs = 2_000L).image
    }

    suspend fun downloadImageDetailed(
        url: String,
        requestTimeoutMs: Long,
        useBrowserUserAgent: Boolean = false,
    ): ImageDownloadResult {
        val normalized = normalizeUrl(url) ?: return ImageDownloadResult(
            normalizedUrl = null,
            failureKind = ImageDownloadFailureKind.InvalidUrl,
            usedBrowserUserAgent = useBrowserUserAgent,
        )
        val normalizedUrl = canonicalizeWebsiteUrl(normalized) ?: normalized
        val timeoutMs = requestTimeoutMs.coerceAtLeast(100L)
        val result = withTimeoutOrNull(timeoutMs) {
            runSuspendCatching {
                val response = httpClient.get(normalizedUrl) {
                    header(HttpHeaders.UserAgent, if (useBrowserUserAgent) BrowserLikeUserAgent else ProbeUserAgent)
                    header(HttpHeaders.Accept, ImageAcceptHeader)
                }
                if (!response.status.isSuccess()) {
                    return@runSuspendCatching ImageDownloadResult(
                        normalizedUrl = normalizedUrl,
                        statusCode = response.status.value,
                        failureKind = ImageDownloadFailureKind.HttpStatus,
                        usedBrowserUserAgent = useBrowserUserAgent,
                    )
                }
                val rawContentType = response.contentType()?.toString()
                val bytes: ByteArray = response.body()
                if (bytes.isEmpty()) {
                    return@runSuspendCatching ImageDownloadResult(
                        normalizedUrl = normalizedUrl,
                        contentType = rawContentType,
                        failureKind = ImageDownloadFailureKind.EmptyBody,
                        usedBrowserUserAgent = useBrowserUserAgent,
                    )
                }
                val effectiveContentType = resolveImageContentType(rawContentType, normalizedUrl, bytes)
                if (effectiveContentType == null) {
                    return@runSuspendCatching ImageDownloadResult(
                        normalizedUrl = normalizedUrl,
                        contentType = rawContentType,
                        failureKind = ImageDownloadFailureKind.NonImageResponse,
                        usedBrowserUserAgent = useBrowserUserAgent,
                    )
                }
                ImageDownloadResult(
                    image = DownloadedBusinessImage(bytes = bytes, contentType = effectiveContentType),
                    normalizedUrl = normalizedUrl,
                    statusCode = response.status.value,
                    contentType = effectiveContentType,
                    usedBrowserUserAgent = useBrowserUserAgent,
                )
            }.getOrElse {
                ImageDownloadResult(
                    normalizedUrl = normalizedUrl,
                    failureKind = ImageDownloadFailureKind.Exception,
                    usedBrowserUserAgent = useBrowserUserAgent,
                )
            }
        }
        return result ?: ImageDownloadResult(
            normalizedUrl = normalizedUrl,
            failureKind = ImageDownloadFailureKind.Timeout,
            usedBrowserUserAgent = useBrowserUserAgent,
        )
    }

    private suspend fun fetchPage(url: String): CrawledBusinessPage? {
        val uri = runCatching { URI(url) }.getOrNull() ?: return null
        val host = uri.host ?: return null

        return runSuspendCatching {
            val response = httpClient.get(url) {
                header(HttpHeaders.UserAgent, ProbeUserAgent)
                header(HttpHeaders.Accept, HtmlAcceptHeader)
            }
            if (!response.status.isSuccess()) return@runSuspendCatching null

            val contentType = response.contentType()?.toString()?.lowercase()
            if (contentType != null && "text/html" !in contentType) return@runSuspendCatching null

            val html = response.bodyAsText().take(MaxHtmlChars)
            val cleanedText = StripScriptStyleRegex.replace(html, " ")
                .let { StripTagRegex.replace(it, " ") }
                .let { CollapseWhitespaceRegex.replace(it, " ").trim() }
                .take(MaxTextChars)
            val headHtmlSnippet = HeadRegex.find(html)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?.take(MaxHeadHtmlSnippetChars)
                ?.ifBlank { null }
            val logoRelevantHtmlSnippet = extractLogoRelevantHtmlSnippet(html)

            val links = HrefRegex.findAll(html)
                .mapNotNull { normalizeUrl(it.groupValues[1], base = url) }
                .filter { candidate -> runCatching { URI(candidate).host?.equals(host, ignoreCase = true) == true }.getOrDefault(false) }
                .distinct()
                .take(40)
                .toList()

            val structuredData = JsonLdRegex.findAll(html)
                .map { it.groupValues[1].trim().take(4_000) }
                .filter { it.isNotBlank() }
                .take(10)
                .toList()
            val manifestUrls = ManifestHrefRegex.findAll(html)
                .mapNotNull { normalizeUrl(it.groupValues[1], base = url) }
                .distinct()
                .take(2)
                .toList()
            val manifestIcons = manifestUrls
                .flatMap { manifestUrl -> fetchManifestIcons(manifestUrl) }
                .distinct()
                .take(20)

            val logoCandidates = collectLogoCandidates(
                html = html,
                baseUrl = url,
                structuredData = structuredData,
                manifestIcons = manifestIcons
            )

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
                headHtmlSnippet = headHtmlSnippet,
                logoRelevantHtmlSnippet = logoRelevantHtmlSnippet,
                textContent = cleanedText,
                structuredDataSnippets = structuredData,
                emails = emails,
                phones = phones,
                links = links,
                logoCandidates = logoCandidates
            )
        }.getOrNull()
    }

    private fun extractLogoRelevantHtmlSnippet(html: String): String? {
        val snippet = LogoRelevantTagRegex.findAll(html)
            .map { it.value.trim() }
            .filter { it.isNotBlank() }
            .joinToString("\n")
            .take(MaxLogoRelevantHtmlSnippetChars)
            .trim()
        return snippet.ifBlank { null }
    }

    private suspend fun fetchSerperResults(query: String, maxResults: Int): List<WebsiteSearchResult> {
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
            organic.mapIndexedNotNull { index, element ->
                val obj = element as? JsonObject ?: return@mapIndexedNotNull null
                val rawUrl = obj["link"]?.jsonPrimitive?.content ?: return@mapIndexedNotNull null
                val normalizedUrl = canonicalizeWebsiteUrl(rawUrl) ?: return@mapIndexedNotNull null
                WebsiteSearchResult(
                    url = normalizedUrl,
                    title = obj["title"]?.jsonPrimitive?.contentOrNull,
                    snippet = obj["snippet"]?.jsonPrimitive?.contentOrNull,
                    searchRank = index + 1
                )
            }
        }.getOrDefault(emptyList())
    }

    internal fun sanitizeCompanyNameForSearch(companyName: String): String {
        val legalSuffixes = setOf(
            "nv", "bv", "bvba", "srl", "sprl", "sa", "sas", "ag", "gmbh", "llc", "ltd", "inc", "corp"
        )
        val tokens = companyName
            .trim()
            .split(Regex("\\s+"))
            .map { token -> token.trim().trim(',', '.', ';', ':', '-', '_') }
            .filter { it.isNotBlank() }
            .toMutableList()
        while (tokens.isNotEmpty()) {
            val last = tokens.last()
                .lowercase()
                .replace(Regex("[^a-z0-9]"), "")
            if (last.isBlank() || last !in legalSuffixes) break
            tokens.removeAt(tokens.lastIndex)
        }
        return tokens.joinToString(" ").ifBlank { companyName.trim() }
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
            val response = httpClient.get(robotsUrl) {
                header(HttpHeaders.UserAgent, ProbeUserAgent)
                header(HttpHeaders.Accept, "text/plain,*/*;q=0.8")
            }
            if (!response.status.isSuccess()) return@runSuspendCatching RobotsTxtPolicy.ALLOW_ALL
            RobotsTxtParser.parse(response.bodyAsText())
        }.getOrElse { RobotsTxtPolicy.ALLOW_ALL }
    }

    internal fun canonicalizeWebsiteUrl(value: String): String? {
        val normalized = normalizeUrl(value) ?: return null
        return runCatching {
            val uri = URI(normalized)
            val filteredQuery = uri.rawQuery
                ?.split("&")
                ?.mapNotNull { part ->
                    val name = part.substringBefore("=").lowercase()
                    if (name in TrackingQueryParams) return@mapNotNull null
                    if (TrackingQueryParamPrefixes.any { prefix -> name.startsWith(prefix) }) return@mapNotNull null
                    part
                }
                ?.joinToString("&")
                ?.ifBlank { null }
            buildString {
                append(uri.scheme.lowercase())
                append("://")
                append(uri.host)
                if (uri.port != -1) append(":${uri.port}")
                append(uri.rawPath?.ifBlank { "/" } ?: "/")
                if (filteredQuery != null) {
                    append("?")
                    append(filteredQuery)
                }
            }
        }.getOrNull()
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

    internal fun collectLogoCandidates(
        html: String,
        baseUrl: String,
        structuredData: List<String>,
        manifestIcons: List<String>
    ): List<String> {
        return buildList {
            addAll(IconHrefRegex.findAll(html).mapNotNull { normalizeUrl(it.groupValues[1], base = baseUrl) })
            addAll(
                OgImageRegex.findAll(html)
                    .mapNotNull { normalizeUrl(it.groupValues[1], base = baseUrl) }
                    .filter { ogImage ->
                        val lower = ogImage.lowercase()
                        lower.contains("logo") || lower.contains("icon")
                    }
            )
            addAll(extractJsonLdLogoCandidates(structuredData, baseUrl = baseUrl))
            addAll(manifestIcons)
            addAll(
                ImgSrcRegex.findAll(html)
                    .map { it.groupValues[1] }
                    .filter { src -> src.contains("logo", true) || src.contains("icon", true) }
                    .mapNotNull { normalizeUrl(it, base = baseUrl) }
            )
        }.distinct().take(20)
    }

    private suspend fun fetchManifestIcons(manifestUrl: String): List<String> {
        return runSuspendCatching {
            val response = httpClient.get(manifestUrl) {
                header(HttpHeaders.UserAgent, ProbeUserAgent)
                header(HttpHeaders.Accept, "application/manifest+json,application/json,*/*;q=0.8")
            }
            if (!response.status.isSuccess()) return@runSuspendCatching emptyList()
            val body = response.bodyAsText()
            val root = json.parseToJsonElement(body).jsonObject
            val icons = root["icons"]?.jsonArray.orEmpty()
            icons.mapNotNull { iconElement ->
                val iconObject = iconElement as? JsonObject ?: return@mapNotNull null
                val src = iconObject["src"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                normalizeUrl(src, base = manifestUrl)
            }
        }.getOrDefault(emptyList())
    }

    private fun extractJsonLdLogoCandidates(snippets: List<String>, baseUrl: String): List<String> {
        fun extractLogoFromObject(obj: JsonObject): List<String> {
            val logoElement = obj["logo"] ?: return emptyList()
            return when (logoElement) {
                is JsonPrimitive -> listOfNotNull(logoElement.contentOrNull)
                is JsonObject -> listOfNotNull(
                    logoElement["url"]?.jsonPrimitive?.contentOrNull,
                    logoElement["@id"]?.jsonPrimitive?.contentOrNull
                )
                is JsonArray -> logoElement.mapNotNull { item ->
                    when (item) {
                        is JsonPrimitive -> item.contentOrNull
                        is JsonObject -> item["url"]?.jsonPrimitive?.contentOrNull
                        else -> null
                    }
                }
                else -> emptyList()
            }
        }

        fun walk(element: kotlinx.serialization.json.JsonElement): List<String> {
            return when (element) {
                is JsonObject -> {
                    val typeRaw = element["@type"]?.jsonPrimitive?.contentOrNull?.lowercase()
                    val isOrganization = typeRaw?.contains("organization") == true
                    val direct = if (isOrganization) extractLogoFromObject(element) else emptyList()
                    val nested = element.values.flatMap(::walk)
                    direct + nested
                }
                is JsonArray -> element.flatMap(::walk)
                else -> emptyList()
            }
        }

        return snippets.asSequence()
            .flatMap { snippet ->
                runCatching {
                    walk(json.parseToJsonElement(snippet))
                }.getOrElse { emptyList() }.asSequence()
            }
            .mapNotNull { normalizeUrl(it, base = baseUrl) }
            .distinct()
            .toList()
    }

    private fun resolveImageContentType(contentType: String?, url: String, bytes: ByteArray): String? {
        val lowerContentType = contentType?.lowercase()
        if (lowerContentType?.startsWith("image/") == true) return contentType

        sniffImageContentType(bytes)?.let { return it }

        val lowerUrl = url.lowercase()
        return when {
            lowerUrl.endsWith(".png") -> "image/png"
            lowerUrl.endsWith(".svg") || lowerUrl.endsWith(".svgz") -> "image/svg+xml"
            lowerUrl.endsWith(".ico") -> "image/x-icon"
            lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg") -> "image/jpeg"
            lowerUrl.endsWith(".webp") -> "image/webp"
            else -> null
        }
    }

    private fun sniffImageContentType(bytes: ByteArray): String? {
        if (bytes.size >= 8 &&
            bytes[0] == 0x89.toByte() &&
            bytes[1] == 0x50.toByte() &&
            bytes[2] == 0x4E.toByte() &&
            bytes[3] == 0x47.toByte()
        ) {
            return "image/png"
        }
        if (bytes.size >= 3 &&
            bytes[0] == 0xFF.toByte() &&
            bytes[1] == 0xD8.toByte() &&
            bytes[2] == 0xFF.toByte()
        ) {
            return "image/jpeg"
        }
        if (bytes.size >= 4 &&
            bytes[0] == 0x00.toByte() &&
            bytes[1] == 0x00.toByte() &&
            bytes[2] == 0x01.toByte() &&
            bytes[3] == 0x00.toByte()
        ) {
            return "image/x-icon"
        }
        if (bytes.size >= 12) {
            val riff = bytes.copyOfRange(0, 4).decodeToString()
            val webp = bytes.copyOfRange(8, 12).decodeToString()
            if (riff == "RIFF" && webp == "WEBP") return "image/webp"
        }
        val head = bytes.copyOf(minOf(bytes.size, 200)).decodeToString().lowercase()
        if ("<svg" in head) return "image/svg+xml"
        return null
    }
}
