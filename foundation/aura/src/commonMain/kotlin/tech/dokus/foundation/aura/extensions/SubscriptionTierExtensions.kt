package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.subscription_tier_core
import tech.dokus.aura.resources.subscription_tier_founder
import tech.dokus.aura.resources.subscription_tier_one
import tech.dokus.aura.resources.subscription_tier_self_hosted
import tech.dokus.domain.enums.SubscriptionTier

val SubscriptionTier.localized: String
    @Composable get() = when (this) {
        SubscriptionTier.CoreFounder -> stringResource(Res.string.subscription_tier_founder)
        SubscriptionTier.Core -> stringResource(Res.string.subscription_tier_core)
        SubscriptionTier.One -> stringResource(Res.string.subscription_tier_one)
        SubscriptionTier.SelfHosted -> stringResource(Res.string.subscription_tier_self_hosted)
    }
