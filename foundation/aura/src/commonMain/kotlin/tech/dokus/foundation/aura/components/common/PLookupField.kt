package tech.dokus.foundation.aura.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val FieldHeight = 56.dp
private val HorizontalPadding = 14.dp
private val BorderWidth = 1.dp
private val FieldCornerRadius = 12.dp
private val DashedPattern = floatArrayOf(10f, 8f)

enum class PLookupFieldOutline {
    Solid,
    Dashed
}

@Composable
fun PLookupField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    isSelected: Boolean = false,
    outline: PLookupFieldOutline = PLookupFieldOutline.Solid,
    forceFocused: Boolean = false,
    onFocusChanged: (Boolean) -> Unit = {},
    trailingContent: @Composable (() -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val focused = forceFocused || isFocused
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        isSelected || focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(FieldHeight)
            .lookupBorder(color = borderColor, outline = outline)
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.small)
            .padding(horizontal = HorizontalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
    ) {
        PIcon(
            icon = FeatherIcons.Search,
            description = stringResource(Res.string.action_search),
            modifier = Modifier.size(Constraints.IconSize.small),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(modifier = Modifier.weight(1f)) {
            if (value.isBlank()) {
                Text(
                    text = placeholder,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                singleLine = true,
                enabled = enabled,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged {
                        isFocused = it.isFocused
                        onFocusChanged(it.isFocused)
                    },
                decorationBox = { inner -> inner() }
            )
        }
        trailingContent?.invoke()
    }
}

private fun Modifier.lookupBorder(
    color: androidx.compose.ui.graphics.Color,
    outline: PLookupFieldOutline
): Modifier {
    return drawBehind {
        val strokeWidth = BorderWidth.toPx()
        drawRoundRect(
            color = color,
            cornerRadius = CornerRadius(FieldCornerRadius.toPx(), FieldCornerRadius.toPx()),
            style = Stroke(
                width = strokeWidth,
                pathEffect = if (outline == PLookupFieldOutline.Dashed) {
                    PathEffect.dashPathEffect(DashedPattern)
                } else {
                    null
                }
            )
        )
    }
}

@Preview(name = "Lookup Field Idle")
@Composable
private fun PLookupFieldIdlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PLookupField(
            value = "",
            onValueChange = {},
            placeholder = "Search by name, VAT, or email",
            outline = PLookupFieldOutline.Dashed
        )
    }
}

@Preview(name = "Lookup Field Focused")
@Composable
private fun PLookupFieldFocusedPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PLookupField(
            value = "Coolblue",
            onValueChange = {},
            placeholder = "Search by name, VAT, or email",
            forceFocused = true,
            outline = PLookupFieldOutline.Solid
        )
    }
}

@Preview(name = "Lookup Field Error")
@Composable
private fun PLookupFieldErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PLookupField(
            value = "invalid",
            onValueChange = {},
            placeholder = "Search by name, VAT, or email",
            isError = true,
            isSelected = true,
            outline = PLookupFieldOutline.Solid
        )
    }
}

@Preview(name = "Lookup Field Disabled")
@Composable
private fun PLookupFieldDisabledPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PLookupField(
            value = "Acme NV",
            onValueChange = {},
            placeholder = "Search by name, VAT, or email",
            enabled = false,
            outline = PLookupFieldOutline.Solid
        )
    }
}
