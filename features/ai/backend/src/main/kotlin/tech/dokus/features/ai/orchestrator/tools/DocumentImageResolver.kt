package tech.dokus.features.ai.orchestrator.tools

import tech.dokus.features.ai.services.DocumentImageCache
import tech.dokus.features.ai.services.DocumentImageService.DocumentImage
import java.util.Base64

internal class DocumentImageResolver(
    private val imageCache: DocumentImageCache
) {
    fun resolve(imagesInput: String): List<DocumentImage> {
        val imageLines = imagesInput.trim().lines().map { it.trim() }.filter { it.isNotBlank() }
        if (imageLines.isEmpty()) {
            throw IllegalArgumentException("No images provided for processing")
        }

        return imageLines.mapIndexed { index, token ->
            imageCache.get(token) ?: decodeBase64(token, index)
        }
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
