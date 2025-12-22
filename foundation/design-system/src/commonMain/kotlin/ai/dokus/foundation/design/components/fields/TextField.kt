package ai.dokus.foundation.design.components.fields

import ai.dokus.foundation.design.components.PErrorText
import ai.dokus.foundation.design.components.PIcon
import ai.dokus.foundation.design.constrains.Constrains
import ai.dokus.foundation.domain.exceptions.DokusException
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

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
    onValueChange: (String) -> Unit,
) {
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = Constrains.Stroke.thin,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                )
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.small
                )
                .padding(horizontal = Constrains.Spacing.large, vertical = Constrains.Spacing.medium)
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
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    innerTextField()
                }
            )
        }

        if (error != null) {
            PErrorText(error)
        }
    }
}