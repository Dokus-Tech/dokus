package tech.dokus.app.share

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
actual fun ShareImportBackHandler(enabled: Boolean) {
    BackHandler(enabled = enabled) {
        // Block system back while the import is locked.
    }
}
