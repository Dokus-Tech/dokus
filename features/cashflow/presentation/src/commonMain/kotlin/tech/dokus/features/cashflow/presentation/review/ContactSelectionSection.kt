@file:Suppress("UnusedParameter") // reserved params

package tech.dokus.features.cashflow.presentation.review

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Edit2
import compose.icons.feathericons.Link
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_change
import tech.dokus.aura.resources.cashflow_ai_suggested
import tech.dokus.aura.resources.cashflow_bound_to
import tech.dokus.aura.resources.cashflow_choose_different
import tech.dokus.aura.resources.cashflow_client_label
import tech.dokus.aura.resources.cashflow_contact_label
import tech.dokus.aura.resources.cashflow_contact_selected
import tech.dokus.aura.resources.cashflow_no_contact_selected
import tech.dokus.aura.resources.cashflow_saving_contact
import tech.dokus.aura.resources.cashflow_select_contact
import tech.dokus.aura.resources.cashflow_suggested_contact
import tech.dokus.aura.resources.cashflow_supplier_label
import tech.dokus.aura.resources.cashflow_use_this_contact
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.common_vat_value
import tech.dokus.aura.resources.contacts_create_contact
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized

// UI dimension constants
private val ErrorSurfaceCornerRadius = 8.dp
private val ProgressIndicatorSize = 20.dp
private val ProgressIndicatorStrokeWidth = 2.dp
private val PersonIconSize = 32.dp
private val ButtonIconSize = 18.dp
private val ReasonTextTopPadding = 2.dp
private val BadgeCornerRadius = 4.dp
private val BadgeHorizontalPadding = 6.dp
private val BadgeVerticalPadding = 2.dp
private val EditIconSize = 16.dp
private val SpacerWidth = 4.dp
private val LinkIconSize = 20.dp

// Confidence and alpha constants
private const val IconAlpha = 0.6f
private const val ReasonTextAlpha = 0.8f
private const val PercentageMultiplier = 100
private const val HighConfidenceThreshold = 0.9f
private const val MediumConfidenceThreshold = 0.7f
private const val SurfaceColorAlpha = 0.15f

/**
 * Contact selection section for the Document Review screen.
 * Replaces the free-text client/supplier information fields with a contact binding UI.
 *
 * Shows different states:
 * - NoContact: "Select contact" + "Create new" buttons
 * - Suggested: AI suggestion card with "Use this contact" / "Choose different"
 * - Selected: Compact contact card with "Change" button
 * - Loading: When binding contact to backend
 * - ReadOnly: When document is confirmed, shows "Bound to {name}"
 *
 * @param documentType The document type (used for label: "Client" vs "Supplier")
 * @param selectionState Current selection state
 * @param selectedContactSnapshot Contact details when selected
 * @param isBindingContact Whether binding operation is in progress
 * @param isReadOnly Whether the document is confirmed (read-only mode)
 * @param validationError Error message for contact binding failures
 * @param onAcceptSuggestion Callback when user accepts suggested contact
 * @param onChooseDifferent Callback when user wants to choose a different contact
 * @param onSelectContact Callback to open contact picker
 * @param onClearContact Callback to clear selected contact
 * @param onCreateNewContact Callback to open contact creation sheet
 */
@Composable
fun ContactSelectionSection(
    documentType: DocumentType,
    selectionState: ContactSelectionState,
    selectedContactSnapshot: ContactSnapshot?,
    isBindingContact: Boolean,
    isReadOnly: Boolean,
    validationError: DokusException?,
    onAcceptSuggestion: () -> Unit,
    onChooseDifferent: () -> Unit,
    onSelectContact: () -> Unit,
    onClearContact: () -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sectionLabel = when (documentType) {
        DocumentType.Invoice -> stringResource(Res.string.cashflow_client_label)
        DocumentType.Bill -> stringResource(Res.string.cashflow_supplier_label)
        else -> stringResource(Res.string.cashflow_contact_label)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Section header
        Text(
            text = sectionLabel,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = Constrains.Spacing.small),
        )

        // Validation error
        if (validationError != null) {
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                shape = RoundedCornerShape(ErrorSurfaceCornerRadius),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = Constrains.Spacing.small),
            ) {
                Text(
                    text = validationError.localized,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(Constrains.Spacing.small),
                )
            }
        }

        // Content based on state
        AnimatedContent(
            targetState = Triple(selectionState, isBindingContact, isReadOnly),
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ContactSelectionContent",
        ) { (state, binding, readOnly) ->
            when {
                binding -> {
                    LoadingState()
                }
                readOnly && state is ContactSelectionState.Selected && selectedContactSnapshot != null -> {
                    BoundContactCard(
                        snapshot = selectedContactSnapshot,
                        sectionLabel = sectionLabel,
                    )
                }
                state is ContactSelectionState.NoContact -> {
                    NoContactState(
                        sectionLabel = sectionLabel,
                        onSelectContact = onSelectContact,
                        onCreateNewContact = onCreateNewContact,
                    )
                }
                state is ContactSelectionState.Suggested -> {
                    SuggestedContactCard(
                        suggestion = state,
                        onAccept = onAcceptSuggestion,
                        onChooseDifferent = onChooseDifferent,
                    )
                }
                state is ContactSelectionState.Selected && selectedContactSnapshot != null -> {
                    SelectedContactCard(
                        snapshot = selectedContactSnapshot,
                        onChangeContact = onChooseDifferent,
                    )
                }
                state is ContactSelectionState.Selected -> {
                    // Selected but no snapshot - fallback to ID display
                    SelectedContactCard(
                        snapshot = null,
                        onChangeContact = onChooseDifferent,
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingState(
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(ProgressIndicatorSize),
                strokeWidth = ProgressIndicatorStrokeWidth,
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Text(
                text = stringResource(Res.string.cashflow_saving_contact),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun NoContactState(
    sectionLabel: String,
    onSelectContact: () -> Unit,
    onCreateNewContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(PersonIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = IconAlpha),
            )
            Spacer(modifier = Modifier.height(Constrains.Spacing.small))
            Text(
                text = stringResource(Res.string.cashflow_no_contact_selected, sectionLabel),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            ) {
                Button(
                    onClick = onSelectContact,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonIconSize),
                    )
                    Spacer(modifier = Modifier.width(Constrains.Spacing.xSmall))
                    Text(stringResource(Res.string.cashflow_select_contact))
                }

                OutlinedButton(
                    onClick = onCreateNewContact,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonIconSize),
                    )
                    Spacer(modifier = Modifier.width(Constrains.Spacing.xSmall))
                    Text(stringResource(Res.string.contacts_create_contact))
                }
            }
        }
    }
}

@Composable
private fun SuggestedContactCard(
    suggestion: ContactSelectionState.Suggested,
    onAccept: () -> Unit,
    onChooseDifferent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val displayName = suggestion.name.takeIf { it.isNotBlank() }
        ?: stringResource(Res.string.common_unknown)
    val reasonText = when (val reason = suggestion.reason) {
        ContactSuggestionReason.AiSuggested -> stringResource(Res.string.cashflow_ai_suggested)
        is ContactSuggestionReason.Custom -> reason.value
    }

    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
        ) {
            // Header: "Suggested contact" with confidence badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.cashflow_suggested_contact),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                )
                if (suggestion.confidence > 0) {
                    ConfidenceBadge(confidence = suggestion.confidence)
                }
            }

            Spacer(modifier = Modifier.height(Constrains.Spacing.small))

            // Contact details
            Text(
                text = displayName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (suggestion.vatNumber != null) {
                Text(
                    text = stringResource(Res.string.common_vat_value, suggestion.vatNumber),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (reasonText.isNotBlank()) {
                Text(
                    text = reasonText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = ReasonTextAlpha),
                    modifier = Modifier.padding(top = ReasonTextTopPadding),
                )
            }

            Spacer(modifier = Modifier.height(Constrains.Spacing.medium))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            ) {
                Button(
                    onClick = onAccept,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonIconSize),
                    )
                    Spacer(modifier = Modifier.width(Constrains.Spacing.xSmall))
                    Text(stringResource(Res.string.cashflow_use_this_contact))
                }

                OutlinedButton(
                    onClick = onChooseDifferent,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.cashflow_choose_different))
                }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(
    confidence: Float,
    modifier: Modifier = Modifier,
) {
    val percentage = (confidence * PercentageMultiplier).toInt()
    val color = when {
        confidence >= HighConfidenceThreshold -> MaterialTheme.colorScheme.tertiary
        confidence >= MediumConfidenceThreshold -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Surface(
        color = color.copy(alpha = SurfaceColorAlpha),
        shape = RoundedCornerShape(BadgeCornerRadius),
        modifier = modifier,
    ) {
        Text(
            text = stringResource(Res.string.common_percent_value, percentage),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = BadgeHorizontalPadding, vertical = BadgeVerticalPadding),
        )
    }
}

@Composable
private fun SelectedContactCard(
    snapshot: ContactSnapshot?,
    onChangeContact: () -> Unit,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (snapshot != null) {
                    Text(
                        text = snapshot.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (snapshot.vatNumber != null) {
                        Text(
                            text = stringResource(Res.string.common_vat_value, snapshot.vatNumber),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (snapshot.email != null) {
                        Text(
                            text = snapshot.email,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    Text(
                        text = stringResource(Res.string.cashflow_contact_selected),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            TextButton(onClick = onChangeContact) {
                PIcon(
                    icon = FeatherIcons.Edit2,
                    description = stringResource(Res.string.action_change),
                    modifier = Modifier.size(EditIconSize),
                )
                Spacer(modifier = Modifier.width(SpacerWidth))
                Text(stringResource(Res.string.action_change))
            }
        }
    }
}

@Composable
private fun BoundContactCard(
    snapshot: ContactSnapshot,
    sectionLabel: String,
    modifier: Modifier = Modifier,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = FeatherIcons.Link,
                contentDescription = null,
                modifier = Modifier.size(LinkIconSize),
                tint = MaterialTheme.colorScheme.tertiary,
            )
            Spacer(modifier = Modifier.width(Constrains.Spacing.small))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.cashflow_bound_to, sectionLabel),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
                Text(
                    text = snapshot.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (snapshot.vatNumber != null) {
                    Text(
                        text = stringResource(Res.string.common_vat_value, snapshot.vatNumber),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
