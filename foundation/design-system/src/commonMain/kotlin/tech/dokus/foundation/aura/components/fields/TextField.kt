package tech.dokus.foundation.aura.components.fields

import tech.dokus.foundation.aura.components.PErrorText
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.domain.exceptions.DokusException
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
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_clear
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

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
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isFocused = focusState.hasFocus
                }
                .border(
                    width = Constrains.Stroke.thin,
                    color = when {
                        error != null -> MaterialTheme.colorScheme.error
                        isFocused -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.outline
                    },
                    shape = MaterialTheme.shapes.small
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium),
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
                        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        value.isEmpty() -> MaterialTheme.colorScheme.onSurfaceVariant
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                ),
                singleLine = singleLine,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
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
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (error != null) {
            PErrorText(error)
        }
    }
}
