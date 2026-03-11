package tech.dokus.features.contacts.presentation.contacts.components.create

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_view
import tech.dokus.aura.resources.contacts_duplicate_exists
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.features.contacts.mvi.DuplicateVatUi
import tech.dokus.foundation.aura.components.common.CalloutVariant
import tech.dokus.foundation.aura.components.common.DokusCalloutBanner
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * Banner shown when a contact with the same VAT number already exists.
 * This is a hard block - user cannot create duplicate VAT contacts.
 */
@Composable
fun DuplicateVatBanner(
    duplicate: DuplicateVatUi,
    onViewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCalloutBanner(
        title = stringResource(Res.string.contacts_duplicate_exists),
        subtitle = "${duplicate.displayName} (${duplicate.vatNumber})",
        modifier = modifier,
        variant = CalloutVariant.Filled(color = MaterialTheme.colorScheme.error),
        trailing = {
            TextButton(onClick = onViewContact) {
                Text(
                    text = stringResource(Res.string.action_view),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun DuplicateVatBannerPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        DuplicateVatBanner(
            duplicate = DuplicateVatUi(
                contactId = ContactId.generate(),
                displayName = "Acme Corporation",
                vatNumber = VatNumber("BE0123456789"),
            ),
            onViewContact = {},
        )
    }
}
