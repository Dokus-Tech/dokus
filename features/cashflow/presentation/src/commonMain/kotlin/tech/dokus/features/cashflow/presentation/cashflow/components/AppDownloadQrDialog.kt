package tech.dokus.features.cashflow.presentation.cashflow.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.app_download_description
import tech.dokus.aura.resources.app_download_platforms
import tech.dokus.aura.resources.app_download_qr_code
import tech.dokus.aura.resources.app_download_title
import tech.dokus.foundation.aura.components.dialog.DokusDialog
import tech.dokus.foundation.aura.components.dialog.DokusDialogAction
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Dialog showing a QR code for downloading the mobile application.
 *
 * @param isVisible Whether the dialog is visible
 * @param onDismiss Called when the dialog should be closed
 */
@Composable
fun AppDownloadQrDialog(
    isVisible: Boolean,
    onDismiss: () -> Unit
) {
    if (!isVisible) return

    DokusDialog(
        onDismissRequest = onDismiss,
        title = stringResource(Res.string.app_download_title),
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)
            ) {
                Text(
                    text = stringResource(Res.string.app_download_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                // QR Code placeholder
                // TODO: Replace with actual QR code generation library
                QrCodePlaceholder()

                Text(
                    text = stringResource(Res.string.app_download_platforms),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        },
        primaryAction = DokusDialogAction(
            text = stringResource(Res.string.action_close),
            onClick = onDismiss
        )
    )
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun AppDownloadQrDialogPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        AppDownloadQrDialog(
            isVisible = true,
            onDismiss = {}
        )
    }
}

@Composable
private fun QrCodePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(200.dp)
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp)
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCode2,
                contentDescription = stringResource(Res.string.app_download_qr_code),
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.app_download_qr_code),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
