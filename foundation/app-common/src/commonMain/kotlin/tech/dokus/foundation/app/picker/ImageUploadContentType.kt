package tech.dokus.foundation.app.picker

fun inferImageContentType(filename: String): String = when {
    filename.endsWith(".png", ignoreCase = true) -> "image/png"
    filename.endsWith(".gif", ignoreCase = true) -> "image/gif"
    filename.endsWith(".webp", ignoreCase = true) -> "image/webp"
    else -> "image/jpeg"
}
