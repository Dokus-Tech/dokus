package ai.thepredict.ui

import ai.thepredict.domain.exceptions.PredictException
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Composable
@Preview
fun PErrorTextPreview() {
    PreviewWrapper {
        PErrorText(PredictException.NotAuthenticated)
    }
}