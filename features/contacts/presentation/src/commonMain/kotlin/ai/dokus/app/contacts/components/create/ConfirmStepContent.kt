package ai.dokus.app.contacts.components.create

import ai.dokus.app.contacts.viewmodel.CreateContactIntent
import ai.dokus.app.contacts.viewmodel.CreateContactState
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.fields.PTextFieldEmail
import ai.dokus.foundation.design.components.fields.PTextFieldPhone
import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import tech.dokus.domain.Email

/**
 * Confirm step content - review company data and add billing email.
 */
@Composable
fun ConfirmStepContent(
    state: CreateContactState.ConfirmStep,
    onIntent: (CreateContactIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Constrains.Spacing.large)
    ) {
        // Header with back button
        ConfirmHeader(
            onBack = { onIntent(CreateContactIntent.BackToLookup) }
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.large))

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            // Company info card (read-only)
            CompanyInfoCard(
                entity = state.selectedEntity,
                showAddress = state.showAddressDetails,
                onToggleAddress = { onIntent(CreateContactIntent.ToggleAddressDetails) }
            )

            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            // Form fields
            Text(
                text = "Contact Details",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            PTextFieldEmail(
                fieldName = "Billing Email *",
                value = Email(state.billingEmail),
                error = state.emailError?.let {
                    tech.dokus.domain.exceptions.DokusException.Validation.InvalidEmail
                },
                onValueChange = { onIntent(CreateContactIntent.BillingEmailChanged(it.value)) },
                modifier = Modifier.fillMaxWidth()
            )

            PTextFieldPhone(
                fieldName = "Phone (optional)",
                value = tech.dokus.domain.PhoneNumber(state.phone),
                onValueChange = { onIntent(CreateContactIntent.PhoneChanged(it.value)) },
                modifier = Modifier.fillMaxWidth()
            )

            // TODO: Language dropdown (NL/FR/EN)
        }

        // Primary action button
        PPrimaryButton(
            text = if (state.isSubmitting) "Creating..." else "Create Contact",
            enabled = !state.isSubmitting && state.emailError == null,
            loading = state.isSubmitting,
            onClick = { onIntent(CreateContactIntent.ConfirmAndCreate) },
            modifier = Modifier
                .fillMaxWidth()
                .height(Constrains.Height.button)
        )
    }
}

@Composable
private fun ConfirmHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = "Confirm Company",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompanyInfoCard(
    entity: tech.dokus.domain.model.entity.EntityLookup,
    showAddress: Boolean,
    onToggleAddress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium)
        ) {
            // Company name
            Text(
                text = entity.name,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            // VAT number
            if (entity.vatNumber != null) {
                Row {
                    Text(
                        text = "VAT: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entity.vatNumber.value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Country
            if (entity.address?.country != null) {
                Row {
                    Text(
                        text = "Country: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = entity.address.country.dbValue,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Address toggle
            if (entity.address != null) {
                Spacer(modifier = Modifier.height(Constrains.Spacing.small))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleAddress),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAddress) "Hide address" else "Show address",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = if (showAddress) {
                            Icons.Default.KeyboardArrowUp
                        } else {
                            Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                AnimatedVisibility(visible = showAddress) {
                    Column(
                        modifier = Modifier.padding(top = Constrains.Spacing.small)
                    ) {
                        Text(
                            text = entity.address.streetLine1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (entity.address.streetLine2 != null) {
                            Text(
                                text = entity.address.streetLine2,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${entity.address.postalCode} ${entity.address.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
