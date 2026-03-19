package tech.dokus.features.cashflow.presentation.review.components.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
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
import tech.dokus.foundation.aura.style.statusConfirmed
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
private val FactFieldCornerRadius = 6.dp

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
private fun ContactFactDisplay(
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
            .padding(vertical = Constraints.Spacing.xSmall),
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
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.textMuted,
        modifier = modifier.padding(bottom = Constraints.Spacing.xSmall)
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
    val hasValue = !value.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FactFieldCornerRadius))
            .background(
                if (isHovered && isClickable) {
                    MaterialTheme.colorScheme.outline.copy(alpha = HoverBackgroundAlpha + 0.03f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }
            )
            .then(if (isClickable) Modifier.hoverable(interactionSource) else Modifier)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(
                horizontal = Constraints.Spacing.xSmall,
                vertical = 2.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(
                        color = if (hasValue) {
                            MaterialTheme.colorScheme.statusConfirmed.copy(alpha = 0.86f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
                        },
                        shape = CircleShape,
                    )
            )
            Text(
                text = value ?: "—",
                style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                color = if (value != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.textMuted
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (isClickable && (!isLargeScreen || isHovered)) {
                Spacer(Modifier.width(Constraints.Spacing.xSmall))
                Icon(
                    imageVector = Lucide.Pencil,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun contactInitials(name: String): String = name
    .split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.take(1) }
    .ifBlank { "?" }

/**
 * Editable fact field — displays as read-only text, switches to inline input on click.
 * Same visual layout as [FactField] in display mode for seamless transition.
 */
@Composable
fun EditableFactField(
    label: String,
    value: String?,
    onValueChanged: (String) -> Unit,
    isReadOnly: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val isLargeScreen = LocalScreenSize.current.isLarge
    var isEditing by remember { mutableStateOf(false) }
    var hasFocused by remember { mutableStateOf(false) }
    var editFieldValue by remember(value) {
        val text = value.orEmpty()
        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
    }
    val focusRequester = remember { FocusRequester() }
    val hasValue = !value.isNullOrBlank()

    fun commit() {
        isEditing = false
        hasFocused = false
        val trimmed = editFieldValue.text.trim()
        if (trimmed != (value.orEmpty())) {
            onValueChanged(trimmed)
        }
    }

    fun cancel() {
        isEditing = false
        hasFocused = false
        val text = value.orEmpty()
        editFieldValue = TextFieldValue(text, selection = TextRange(text.length))
    }

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FactFieldCornerRadius))
            .background(
                if ((isHovered || isEditing) && !isReadOnly) {
                    MaterialTheme.colorScheme.outline.copy(alpha = HoverBackgroundAlpha + 0.03f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }
            )
            .then(if (!isReadOnly) Modifier.hoverable(interactionSource) else Modifier)
            .then(
                if (!isReadOnly && !isEditing) {
                    Modifier.clickable { isEditing = true }
                } else {
                    Modifier
                }
            )
            .padding(
                horizontal = Constraints.Spacing.xSmall,
                vertical = 2.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(StatusDotSize)
                    .background(
                        color = if (hasValue) {
                            MaterialTheme.colorScheme.statusConfirmed.copy(alpha = 0.86f)
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.85f)
                        },
                        shape = CircleShape,
                    )
            )
            AnimatedContent(
                targetState = isEditing,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                modifier = Modifier.weight(1f),
            ) { editing ->
                if (editing) {
                    BasicTextField(
                        value = editFieldValue,
                        onValueChange = { editFieldValue = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            fontFeatureSettings = "tnum",
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = keyboardType,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { commit() }),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                if (it.isFocused) {
                                    hasFocused = true
                                } else if (hasFocused && isEditing) {
                                    commit()
                                }
                            }
                            .onKeyEvent {
                                if (it.key == Key.Escape) {
                                    cancel()
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                } else {
                    Text(
                        text = value ?: "\u2014",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFeatureSettings = "tnum"),
                        color = if (value != null) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.textMuted
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!isReadOnly && !isEditing && (!isLargeScreen || isHovered)) {
                Spacer(Modifier.width(Constraints.Spacing.xSmall))
                Icon(
                    imageVector = Lucide.Pencil,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

/**
 * Editable amount row — displays as read-only, switches to inline numeric input on click.
 * Same visual layout as [AmountRow] in display mode.
 */
@Composable
fun EditableAmountRow(
    label: String,
    value: String?,
    onValueChanged: (String) -> Unit,
    isTotal: Boolean = false,
    isReadOnly: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    var isEditing by remember { mutableStateOf(false) }
    var hasFocused by remember { mutableStateOf(false) }
    var editFieldValue by remember(value) {
        val text = value.orEmpty()
        mutableStateOf(TextFieldValue(text, selection = TextRange(text.length)))
    }
    val focusRequester = remember { FocusRequester() }

    fun commit() {
        isEditing = false
        hasFocused = false
        val trimmed = editFieldValue.text.trim()
        if (trimmed != (value.orEmpty())) {
            onValueChanged(trimmed)
        }
    }

    fun cancel() {
        isEditing = false
        hasFocused = false
        val text = value.orEmpty()
        editFieldValue = TextFieldValue(text, selection = TextRange(text.length))
    }

    LaunchedEffect(isEditing) {
        if (isEditing) focusRequester.requestFocus()
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(FactFieldCornerRadius))
            .background(
                if ((isHovered || isEditing) && !isReadOnly) {
                    MaterialTheme.colorScheme.outline.copy(alpha = HoverBackgroundAlpha + 0.03f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0f)
                }
            )
            .then(if (!isReadOnly) Modifier.hoverable(interactionSource) else Modifier)
            .then(
                if (!isReadOnly && !isEditing) {
                    Modifier.clickable { isEditing = true }
                } else {
                    Modifier
                }
            )
            .padding(vertical = Constraints.Spacing.xSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
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
        AnimatedContent(
            targetState = isEditing,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
        ) { editing ->
            if (editing) {
                BasicTextField(
                    value = editFieldValue,
                    onValueChange = { editFieldValue = it },
                    singleLine = true,
                    textStyle = if (isTotal) {
                        MaterialTheme.typography.bodyLarge.copy(
                            fontFeatureSettings = "tnum",
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = "tnum",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { commit() }),
                    modifier = Modifier
                        .width(120.dp)
                        .focusRequester(focusRequester)
                        .onFocusChanged {
                            if (it.isFocused) {
                                hasFocused = true
                            } else if (hasFocused && isEditing) {
                                commit()
                            }
                        }
                        .onKeyEvent {
                            if (it.key == Key.Escape) {
                                cancel()
                                true
                            } else {
                                false
                            }
                        },
                )
            } else {
                Text(
                    text = value ?: "\u2014",
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
    }
}

// =============================================================================
// Previews
// =============================================================================

@Preview
@Composable
private fun AmountRowPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        AmountRow(
            label = "Subtotal",
            value = "1,250.00"
        )
    }
}

@Preview
@Composable
private fun MicroLabelPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MicroLabel(text = "AMOUNTS")
    }
}

@Preview
@Composable
private fun FactFieldPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        FactField(
            label = "Invoice Number",
            value = "INV-2024-001",
            onClick = {}
        )
    }
}

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
