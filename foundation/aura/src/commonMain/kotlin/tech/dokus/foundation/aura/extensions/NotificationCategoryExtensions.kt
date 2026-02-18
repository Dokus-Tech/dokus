package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.today_notifications_category_billing
import tech.dokus.aura.resources.today_notifications_category_compliance
import tech.dokus.aura.resources.today_notifications_category_peppol
import tech.dokus.domain.enums.NotificationCategory

/**
 * Extension property to get a localized display name for a NotificationCategory.
 */
val NotificationCategory.localized: String
    @Composable get() = when (this) {
        NotificationCategory.Peppol -> stringResource(Res.string.today_notifications_category_peppol)
        NotificationCategory.Compliance -> stringResource(Res.string.today_notifications_category_compliance)
        NotificationCategory.Billing -> stringResource(Res.string.today_notifications_category_billing)
    }
