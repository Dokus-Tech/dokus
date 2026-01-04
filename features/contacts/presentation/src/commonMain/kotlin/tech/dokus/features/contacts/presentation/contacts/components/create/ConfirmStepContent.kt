package tech.dokus.features.contacts.presentation.contacts.components.create

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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.common_country_value
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.contacts_billing_email
import tech.dokus.aura.resources.contacts_confirm_company
import tech.dokus.aura.resources.contacts_contact_details
import tech.dokus.aura.resources.contacts_create_contact
import tech.dokus.aura.resources.contacts_creating
import tech.dokus.aura.resources.contacts_hide_address
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_show_address
import tech.dokus.aura.resources.country_belgium
import tech.dokus.aura.resources.country_france
import tech.dokus.aura.resources.country_netherlands
import tech.dokus.aura.resources.field_optional
import tech.dokus.domain.enums.Country
import tech.dokus.domain.model.entity.EntityLookup
import tech.dokus.features.contacts.mvi.CreateContactIntent
import tech.dokus.features.contacts.mvi.CreateContactState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.fields.PTextFieldEmail
import tech.dokus.foundation.aura.components.fields.PTextFieldPhone
import tech.dokus.foundation.aura.constrains.Constrains

/**
 * Confirm step content - review company data and add billing email.
 */
@Composable
fun ConfirmStepContent(
    state: CreateContactState.ConfirmStep,
    headerTitle: String,
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
            title = headerTitle,
            onBack = { onIntent(CreateContactIntent.BackToLookup) }
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            Text(
                text = stringResource(Res.string.contacts_confirm_company),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Company info card (read-only)
            CompanyInfoCard(
                entity = state.selectedEntity,
                showAddress = state.showAddressDetails,
                onToggleAddress = { onIntent(CreateContactIntent.ToggleAddressDetails) }
            )

            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            // Form fields
            Text(
                text = stringResource(Res.string.contacts_contact_details),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            PTextFieldEmail(
                fieldName = stringResource(
                    Res.string.field_optional,
                    stringResource(Res.string.contacts_billing_email)
                ),
                value = state.billingEmail,
                error = state.emailError,
                onValueChange = { onIntent(CreateContactIntent.BillingEmailChanged(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            PTextFieldPhone(
                fieldName = stringResource(
                    Res.string.field_optional,
                    stringResource(Res.string.contacts_phone)
                ),
                value = state.phone,
                onValueChange = { onIntent(CreateContactIntent.PhoneChanged(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            // TODO: Language dropdown (NL/FR/EN)
        }

        // Primary action button
        PPrimaryButton(
            text = if (state.isSubmitting) {
                stringResource(Res.string.contacts_creating)
            } else {
                stringResource(Res.string.contacts_create_contact)
            },
            enabled = !state.isSubmitting && state.emailError == null,
            isLoading = state.isSubmitting,
            onClick = { onIntent(CreateContactIntent.ConfirmAndCreate) },
            modifier = Modifier
                .fillMaxWidth()
                .height(Constrains.Height.button)
        )
    }
}

@Composable
private fun ConfirmHeader(
    title: String,
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
                contentDescription = stringResource(Res.string.action_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun CompanyInfoCard(
    entity: EntityLookup,
    showAddress: Boolean,
    onToggleAddress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium)
        ) {
            // Company name
            Text(
                text = entity.name.value,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            // VAT number
            val vatNumber = entity.vatNumber
            Text(
                text = stringResource(Res.string.common_vat_value, vatNumber.value),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Country
            val address = entity.address
            val country = address?.country
            if (country != null) {
                Text(
                    text = stringResource(Res.string.common_country_value, country.localizedName()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Address toggle
            if (address != null) {
                Spacer(modifier = Modifier.height(Constrains.Spacing.small))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onToggleAddress),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (showAddress) {
                            stringResource(Res.string.contacts_hide_address)
                        } else {
                            stringResource(Res.string.contacts_show_address)
                        },
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
                            text = address.streetLine1,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val streetLine2 = address.streetLine2
                        if (streetLine2 != null) {
                            Text(
                                text = streetLine2,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = "${address.postalCode} ${address.city}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Country.localizedName(): String =
    when (this) {
        Country.Belgium -> stringResource(Res.string.country_belgium)
        Country.Netherlands -> stringResource(Res.string.country_netherlands)
        Country.France -> stringResource(Res.string.country_france)
    }
