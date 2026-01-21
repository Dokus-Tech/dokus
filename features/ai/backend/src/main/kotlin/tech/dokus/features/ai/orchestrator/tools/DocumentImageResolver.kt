package tech.dokus.features.ai.orchestrator.tools

import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

internal class DocumentImageResolver(
    private val imageCache: DocumentImageCache
) {
    suspend fun resolve(imagesInput: String): List<DocumentImage> {
        val imageLines = imagesInput.trim().lines().map { it.trim() }.filter { it.isNotBlank() }
        if (imageLines.isEmpty()) {
            throw IllegalArgumentException("No images provided for processing")
        }

        val resolved = mutableListOf<DocumentImage>()
        imageLines.forEachIndexed { index, token ->
            val imageToken = extractImageToken(token)
            val cached = imageCache.get(imageToken)
            resolved += cached ?: decodeBase64(imageToken, index)
        }
        return resolved
    }

    private fun extractImageToken(token: String): String {
        val trimmed = token.trim()
        val colonIndex = trimmed.indexOf(':')
        if (colonIndex >= 0 && colonIndex + 1 < trimmed.length) {
            return trimmed.substring(colonIndex + 1).trim()
        }
        return trimmed
    }

    private fun decodeBase64(token: String, index: Int): DocumentImage {
        val bytes = try {
            Base64.getDecoder().decode(token)
        } catch (e: Exception) {
            throw IllegalArgumentException("Invalid image reference or base64 data: ${e.message}")
        }
        return DocumentImage(pageNumber = index + 1, imageBytes = bytes)
    }
}
