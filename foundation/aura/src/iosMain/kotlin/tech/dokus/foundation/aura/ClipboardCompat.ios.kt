package tech.dokus.foundation.aura

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
actual fun createPlainTextClipEntry(text: String): ClipEntry =
    ClipEntry.withPlainText(text)
