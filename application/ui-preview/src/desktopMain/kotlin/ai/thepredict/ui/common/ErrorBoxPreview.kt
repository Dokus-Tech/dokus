package ai.thepredict.ui.common

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PreviewWrapper
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Composable
@Preview
fun ErrorBoxPreview() {
    PreviewWrapper {
        ErrorBox(PredictException.UserAlreadyExists) {}
    }
}