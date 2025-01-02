package ai.thepredict.ui

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.extensions.localized
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

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