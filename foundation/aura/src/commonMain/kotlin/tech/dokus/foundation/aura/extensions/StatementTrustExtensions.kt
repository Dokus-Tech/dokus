package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.banking_trust_high
import tech.dokus.aura.resources.banking_trust_low
import tech.dokus.aura.resources.banking_trust_medium
import tech.dokus.domain.enums.StatementTrust
import tech.dokus.foundation.aura.style.positionPositive
import tech.dokus.foundation.aura.style.statusWarning

val StatementTrust.localized: String
    @Composable get() = when (this) {
        StatementTrust.High -> stringResource(Res.string.banking_trust_high)
        StatementTrust.Medium -> stringResource(Res.string.banking_trust_medium)
        StatementTrust.Low -> stringResource(Res.string.banking_trust_low)
    }

val StatementTrust.statusColor: Color
    @Composable get() = when (this) {
        StatementTrust.High -> MaterialTheme.colorScheme.positionPositive
        StatementTrust.Medium -> MaterialTheme.colorScheme.statusWarning
        StatementTrust.Low -> MaterialTheme.colorScheme.error
    }
