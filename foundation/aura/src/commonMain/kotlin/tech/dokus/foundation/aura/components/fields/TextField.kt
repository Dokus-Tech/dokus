package tech.dokus.foundation.aura.components.fields

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_clear
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.components.PErrorText
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.style.brandGold

@Suppress("LongParameterList") // UI styling function with necessary visual parameters
private fun Modifier.dokusFocusGlow(
    enabled: Boolean,
    isFocused: Boolean,
    hasError: Boolean,
    shape: Shape,
    focusColor: Color,
    errorColor: Color,
    glowStroke: androidx.compose.ui.unit.Dp = 10.dp,
    glowAlpha: Float = 0.14f,
): Modifier = composed {
    if (!enabled || (!isFocused && !hasError)) return@composed this

    val layoutDirection = LocalLayoutDirection.current
    val glowColor = (if (hasError) errorColor else focusColor).copy(alpha = glowAlpha)

    this.drawBehind {
        // Fake a soft glow by drawing a thicker translucent stroke *behind* the field.
        // This avoids the "shadow inside" look and stays consistent across KMP targets.
        val outline = shape.createOutline(size, layoutDirection, this)
        drawOutline(outline = outline, color = glowColor, style = Stroke(width = glowStroke.toPx()))
    }
}

@Composable
fun PTextField(
    fieldName: String,
    value: String,
    icon: ImageVector?,
    singleLine: Boolean,
    minLines: Int,
    onAction: () -> Unit,
    keyboardOptions: KeyboardOptions,
    error: DokusException?,
    visualTransformation: VisualTransformation,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
    showClearButton: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val labelTextStyle = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val iconSizeDp = with(density) { labelTextStyle.fontSize.toDp() }

    val gold = MaterialTheme.colorScheme.brandGold

    // Dokus Field tokens (glassy + calm)
    val fieldShape = MaterialTheme.shapes.medium
    val idleBorder = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
    val focusBorder = gold.copy(alpha = 0.85f)
    val errorBorder = MaterialTheme.colorScheme.error

    val containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
    val labelColor = if (isFocused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon?.let {
                PIcon(
                    it,
                    "",
                    modifier = Modifier.padding(end = Constrains.Spacing.xSmall).size(iconSizeDp)
                )
            }
            Text(
                text = fieldName,
                style = labelTextStyle,
                fontWeight = FontWeight.Medium,
                color = labelColor
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.hasFocus
                }
                .dokusFocusGlow(
                    enabled = enabled,
                    isFocused = isFocused,
                    hasError = error != null,
                    shape = fieldShape,
                    focusColor = focusBorder,
                    errorColor = errorBorder,
                )
                // Dokus inputs: calm surface + precise border (no elevation shadow)
                .clip(fieldShape)
                .background(containerColor)
                .border(
                    width = when {
                        error != null -> 2.dp
                        isFocused -> 2.dp
                        else -> Constrains.Stroke.thin
                    },
                    color = when {
                        error != null -> errorBorder
                        isFocused -> focusBorder
                        else -> idleBorder
                    },
                    shape = fieldShape
                )
                .padding(
                    horizontal = Constrains.Spacing.large,
                    vertical = Constrains.Spacing.medium
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = minLines,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.50f)
                        value.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                ),
                singleLine = singleLine,
                cursorBrush = SolidColor(gold),
                keyboardActions = KeyboardActions(
                    onNext = { onAction() },
                    onDone = { onAction() }
                ),
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
                enabled = enabled,
                modifier = Modifier.weight(1f),
                decorationBox = { innerTextField ->
                    innerTextField()
                }
            )

            // Clear button - shown when showClearButton is true and value is not empty
            if (showClearButton && value.isNotEmpty()) {
                IconButton(
                    onClick = { onClear?.invoke() ?: onValueChange("") },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = stringResource(Res.string.action_clear),
                        modifier = Modifier.size(Constrains.IconSize.xSmall),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                    )
                }
            }
        }

        if (error != null) {
            PErrorText(error)
        }
    }
}
