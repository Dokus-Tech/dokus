package ai.thepredict.ui.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun CopyRightText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "©2025 The Predict",
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.titleSmall
    )
}