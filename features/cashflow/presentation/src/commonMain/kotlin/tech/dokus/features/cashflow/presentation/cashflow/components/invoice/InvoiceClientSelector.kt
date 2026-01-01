package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.invoice_client
import tech.dokus.aura.resources.invoice_select_client
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.contact.ContactDto
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

/**
 * Client selector dropdown for invoice creation.
 */
@Composable
fun InvoiceClientSelector(
    selectedClient: ContactDto?,
    clientsState: DokusState<List<ContactDto>>,
    onSelectClient: (ContactDto?) -> Unit,
    error: DokusException?,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.invoice_client),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box {
            DokusCardSurface(
                modifier = Modifier.fillMaxWidth(),
                onClick = { expanded = true },
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedClient?.name?.value ?: stringResource(Res.string.invoice_select_client),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (selectedClient != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    if (clientsState is DokusState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = expanded && clientsState is DokusState.Success,
                onDismissRequest = { expanded = false }
            ) {
                (clientsState as? DokusState.Success)?.data?.forEach { client ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(
                                    text = client.name.value,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                client.email?.let { email ->
                                    Text(
                                        text = email.value,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelectClient(client)
                            expanded = false
                        }
                    )
                }
            }
        }

        error?.let { exception ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = exception.localized,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}
