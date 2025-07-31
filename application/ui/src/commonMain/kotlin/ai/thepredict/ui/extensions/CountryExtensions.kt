package ai.thepredict.ui.extensions

import ai.thepredict.domain.model.Country
import androidx.compose.runtime.Composable

val Country.localized
    @Composable get() = when (this) {
        Country.BE -> "Belgium"
    }