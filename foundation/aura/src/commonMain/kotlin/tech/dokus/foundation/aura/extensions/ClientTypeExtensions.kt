package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_business
import tech.dokus.aura.resources.contacts_government
import tech.dokus.aura.resources.contacts_individual
import tech.dokus.domain.enums.ClientType

val ClientType.localized: String
    @Composable get() = when (this) {
        ClientType.Individual -> stringResource(Res.string.contacts_individual)
        ClientType.Business -> stringResource(Res.string.contacts_business)
        ClientType.Government -> stringResource(Res.string.contacts_government)
    }
