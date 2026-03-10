package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_match_by_auto
import tech.dokus.aura.resources.banking_match_by_manual
import tech.dokus.aura.resources.banking_match_by_review
import tech.dokus.domain.enums.MatchedBy

val MatchedBy.localized: String
    @Composable get() = when (this) {
        MatchedBy.Auto -> stringResource(Res.string.banking_match_by_auto)
        MatchedBy.Review -> stringResource(Res.string.banking_match_by_review)
        MatchedBy.Manual -> stringResource(Res.string.banking_match_by_manual)
    }
