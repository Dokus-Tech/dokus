package tech.dokus.backend.services.business

import org.apache.batik.transcoder.TranscoderInput
import org.apache.batik.transcoder.TranscoderOutput
import org.apache.batik.transcoder.image.PNGTranscoder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URI
import java.nio.charset.StandardCharsets
import javax.imageio.ImageIO

private enum class LogoFormat {
    Png,
    Svg,
    Ico,
}

private data class LogoCandidate(
    val format: LogoFormat,
    val area: Int,
    val image: DownloadedBusinessImage,
)

class BusinessLogoSelectionService(
    private val websiteProbe: BusinessWebsiteProbe,
) {
    suspend fun selectPreferredLogo(
        websiteUrl: String?,
        logoCandidates: List<String>
    ): DownloadedBusinessImage? {
        val normalizedCandidates = logoCandidates
            .asSequence()
            .map { it.trim() }
            .filter { it.startsWith("http://") || it.startsWith("https://") }
            .distinct()
            .take(40)
            .toMutableList()
        if (normalizedCandidates.isEmpty()) {
            normalizedCandidates += buildHostFallbackCandidates(websiteUrl)
        }

        val evaluated = mutableListOf<LogoCandidate>()
        for (url in normalizedCandidates) {
            val downloaded = websiteProbe.downloadImage(url) ?: continue
            val format = detectLogoFormat(url, downloaded.contentType) ?: continue
            val pngBytes = normalizeLogoToPng(downloaded.bytes, format) ?: continue
            val area = when (format) {
                LogoFormat.Png -> imageArea(pngBytes)
                LogoFormat.Svg -> extractSvgArea(downloaded.bytes)
                LogoFormat.Ico -> imageArea(pngBytes)
            }
            evaluated += LogoCandidate(
                format = format,
                area = area,
                image = DownloadedBusinessImage(
                    bytes = pngBytes,
                    contentType = "image/png"
                )
            )
        }

        return evaluated
            .filter { it.format == LogoFormat.Png }
            .maxByOrNull { it.area }
            ?.image
            ?: evaluated.filter { it.format == LogoFormat.Svg }
                .maxByOrNull { it.area }
                ?.image
            ?: evaluated.filter { it.format == LogoFormat.Ico }
                .maxByOrNull { it.area }
                ?.image
    }

    private fun buildHostFallbackCandidates(websiteUrl: String?): List<String> {
        val host = runCatching { URI(websiteUrl).host }
            .getOrNull()
            ?.removePrefix("www.")
            ?.takeIf { it.isNotBlank() }
            ?: return emptyList()
        return listOf(
            "https://$host/favicon.ico",
            "https://$host/apple-touch-icon.png"
        )
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
