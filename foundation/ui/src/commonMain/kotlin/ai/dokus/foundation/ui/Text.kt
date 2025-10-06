package ai.dokus.foundation.ui

import ai.dokus.foundation.domain.exceptions.PredictException
import ai.dokus.foundation.ui.extensions.localized
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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

@Composable
fun PErrorText(text: String, modifier: Modifier = Modifier.padding(all = 16.dp)) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
fun PErrorText(exception: PredictException, modifier: Modifier = Modifier.padding(all = 16.dp)) {
    PErrorText(exception.localized, modifier)
}