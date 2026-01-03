package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
import tech.dokus.foundation.aura.components.common.DokusErrorContent

@Composable
internal fun ContactInfoSection(
    state: DokusState<ContactDto>,
    onPeppolToggle: (Boolean) -> Unit,
    isTogglingPeppol: Boolean,
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
                    contact = state.data,
                    onPeppolToggle = onPeppolToggle,
                    isTogglingPeppol = isTogglingPeppol
                )
                is DokusState.Error -> {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusErrorContent(
                            exception = state.exception,
                            retryHandler = state.retryHandler
                        )
                    }
                }
            }
        }
    }
}
