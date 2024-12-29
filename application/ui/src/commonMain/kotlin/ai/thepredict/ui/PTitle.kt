package ai.thepredict.ui

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

object PTitleDefaults {
    val textAlign: TextAlign = TextAlign.Center
}

@Composable
fun PTitle(
    text: String,
    textAlign: TextAlign = PTitleDefaults.textAlign,
    modifier: Modifier = Modifier,
) {
    Text(text = text, modifier = modifier, textAlign = textAlign)
}