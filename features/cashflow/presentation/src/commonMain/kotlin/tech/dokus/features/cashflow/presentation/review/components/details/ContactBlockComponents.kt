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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_contact_detected_label
import tech.dokus.aura.resources.cashflow_contact_suggested_label
import tech.dokus.aura.resources.cashflow_tap_to_identify
import tech.dokus.aura.resources.cashflow_who_issued_document
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val ContactBlockCornerRadius = 6.dp
private val StatusDotSize = 6.dp
private val PencilIconSize = 16.dp
private val ChevronIconSize = 16.dp
private val AttentionBorderAlpha = 0.3f
private val HoverBackgroundAlpha = 0.08f

/**
 * Contact display as a fact block with hover-to-edit behavior.
 * Renders based on [ResolvedContact] sealed subtype:
 * - Linked: full contact display (avatar, name, VAT, email)
 * - Suggested/Detected: contact display with subtle indicator label
 * - Unknown: amber attention prompt
 *
 * @param displayState The resolved contact from the backend
 * @param onEditClick Callback when user wants to edit/select contact
 * @param isReadOnly Whether editing is disabled (e.g., confirmed document)
 */
@Composable
fun ContactBlock(
    displayState: ResolvedContact,
    onEditClick: () -> Unit,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isLargeScreen = LocalScreenSize.current.isLarge
    val showEditAffordance = !isReadOnly && (!isLargeScreen || isHovered)

    when (displayState) {
        is ResolvedContact.Linked -> ContactFactDisplay(
            name = displayState.name,
            vatNumber = displayState.vatNumber,
            email = displayState.email,
            avatarPath = displayState.avatarPath,
            subtitle = null,
            isHovered = isHovered,
            showEditAffordance = showEditAffordance,
            isReadOnly = isReadOnly,
            onEditClick = onEditClick,
            interactionSource = interactionSource,
            modifier = modifier,
        )

        is ResolvedContact.Suggested -> ContactFactDisplay(
            name = displayState.name,
            vatNumber = displayState.vatNumber,
            email = null,
            avatarPath = null,
            subtitle = stringResource(Res.string.cashflow_contact_suggested_label),
            isHovered = isHovered,
            showEditAffordance = showEditAffordance,
            isReadOnly = isReadOnly,
            onEditClick = onEditClick,
            interactionSource = interactionSource,
            modifier = modifier,
        )

        is ResolvedContact.Detected -> ContactFactDisplay(
            name = displayState.name,
            vatNumber = displayState.vatNumber,
            email = null,
            avatarPath = null,
            subtitle = stringResource(Res.string.cashflow_contact_detected_label),
            isHovered = isHovered,
            showEditAffordance = showEditAffordance,
            isReadOnly = isReadOnly,
            onEditClick = onEditClick,
            interactionSource = interactionSource,
            modifier = modifier,
        )

        is ResolvedContact.Unknown -> ContactMissingPrompt(
            onEditClick = onEditClick,
            isReadOnly = isReadOnly,
            interactionSource = interactionSource,
            modifier = modifier,
        )
    }
}

@Composable
internal fun ContactFactDisplay(
    name: String,
    vatNumber: String?,
    email: String?,
    avatarPath: String?,
    subtitle: String?,
    isHovered: Boolean,
    showEditAffordance: Boolean,
    isReadOnly: Boolean,
    onEditClick: () -> Unit,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier
) {
    val imageLoader = rememberAuthenticatedImageLoader()
    val avatarUrl = rememberResolvedApiUrl(avatarPath)

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
            .padding(Constraints.Spacing.small)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            verticalAlignment = Alignment.Top
        ) {
            CompanyAvatarImage(
                avatarUrl = avatarUrl,
                initial = contactInitials(name),
                size = AvatarSize.Small,
                shape = AvatarShape.RoundedSquare,
                imageLoader = imageLoader,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                vatNumber?.let { vat ->
                    Text(
                        text = vat,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                email?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }

            AnimatedVisibility(
                visible = showEditAffordance,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Lucide.Pencil,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.size(PencilIconSize)
                )
            }
        }
    }
}

@Composable
internal fun ContactMissingPrompt(
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
            .padding(Constraints.Spacing.small),
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
            Spacer(Modifier.width(Constraints.Spacing.small))
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
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.textMuted,
                modifier = Modifier.size(ChevronIconSize)
            )
        }
    }
}

internal fun contactInitials(name: String): String = name
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1) }
    .ifBlank { "?" }

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun ContactBlockEmptyPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ContactBlock(
            displayState = ResolvedContact.Unknown,
            onEditClick = {}
        )
    }
}
