package ai.dokus.foundation.design.components.common

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_clear_field
import ai.dokus.app.resources.generated.action_search
import ai.dokus.foundation.design.components.PIcon
import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.stringResource

/**
 * Compact search field for top bars; independent of PTextField.
 */
@Composable
fun PSearchFieldCompact(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    fieldName: String = stringResource(Res.string.action_search),
    onClear: (() -> Unit)? = null,
) {
    val shape = MaterialTheme.shapes.small
    val textStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = modifier
            .widthIn(min = Constrains.SearchField.minWidth, max = Constrains.SearchField.maxWidth)
            .height(Constrains.Height.button)
            .border(Constrains.Stroke.thin, MaterialTheme.colorScheme.outline, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = Constrains.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        PIcon(icon = FeatherIcons.Search, description = fieldName)
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart),
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
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner -> inner() }
            )
        }

        // Clear button - shown when value is not empty and onClear is provided
        if (value.isNotEmpty() && onClear != null) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(Res.string.action_clear_field, fieldName),
                    modifier = Modifier.size(Constrains.IconSize.xSmall),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
