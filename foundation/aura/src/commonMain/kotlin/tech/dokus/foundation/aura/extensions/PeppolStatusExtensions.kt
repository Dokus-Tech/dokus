package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.peppol_status_delivered
import tech.dokus.aura.resources.peppol_status_failed
import tech.dokus.aura.resources.peppol_status_failed_retryable
import tech.dokus.aura.resources.peppol_status_pending
import tech.dokus.aura.resources.peppol_status_queued
import tech.dokus.aura.resources.peppol_status_rejected
import tech.dokus.aura.resources.peppol_status_sending
import tech.dokus.aura.resources.peppol_status_sent
import tech.dokus.domain.enums.PeppolStatus

val PeppolStatus.localized: String
    @Composable get() = when (this) {
        PeppolStatus.Queued -> stringResource(Res.string.peppol_status_queued)
        PeppolStatus.Sending -> stringResource(Res.string.peppol_status_sending)
        PeppolStatus.Pending -> stringResource(Res.string.peppol_status_pending)
        PeppolStatus.Sent -> stringResource(Res.string.peppol_status_sent)
        PeppolStatus.Delivered -> stringResource(Res.string.peppol_status_delivered)
        PeppolStatus.FailedRetryable -> stringResource(Res.string.peppol_status_failed_retryable)
        PeppolStatus.Failed -> stringResource(Res.string.peppol_status_failed)
        PeppolStatus.Rejected -> stringResource(Res.string.peppol_status_rejected)
    }
