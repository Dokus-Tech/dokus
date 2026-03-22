package tech.dokus.features.cashflow.presentation.detail.components.details

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val FactFieldCornerRadius = 6.dp
private val StatusDotSize = 6.dp
private val HoverBackgroundAlpha = 0.08f

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

// =============================================================================
// Previews
// =============================================================================

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
