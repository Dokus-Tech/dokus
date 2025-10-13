package ai.dokus.foundation.ui.extensions

import ai.dokus.foundation.domain.model.Country
import androidx.compose.runtime.Composable

val Country.localized
    @Composable get() = when (this) {
        Country.BE -> "Belgium"
    }