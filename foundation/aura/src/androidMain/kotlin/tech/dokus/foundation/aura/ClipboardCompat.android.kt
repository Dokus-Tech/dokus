package tech.dokus.foundation.aura

import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry

actual fun createPlainTextClipEntry(text: String): ClipEntry =
    ClipEntry(ClipData.newPlainText("text", text))
