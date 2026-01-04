package tech.dokus.backend.routes.cashflow

import io.ktor.http.content.PartData
import io.ktor.utils.io.readAvailable
import tech.dokus.domain.exceptions.DokusException
import java.io.ByteArrayOutputStream
import kotlin.io.DEFAULT_BUFFER_SIZE

internal suspend fun PartData.FileItem.readBytesWithLimit(maxBytes: Long): ByteArray {
    val channel = provider()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    val outputStream = ByteArrayOutputStream()
    var totalBytes = 0L

    while (true) {
        val read = channel.readAvailable(buffer, 0, buffer.size)
        if (read == -1) break

        totalBytes += read
        if (totalBytes > maxBytes) {
            val maxMb = maxBytes / (1024.0 * 1024.0)
            throw DokusException.Validation.Generic(
                "File size exceeds maximum allowed size (${"%.2f".format(maxMb)}MB)"
            )
        }

        outputStream.write(buffer, 0, read)
    }

    return outputStream.toByteArray()
}
