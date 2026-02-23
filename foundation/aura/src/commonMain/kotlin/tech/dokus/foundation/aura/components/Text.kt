package tech.dokus.foundation.aura.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

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
fun PErrorText(text: String, modifier: Modifier = Modifier.padding(all = Constraints.Spacing.large)) {
    Text(
        text,
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.error
    )
}

@Composable
fun PErrorText(exception: DokusException, modifier: Modifier = Modifier.padding(all = Constraints.Spacing.large)) {
    PErrorText(exception.localized, modifier)
}

@Preview
@Composable
private fun PTitlePreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PTitle(text = "Page Title")
    }
}

@Preview
@Composable
private fun PErrorTextPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        PErrorText(text = "Something went wrong")
    }
}
