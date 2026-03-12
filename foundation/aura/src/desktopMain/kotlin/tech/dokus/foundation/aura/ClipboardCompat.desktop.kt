package tech.dokus.foundation.aura

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry
import java.awt.datatransfer.StringSelection

@OptIn(ExperimentalComposeUiApi::class)
actual fun createPlainTextClipEntry(text: String): ClipEntry =
    ClipEntry(StringSelection(text))
