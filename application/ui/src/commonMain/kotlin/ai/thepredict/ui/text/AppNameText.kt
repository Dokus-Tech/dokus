package ai.thepredict.ui.text

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp

@Composable
fun AppNameText(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = "Predict",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.displaySmall
    )
}