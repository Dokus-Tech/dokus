package tech.dokus.features.auth.presentation.auth.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_email
import tech.dokus.aura.resources.profile_email_not_verified
import tech.dokus.aura.resources.profile_email_verification
import tech.dokus.aura.resources.profile_email_verified
import tech.dokus.aura.resources.profile_personal_info
import tech.dokus.aura.resources.profile_resend_verification
import tech.dokus.domain.model.User
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.settings.SettingsRow
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Account card: email, name, verification status, verify/edit actions.
 */
@Composable
internal fun AccountCard(
    user: User,
    isResendingVerification: Boolean,
    onResendVerification: () -> Unit,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fullName = userDisplayName(user)
    val isVerified = user.emailVerified

    DokusCardSurface(modifier = modifier.fillMaxWidth()) {
        Column {
            SettingsRow(label = stringResource(Res.string.profile_email), value = user.email.value, mono = true)
            SettingsRow(label = stringResource(Res.string.profile_personal_info), value = fullName)
            SettingsRow(
                label = stringResource(Res.string.profile_email_verification),
                trailing = {
                    StatusDot(
                        type = if (isVerified) StatusDotType.Confirmed else StatusDotType.Warning,
                    )
                    Text(
                        text = if (isVerified) {
                            stringResource(Res.string.profile_email_verified)
                        } else {
                            stringResource(Res.string.profile_email_not_verified)
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isVerified) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                },
            )
            if (!isVerified) {
                SettingsRow(
                    label = stringResource(Res.string.profile_resend_verification),
                    chevron = true,
                    showDivider = false,
                    onClick = onResendVerification,
                )
            }
        }
    }
}

@Preview
@Composable
private fun AccountCardPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        AccountCard(
            user = previewUser,
            isResendingVerification = false,
            onResendVerification = {},
            onEditClick = {},
        )
    }
}
