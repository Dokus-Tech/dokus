package ai.thepredict.ui.fields

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PErrorText
import ai.thepredict.ui.PIcon
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun PTextField(
    fieldName: String,
    value: String,
    icon: ImageVector?,
    singleLine: Boolean,
    minLines: Int,
    onAction: () -> Unit,
    keyboardOptions: KeyboardOptions,
    error: PredictException?,
    visualTransformation: VisualTransformation,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    val labelTextStyle = MaterialTheme.typography.bodyMedium
    val density = LocalDensity.current
    val iconSizeDp = with(density) { labelTextStyle.fontSize.toDp() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                    modifier = Modifier.padding(end = 4.dp).size(iconSizeDp)
                )
            }
            Text(
                text = fieldName,
                style = labelTextStyle,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF1F1F1F)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (error != null) Color.Red else Color(0xFFE1E1E1),
                    shape = RoundedCornerShape(8.dp)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                minLines = minLines,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = if (value.isEmpty()) Color(0xFF999999) else Color(0xFF1F1F1F) // TODO: Use theme
                ),
                singleLine = singleLine,
                keyboardActions = KeyboardActions(
                    onNext = { onAction() },
                    onDone = { onAction() }
                ),
                keyboardOptions = keyboardOptions,
                visualTransformation = visualTransformation,
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