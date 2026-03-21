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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val FactFieldCornerRadius = 6.dp
private val HoverBackgroundAlpha = 0.08f

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
