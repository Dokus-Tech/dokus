package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_tap_to_identify
import tech.dokus.aura.resources.cashflow_who_issued_document
import tech.dokus.features.cashflow.presentation.review.ContactSnapshot
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

private val ContactBlockCornerRadius = 6.dp
private val StatusDotSize = 6.dp
private val PencilIconSize = 16.dp
private val ChevronIconSize = 16.dp
private val AttentionBorderAlpha = 0.3f
private val HoverBackgroundAlpha = 0.08f

/**
 * Contact display as a fact block with hover-to-edit behavior.
 *
 * - When contact exists: shows name, VAT, address as text with hover-reveal pencil
 * - When no contact: shows subtle amber border + dot + prompt text
 *
 * @param contact The contact snapshot to display, or null if no contact
 * @param onEditClick Callback when user wants to edit/select contact
 * @param isReadOnly Whether editing is disabled (e.g., confirmed document)
 */
@Composable
fun ContactBlock(
    contact: ContactSnapshot?,
    onEditClick: () -> Unit,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isLargeScreen = LocalScreenSize.current.isLarge

    // On mobile, always show the edit affordance; on desktop, show on hover
    val showEditAffordance = !isReadOnly && (!isLargeScreen || isHovered)

    if (contact != null) {
        // Contact exists - show as fact
        ContactFactDisplay(
            contact = contact,
            isHovered = isHovered,
            showEditAffordance = showEditAffordance,
            isReadOnly = isReadOnly,
            onEditClick = onEditClick,
            interactionSource = interactionSource,
            modifier = modifier
        )
    } else {
        // No contact - show attention prompt
        ContactMissingPrompt(
            onEditClick = onEditClick,
            isReadOnly = isReadOnly,
            interactionSource = interactionSource,
            modifier = modifier
        )
    }
}

@Composable
private fun ContactFactDisplay(
    contact: ContactSnapshot,
    isHovered: Boolean,
    showEditAffordance: Boolean,
    isReadOnly: Boolean,
    onEditClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ContactBlockCornerRadius))
            .background(
                if (isHovered && !isReadOnly) {
                    MaterialTheme.colorScheme.outline.copy(alpha = HoverBackgroundAlpha)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }
            )
            .hoverable(interactionSource)
            .then(
                if (!isReadOnly) {
                    Modifier.clickable(onClick = onEditClick)
                } else {
                    Modifier
                }
            )
            .padding(Constrains.Spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // Name (primary)
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // VAT number (monospace)
                contact.vatNumber?.let { vat ->
                    Text(
                        text = vat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Email
                contact.email?.let { email ->
                    Text(
                        text = email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Edit icon (visible on hover/mobile)
            AnimatedVisibility(
                visible = showEditAffordance,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.size(PencilIconSize)
                )
            }
        }
    }
}

@Composable
private fun ContactMissingPrompt(
    onEditClick: () -> Unit,
    isReadOnly: Boolean,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    val attentionColor = MaterialTheme.colorScheme.statusWarning

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ContactBlockCornerRadius))
            .border(
                width = 1.dp,
                color = attentionColor.copy(alpha = AttentionBorderAlpha),
                shape = RoundedCornerShape(ContactBlockCornerRadius)
            )
            .hoverable(interactionSource)
            .then(
                if (!isReadOnly) {
                    Modifier.clickable(onClick = onEditClick)
                } else {
                    Modifier
                }
            )
            .padding(Constrains.Spacing.small),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            // Small amber dot
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(attentionColor, CircleShape)
            )
            Spacer(Modifier.width(Constrains.Spacing.small))
            Column {
                Text(
                    text = stringResource(Res.string.cashflow_who_issued_document),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!isReadOnly) {
                    Text(
                        text = stringResource(Res.string.cashflow_tap_to_identify),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }
            }
        }

        if (!isReadOnly) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.size(ChevronIconSize)
            )
        }
    }
}

/**
 * Amount display row with tabular numbers.
 *
 * @param label Label text (left-aligned)
 * @param value Money value (right-aligned with tabular numbers)
 * @param isTotal Whether this is a total row (emphasized styling)
 */
@Composable
fun AmountRow(
    label: String,
    value: String?,
    isTotal: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = Constrains.Spacing.xSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isTotal) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.textMuted
            }
        )
        Text(
            text = value ?: "—",
            style = if (isTotal) {
                MaterialTheme.typography.bodyLarge.copy(
                    fontFeatureSettings = "tnum",
                    fontWeight = FontWeight.Medium
                )
            } else {
                MaterialTheme.typography.bodyMedium.copy(
                    fontFeatureSettings = "tnum"
                )
            },
            color = when {
                isTotal && value != null -> MaterialTheme.colorScheme.onSurface
                value != null -> MaterialTheme.colorScheme.onSurfaceVariant
                else -> MaterialTheme.colorScheme.textMuted
            }
        )
    }
}

/**
 * Micro-label for section headers.
 * Use when a group has 3+ lines of content to help scanning.
 */
@Composable
fun MicroLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.textMuted,
        modifier = modifier.padding(bottom = Constrains.Spacing.xSmall)
    )
}

/**
 * Fact field - text display that can become editable on click.
 */
@Composable
fun FactField(
    label: String,
    value: String?,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isLargeScreen = LocalScreenSize.current.isLarge
    val isClickable = onClick != null

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(4.dp))
            .background(
                if (isHovered && isClickable) {
                    MaterialTheme.colorScheme.outline.copy(alpha = HoverBackgroundAlpha)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }
            )
            .then(if (isClickable) Modifier.hoverable(interactionSource) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = Constrains.Spacing.xSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textMuted
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                color = if (value != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.textMuted
                }
            )
            if (isClickable && (!isLargeScreen || isHovered)) {
                Spacer(Modifier.width(Constrains.Spacing.xSmall))
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
