package tech.dokus.foundation.aura.extensions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Inbox
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.notification_type_compliance_blocker
import tech.dokus.aura.resources.notification_type_payment_confirmed
import tech.dokus.aura.resources.notification_type_payment_failed
import tech.dokus.aura.resources.notification_type_peppol_received
import tech.dokus.aura.resources.notification_type_peppol_send_confirmed
import tech.dokus.aura.resources.notification_type_peppol_send_failed
import tech.dokus.aura.resources.notification_type_subscription_changed
import tech.dokus.aura.resources.notification_type_vat_warning
import tech.dokus.domain.enums.NotificationType

/**
 * Extension property to get a localized display name for a NotificationType.
 */
val NotificationType.localized: String
    @Composable get() = when (this) {
        NotificationType.PeppolReceived -> stringResource(Res.string.notification_type_peppol_received)
        NotificationType.PeppolSendConfirmed -> stringResource(Res.string.notification_type_peppol_send_confirmed)
        NotificationType.PeppolSendFailed -> stringResource(Res.string.notification_type_peppol_send_failed)
        NotificationType.ComplianceBlocker -> stringResource(Res.string.notification_type_compliance_blocker)
        NotificationType.VatWarning -> stringResource(Res.string.notification_type_vat_warning)
        NotificationType.PaymentConfirmed -> stringResource(Res.string.notification_type_payment_confirmed)
        NotificationType.PaymentFailed -> stringResource(Res.string.notification_type_payment_failed)
        NotificationType.SubscriptionChanged -> stringResource(Res.string.notification_type_subscription_changed)
    }

/**
 * Whether this notification type represents a critical/urgent event.
 */
val NotificationType.isCritical: Boolean
    get() = when (this) {
        NotificationType.PeppolSendFailed,
        NotificationType.ComplianceBlocker,
        NotificationType.PaymentFailed -> true

        NotificationType.PeppolReceived,
        NotificationType.PeppolSendConfirmed,
        NotificationType.VatWarning,
        NotificationType.PaymentConfirmed,
        NotificationType.SubscriptionChanged -> false
    }

/**
 * Extension property to get the appropriate icon for a NotificationType.
 */
val NotificationType.icon: ImageVector
    get() = if (isCritical) Icons.Default.Warning else FeatherIcons.Inbox

/**
 * Extension property to get the icon tint color for a NotificationType.
 */
val NotificationType.iconTint: Color
    @Composable get() = if (isCritical) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.primary
    }
