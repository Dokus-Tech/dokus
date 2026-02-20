package tech.dokus.foundation.aura.components.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_cancel
import tech.dokus.aura.resources.action_collapse
import tech.dokus.aura.resources.action_edit
import tech.dokus.aura.resources.action_expand
import tech.dokus.aura.resources.action_save
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.status.toColor
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted

/**
 * A collapsible settings section with optional edit mode support.
 *
 * Features:
 * - Collapsible/expandable content
 * - Subtitle shown when collapsed
 * - Status badge in header
 * - Section-level edit mode with Save/Cancel actions
 * - Primary visual emphasis option (for PEPPOL section)
 *
 * Header states:
 * - Collapsed: `[▶ Title] [Subtitle muted] [Status Badge]`
 * - Expanded (view): `[▼ Title] [Edit Button] [Status Badge]`
 * - Expanded (edit): `[▼ Title] [Save] [Cancel]`
 *
 * @param title Section title
 * @param modifier Optional modifier
 * @param subtitle Shown when collapsed (summary info)
 * @param status Status badge in header
 * @param expanded Whether section is expanded
 * @param onToggle Toggle collapse/expand (null = not collapsible)
 * @param primary Primary visual emphasis (elevated background)
 * @param editMode Section is in edit mode
 * @param onEdit Enter edit mode callback
 * @param onSave Save changes callback
 * @param onCancel Cancel edit mode callback
 * @param content Section content
 */
@Composable
fun SettingsSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    status: DataRowStatus? = null,
    expanded: Boolean = true,
    onToggle: (() -> Unit)? = null,
    primary: Boolean = false,
    editMode: Boolean = false,
    onEdit: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val isCollapsible = onToggle != null
    val rotation by animateFloatAsState(if (expanded) 90f else 0f)

    val sectionModifier = if (primary) {
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Constraints.CornerRadius.badge))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(Constraints.Spacing.medium)
    } else {
        modifier
            .fillMaxWidth()
            .padding(vertical = Constraints.Spacing.small)
    }

    Column(modifier = sectionModifier) {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isCollapsible) {
                        Modifier.clickable(onClick = onToggle!!)
                    } else {
                        Modifier
                    }
                )
                .padding(vertical = Constraints.Spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Collapse chevron (only if collapsible)
            if (isCollapsible) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = stringResource(
                        if (expanded) Res.string.action_collapse else Res.string.action_expand
                    ),
                    modifier = Modifier
                        .size(Constraints.IconSize.xSmall)
                        .rotate(rotation),
                    tint = MaterialTheme.colorScheme.textMuted,
                )
                Spacer(Modifier.width(Constraints.Spacing.xSmall))
            }

            // Title
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )

            // Subtitle (only when collapsed)
            if (!expanded && subtitle != null) {
                Spacer(Modifier.width(Constraints.Spacing.medium))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Spacer(Modifier.weight(1f))
            }

            // Actions (Edit/Save/Cancel)
            if (expanded) {
                when {
                    editMode && onSave != null && onCancel != null -> {
                        // Edit mode: Save + Cancel
                        TextButton(onClick = onCancel) {
                            Text(
                                text = stringResource(Res.string.action_cancel),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                        TextButton(onClick = onSave) {
                            Text(
                                text = stringResource(Res.string.action_save),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    !editMode && onEdit != null -> {
                        // View mode: Edit button
                        TextButton(onClick = onEdit) {
                            Text(
                                text = stringResource(Res.string.action_edit),
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }

            // Status badge
            if (status != null) {
                Spacer(Modifier.width(Constraints.Spacing.small))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
                ) {
                    StatusDot(type = status.type)
                    Text(
                        text = status.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = status.type.toColor(),
                    )
                }
            }
        }

        // Content (animated visibility)
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier.padding(top = Constraints.Spacing.small),
            ) {
                content()
            }
        }
    }
}
