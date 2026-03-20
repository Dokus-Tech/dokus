package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_contact_info
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.Name
import tech.dokus.domain.Email
import tech.dokus.domain.PhoneNumber
import kotlinx.datetime.LocalDateTime

@Composable
internal fun ContactInfoSection(
    state: DokusState<ContactDto>,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.contacts_contact_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> ContactInfoSkeleton()
                is DokusState.Success -> ContactInfoContent(
                    contact = state.data
                )
                is DokusState.Error -> {
                    DokusErrorBanner(
                        exception = state.exception,
                        retryHandler = state.retryHandler,
                    )
                }
            }
        }
    }
}

// ============================================================================
// PREVIEWS
// ============================================================================

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun ContactInfoSectionPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    val now = LocalDateTime(2026, 1, 15, 10, 0)
    TestWrapper(parameters) {
        ContactInfoSection(
            state = DokusState.success(
                ContactDto(
                    id = ContactId.generate(),
                    tenantId = TenantId.generate(),
                    name = Name("Acme Corporation"),
                    email = Email("info@acme.be"),
                    phone = PhoneNumber("+32 2 123 45 67"),
                    vatNumber = VatNumber("BE0123456789"),
                    defaultPaymentTerms = 30,
                    isActive = true,
                    createdAt = now,
                    updatedAt = now
                )
            )
        )
    }
}
