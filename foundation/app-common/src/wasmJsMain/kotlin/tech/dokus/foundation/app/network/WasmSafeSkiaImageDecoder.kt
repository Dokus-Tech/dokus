package tech.dokus.foundation.app.network

import coil3.ImageLoader
import coil3.asImage
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.use
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image as SkiaImage

/**
 * Avoids Coil's worker-based browser decoder on WASM.
 *
 * Safari has been unreliable with the worker/OffscreenCanvas/createImageBitmap path, while
 * Skia's direct decode from encoded bytes works consistently for the image types we serve.
 */
internal class WasmSafeSkiaImageDecoder(
    private val imageSource: coil3.decode.ImageSource,
) : Decoder {
    override suspend fun decode(): DecodeResult {
        val bytes = imageSource.source().use { it.readByteArray() }
        val image = SkiaImage.makeFromEncoded(bytes)

        return try {
            val bitmap = Bitmap.makeFromImage(image)
            bitmap.setImmutable()
            DecodeResult(
                image = bitmap.asImage(),
                isSampled = false,
            )
        } finally {
            image.close()
        }
    }

    internal class Factory : Decoder.Factory {
        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder {
            return WasmSafeSkiaImageDecoder(result.source)
        }
    }
}
