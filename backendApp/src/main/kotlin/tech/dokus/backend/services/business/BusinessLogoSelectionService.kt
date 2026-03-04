package tech.dokus.backend.services.business

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import tech.dokus.foundation.backend.utils.loggerFor
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

private const val LogoDiscoveryBudgetMs = 3_500L
private const val LogoPerAttemptTimeoutMs = 1_200L
private const val LogoRetryMinRemainingBudgetMs = 900L
private val RetryableBotBlockStatusCodes = setOf(403, 406, 429)

private enum class LogoFormat {
    Png,
    Svg,
    Ico,
}

private data class LogoCandidate(
    val sourceUrl: String,
    val format: LogoFormat,
    val area: Int,
    val image: DownloadedBusinessImage,
)

enum class LogoSelectionTerminalReason {
    NO_CANDIDATES,
    ALL_DOWNLOAD_FAILED,
    BLOCKED_OR_FORBIDDEN,
    NON_IMAGE_RESPONSE,
    NORMALIZATION_FAILED,
    BUDGET_EXHAUSTED,
}

data class LogoSelectionTrace(
    val initialCandidates: Int,
    val fallbackCandidatesAppended: Int,
    val totalCandidates: Int,
    val attempts: Int,
    val pngSuccesses: Int,
    val svgSuccesses: Int,
    val icoSuccesses: Int,
    val selectedSourceUrl: String? = null,
    val selectedFormat: String? = null,
    val terminalReason: LogoSelectionTerminalReason? = null,
    val elapsedMs: Long,
)

data class LogoSelectionResult(
    val image: DownloadedBusinessImage? = null,
    val trace: LogoSelectionTrace,
)

class BusinessLogoSelectionService(
    private val websiteProbe: BusinessWebsiteProbe,
) {
    private val logger = loggerFor()

    suspend fun selectPreferredLogo(
        websiteUrl: String?,
        logoCandidates: List<String>
    ): LogoSelectionResult {
        val initialCandidates = logoCandidates
            .asSequence()
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .take(40)
            .toList()
        val fallbackCandidates = buildHostFallbackCandidates(websiteUrl)
        val mergedCandidates = (initialCandidates + fallbackCandidates)
            .distinct()
            .take(60)
        val appendedFallbackCount = (mergedCandidates.size - initialCandidates.size).coerceAtLeast(0)

        if (mergedCandidates.isEmpty()) {
            return LogoSelectionResult(
                image = null,
                trace = LogoSelectionTrace(
                    initialCandidates = 0,
                    fallbackCandidatesAppended = 0,
                    totalCandidates = 0,
                    attempts = 0,
                    pngSuccesses = 0,
                    svgSuccesses = 0,
                    icoSuccesses = 0,
                    selectedSourceUrl = null,
                    selectedFormat = null,
                    terminalReason = LogoSelectionTerminalReason.NO_CANDIDATES,
                    elapsedMs = 0
                )
            )
        }

        var attempts = 0
        var pngSuccesses = 0
        var svgSuccesses = 0
        var icoSuccesses = 0
        var blockedFailures = 0
        var nonImageFailures = 0
        var normalizationFailures = 0
        var budgetExhausted = false

        val startedAtNanos = System.nanoTime()
        fun elapsedMs(): Long = (System.nanoTime() - startedAtNanos) / 1_000_000
        fun remainingBudgetMs(): Long = (LogoDiscoveryBudgetMs - elapsedMs()).coerceAtLeast(0)

        val evaluated = mutableListOf<LogoCandidate>()
        suspend fun evaluateAttempt(url: String, result: ImageDownloadResult) {
            if (result.image == null) {
                when (result.failureKind) {
                    ImageDownloadFailureKind.HttpStatus -> {
                        if (result.statusCode in RetryableBotBlockStatusCodes) {
                            blockedFailures++
                        }
                    }
                    ImageDownloadFailureKind.NonImageResponse -> nonImageFailures++
                    else -> Unit
                }
                return
            }

            val downloaded = result.image
            val sourceUrl = result.normalizedUrl ?: url
            val format = detectLogoFormat(sourceUrl, downloaded.contentType)
            if (format == null) {
                normalizationFailures++
                return
            }
            val pngBytes = normalizeLogoToPng(downloaded.bytes, format)
            if (pngBytes == null) {
                normalizationFailures++
                return
            }
            val area = when (format) {
                LogoFormat.Png -> {
                    pngSuccesses++
                    imageArea(pngBytes)
                }
                LogoFormat.Svg -> {
                    svgSuccesses++
                    extractSvgArea(downloaded.bytes)
                }
                LogoFormat.Ico -> {
                    icoSuccesses++
                    imageArea(pngBytes)
                }
            }
            evaluated += LogoCandidate(
                sourceUrl = sourceUrl,
                format = format,
                area = area,
                image = DownloadedBusinessImage(
                    bytes = pngBytes,
                    contentType = "image/png"
                )
            )
        }

        for (url in mergedCandidates) {
            val remainingBeforeAttempt = remainingBudgetMs()
            if (remainingBeforeAttempt <= 0) {
                budgetExhausted = true
                break
            }

            attempts++
            val firstAttempt = websiteProbe.downloadImageDetailed(
                url = url,
                requestTimeoutMs = minOf(LogoPerAttemptTimeoutMs, remainingBeforeAttempt),
                useBrowserUserAgent = false
            )
            evaluateAttempt(url, firstAttempt)

            val shouldRetryWithBrowserUa = firstAttempt.image == null &&
                firstAttempt.failureKind == ImageDownloadFailureKind.HttpStatus &&
                firstAttempt.statusCode in RetryableBotBlockStatusCodes
            if (shouldRetryWithBrowserUa) {
                val remainingBeforeRetry = remainingBudgetMs()
                if (remainingBeforeRetry >= LogoRetryMinRemainingBudgetMs) {
                    attempts++
                    val retryAttempt = websiteProbe.downloadImageDetailed(
                        url = url,
                        requestTimeoutMs = minOf(LogoPerAttemptTimeoutMs, remainingBeforeRetry),
                        useBrowserUserAgent = true
                    )
                    evaluateAttempt(url, retryAttempt)
                }
            }
        }

        val selected = evaluated
            .filter { it.format == LogoFormat.Png }
            .maxByOrNull { it.area }
            ?: evaluated.filter { it.format == LogoFormat.Svg }
                .maxByOrNull { it.area }
            ?: evaluated.filter { it.format == LogoFormat.Ico }
                .maxByOrNull { it.area }
        val terminalReason = when {
            selected != null -> null
            budgetExhausted -> LogoSelectionTerminalReason.BUDGET_EXHAUSTED
            blockedFailures > 0 -> LogoSelectionTerminalReason.BLOCKED_OR_FORBIDDEN
            nonImageFailures > 0 -> LogoSelectionTerminalReason.NON_IMAGE_RESPONSE
            normalizationFailures > 0 -> LogoSelectionTerminalReason.NORMALIZATION_FAILED
            else -> LogoSelectionTerminalReason.ALL_DOWNLOAD_FAILED
        }
        val trace = LogoSelectionTrace(
            initialCandidates = initialCandidates.size,
            fallbackCandidatesAppended = appendedFallbackCount,
            totalCandidates = mergedCandidates.size,
            attempts = attempts,
            pngSuccesses = pngSuccesses,
            svgSuccesses = svgSuccesses,
            icoSuccesses = icoSuccesses,
            selectedSourceUrl = selected?.sourceUrl,
            selectedFormat = selected?.format?.name,
            terminalReason = terminalReason,
            elapsedMs = elapsedMs()
        )
        logger.debug(
            "Logo selection trace websiteUrl={}, initialCandidates={}, fallbackAppended={}, attempts={}, pngSuccesses={}, svgSuccesses={}, icoSuccesses={}, selectedSource={}, selectedFormat={}, terminalReason={}, elapsedMs={}",
            websiteUrl,
            trace.initialCandidates,
            trace.fallbackCandidatesAppended,
            trace.attempts,
            trace.pngSuccesses,
            trace.svgSuccesses,
            trace.icoSuccesses,
            trace.selectedSourceUrl,
            trace.selectedFormat,
            trace.terminalReason,
            trace.elapsedMs
        )
        return LogoSelectionResult(
            image = selected?.image,
            trace = trace
        )
    }

    private fun buildHostFallbackCandidates(websiteUrl: String?): List<String> {
        val host = runCatching { URI(websiteUrl).host }
            .getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        val hosts = linkedSetOf(host, "www.$host")
        return listOf(
            "/favicon.ico",
            "/apple-touch-icon.png",
            "/favicon-32x32.png",
            "/favicon-96x96.png",
            "/android-chrome-192x192.png"
        ).flatMap { path ->
            hosts.map { fallbackHost -> "https://$fallbackHost$path" }
        }.distinct()
    }

    private fun detectLogoFormat(url: String, contentType: String): LogoFormat? {
        val lowerContentType = contentType.lowercase()
        val lowerUrl = url.lowercase()
        return when {
            lowerContentType.contains("png") || lowerUrl.contains(".png") -> LogoFormat.Png
            lowerContentType.contains("svg") || lowerUrl.contains(".svg") -> LogoFormat.Svg
            lowerContentType.contains("icon") ||
                lowerContentType.contains("ico") ||
                lowerUrl.contains(".ico") ||
                lowerUrl.contains("favicon") -> LogoFormat.Ico
            else -> null
        }
    }

    private fun normalizeLogoToPng(bytes: ByteArray, format: LogoFormat): ByteArray? {
        return when (format) {
            LogoFormat.Png -> bytes
            LogoFormat.Svg -> convertSvgToPng(bytes)
            LogoFormat.Ico -> convertRasterToPng(bytes)
        }
    }

    private fun convertRasterToPng(bytes: ByteArray): ByteArray? {
        return runCatching {
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return null
            ByteArrayOutputStream().use { output ->
                if (!ImageIO.write(image, "png", output)) return null
                output.toByteArray()
            }
        }.getOrNull()
    }

    private fun convertSvgToPng(bytes: ByteArray): ByteArray? {
        return runCatching {
            val transcoder = PNGTranscoder()
            val dimensions = extractSvgDimensions(bytes)
            if (dimensions != null) {
                transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, dimensions.first)
                transcoder.addTranscodingHint(PNGTranscoder.KEY_HEIGHT, dimensions.second)
            }
            ByteArrayOutputStream().use { output ->
                transcoder.transcode(
                    TranscoderInput(ByteArrayInputStream(bytes)),
                    TranscoderOutput(output)
                )
                output.toByteArray()
            }
        }.getOrNull()
    }

    private fun extractSvgDimensions(bytes: ByteArray): Pair<Float, Float>? {
        val content = runCatching { bytes.toString(StandardCharsets.UTF_8) }.getOrNull() ?: return null
        val width = Regex("""\bwidth\s*=\s*[\"']?([0-9]+(?:\\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.toFloatOrNull()
        val height = Regex("""\bheight\s*=\s*[\"']?([0-9]+(?:\\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
            .find(content)
            ?.groupValues
            ?.getOrNull(1)
            ?.toFloatOrNull()

        if (width != null && height != null && width > 0f && height > 0f) {
            return width to height
        }

        val viewBox = Regex(
            """\bviewBox\s*=\s*[\"']?([0-9.-]+)\s+([0-9.-]+)\s+([0-9.-]+)\s+([0-9.-]+)""",
            RegexOption.IGNORE_CASE
        ).find(content)
        if (viewBox != null) {
            val vbWidth = viewBox.groupValues[3].toFloatOrNull()
            val vbHeight = viewBox.groupValues[4].toFloatOrNull()
            if (vbWidth != null && vbHeight != null && vbWidth > 0f && vbHeight > 0f) {
                return vbWidth to vbHeight
            }
        }
        return null
    }

    private fun extractSvgArea(bytes: ByteArray): Int {
        val dimensions = extractSvgDimensions(bytes) ?: return 0
        return (dimensions.first * dimensions.second).toInt()
    }

    private fun imageArea(bytes: ByteArray): Int {
        return runCatching {
            val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return 0
            image.width * image.height
        }.getOrDefault(0)
    }
}
