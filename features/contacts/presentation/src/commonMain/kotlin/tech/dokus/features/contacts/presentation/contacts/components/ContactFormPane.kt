package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_close
import tech.dokus.aura.resources.contacts_create_contact
import tech.dokus.aura.resources.contacts_edit_contact
import tech.dokus.aura.resources.contacts_required_fields_hint
import tech.dokus.aura.resources.contacts_update_mobile_hint
import tech.dokus.domain.enums.ClientType
import tech.dokus.features.contacts.mvi.ContactFormData
import tech.dokus.features.contacts.mvi.PotentialDuplicate
import tech.dokus.foundation.aura.components.DokusCardSurface

// UI dimension constants
private val FormContentPadding = 16.dp
private val FormContentSpacing = 16.dp
private val FormBottomSpacing = 8.dp
private val HeaderPaddingStart = 16.dp
private val HeaderPaddingEnd = 8.dp
private val HeaderPaddingTop = 16.dp
private val HeaderPaddingBottom = 8.dp
private val PaneMinWidth = 400.dp
private val PaneMaxWidth = 600.dp
private const val AnimationDurationMs = 200
private const val SlideAnimationDurationMs = 300
private const val ScrimAlpha = 0.32f
private const val PaneWidthFraction = 0.4f

/**
 * Side pane for creating or editing a contact on desktop.
 *
 * This component displays the contact form in a sliding right pane with:
 * - Animated entrance/exit (slide from right)
 * - Semi-transparent backdrop that dismisses on click
 * - Header with title and close button
 * - Scrollable form content using ContactFormContentCompact
 *
 * Follows the same pattern as InvoiceClientSidePanel for consistency.
 *
 * @param isVisible Whether the pane is currently visible
 * @param isEditMode Whether the form is in edit mode (vs create mode)
 * @param formData Current form data containing field values and validation
 * @param isSaving Whether save operation is in progress
 * @param isDeleting Whether delete operation is in progress
 * @param duplicates List of potential duplicate contacts detected
 * @param onDismiss Callback when the pane should be dismissed (backdrop click or close button)
 * @param onNameChange Callback for name field changes
 * @param onEmailChange Callback for email field changes
 * @param onPhoneChange Callback for phone field changes
 * @param onContactPersonChange Callback for contact person field changes
 * @param onVatNumberChange Callback for VAT number field changes
 * @param onCompanyNumberChange Callback for company number field changes
 * @param onBusinessTypeChange Callback for business type selection
 * @param onAddressLine1Change Callback for address line 1 field changes
 * @param onAddressLine2Change Callback for address line 2 field changes
 * @param onCityChange Callback for city field changes
 * @param onPostalCodeChange Callback for postal code field changes
 * @param onCountryChange Callback for country field changes
 * @param onPeppolIdChange Callback for Peppol ID field changes
 * @param onPeppolEnabledChange Callback for Peppol enabled toggle
 * @param onDefaultPaymentTermsChange Callback for payment terms field changes
 * @param onDefaultVatRateChange Callback for VAT rate field changes
 * @param onTagsChange Callback for tags field changes
 * @param onInitialNoteChange Callback for initial note field changes (create mode only)
 * @param onIsActiveChange Callback for active status toggle (edit mode only)
 * @param onSave Callback when save button is pressed
 * @param onCancel Callback when cancel button is pressed
 * @param onDelete Callback when delete button is pressed (edit mode only)
 * @param onDismissDuplicates Callback when user dismisses duplicate warnings
 * @param onMergeWithExisting Callback when user chooses to merge with an existing contact
 * @param modifier Modifier for the root component
 */
@Composable
fun ContactFormPane(
    isVisible: Boolean,
    isEditMode: Boolean,
    formData: ContactFormData,
    isSaving: Boolean,
    isDeleting: Boolean,
    duplicates: List<PotentialDuplicate>,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onContactPersonChange: (String) -> Unit,
    onVatNumberChange: (String) -> Unit,
    onCompanyNumberChange: (String) -> Unit,
    onBusinessTypeChange: (ClientType) -> Unit,
    onAddressLine1Change: (String) -> Unit,
    onAddressLine2Change: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onPeppolIdChange: (String) -> Unit,
    onPeppolEnabledChange: (Boolean) -> Unit,
    onDefaultPaymentTermsChange: (Int) -> Unit,
    onDefaultVatRateChange: (String) -> Unit,
    onTagsChange: (String) -> Unit,
    onInitialNoteChange: (String) -> Unit,
    onIsActiveChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit,
    onDismissDuplicates: () -> Unit,
    onMergeWithExisting: (PotentialDuplicate) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(AnimationDurationMs)),
            exit = fadeOut(tween(AnimationDurationMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Side pane
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(SlideAnimationDurationMs)
            ) + fadeIn(tween(SlideAnimationDurationMs)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(SlideAnimationDurationMs)
            ) + fadeOut(tween(SlideAnimationDurationMs)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                // Calculate pane width: 40% of screen, between 400dp and 600dp
                val paneWidth = (maxWidth * PaneWidthFraction).coerceIn(PaneMinWidth, PaneMaxWidth)

                DokusCardSurface(
                    modifier = Modifier
                        .width(paneWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = MaterialTheme.shapes.medium.copy(
                        topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                        bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                    ),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Header
                        ContactFormPaneHeader(
                            title = if (isEditMode) {
                                stringResource(Res.string.contacts_edit_contact)
                            } else {
                                stringResource(Res.string.contacts_create_contact)
                            },
                            onClose = onDismiss
                        )

                        // Scrollable form content
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = FormContentPadding),
                            verticalArrangement = Arrangement.spacedBy(FormContentSpacing)
                        ) {
                            // Description
                            Text(
                                text = if (isEditMode) {
                                    stringResource(Res.string.contacts_update_mobile_hint)
                                } else {
                                    stringResource(Res.string.contacts_required_fields_hint)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Duplicate warning banner
                            if (duplicates.isNotEmpty()) {
                                DuplicateWarningBanner(
                                    duplicates = duplicates,
                                    onContinueAnyway = onDismissDuplicates,
                                    onMergeWithExisting = onMergeWithExisting,
                                    onCancel = onCancel
                                )
                            }

                            // Form fields
                            ContactFormFields(
                                formData = formData,
                                onNameChange = onNameChange,
                                onEmailChange = onEmailChange,
                                onPhoneChange = onPhoneChange,
                                onContactPersonChange = onContactPersonChange,
                                onVatNumberChange = onVatNumberChange,
                                onCompanyNumberChange = onCompanyNumberChange,
                                onBusinessTypeChange = onBusinessTypeChange,
                                onAddressLine1Change = onAddressLine1Change,
                                onAddressLine2Change = onAddressLine2Change,
                                onCityChange = onCityChange,
                                onPostalCodeChange = onPostalCodeChange,
                                onCountryChange = onCountryChange,
                                onPeppolIdChange = onPeppolIdChange,
                                onPeppolEnabledChange = onPeppolEnabledChange,
                                onDefaultPaymentTermsChange = onDefaultPaymentTermsChange,
                                onDefaultVatRateChange = onDefaultVatRateChange,
                                onTagsChange = onTagsChange,
                                onInitialNoteChange = onInitialNoteChange,
                                onIsActiveChange = onIsActiveChange,
                                showInitialNote = !isEditMode
                            )

                            Spacer(modifier = Modifier.height(FormBottomSpacing))
                        }

                        // Action buttons at bottom
                        ContactFormActionButtonsCompact(
                            isEditMode = isEditMode,
                            isSaving = isSaving,
                            isDeleting = isDeleting,
                            isValid = formData.isValid,
                            onSave = onSave,
                            onCancel = onCancel,
                            onDelete = onDelete,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(FormContentPadding)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Header for the contact form pane with title and close button.
 */
@Composable
private fun ContactFormPaneHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = HeaderPaddingStart,
                end = HeaderPaddingEnd,
                top = HeaderPaddingTop,
                bottom = HeaderPaddingBottom
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(Res.string.action_close),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
