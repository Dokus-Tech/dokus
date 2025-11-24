package ai.dokus.foundation.design.components.common

import ai.dokus.foundation.design.components.PIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search

/**
 * Compact search field used in top bars and narrow spaces.
 */
@Composable
fun PSearchFieldCompact(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val containerShape = remember { RoundedCornerShape(8.dp) }
    val textStyle: TextStyle = LocalTextStyle.current.copy(
        fontSize = 14.sp,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.onSurface
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, containerShape)
            .background(MaterialTheme.colorScheme.surface, containerShape)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PIcon(icon = FeatherIcons.Search, description = "Search", modifier = Modifier)

        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                // Placeholder uses onSurfaceVariant to be less prominent
                androidx.compose.material3.Text(
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
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner -> inner() }
            )
        }
    }
}
