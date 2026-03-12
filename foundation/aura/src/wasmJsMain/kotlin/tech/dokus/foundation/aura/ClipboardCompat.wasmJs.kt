package tech.dokus.foundation.aura

import androidx.compose.ui.platform.ClipEntry

actual fun createPlainTextClipEntry(text: String): ClipEntry =
    ClipEntry.withPlainText(text)
