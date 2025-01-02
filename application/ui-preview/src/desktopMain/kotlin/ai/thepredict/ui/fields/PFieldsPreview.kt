package ai.thepredict.ui.fields

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PreviewWrapper
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Composable
@Preview
fun PTextFieldsPasswordPreview() {
    PreviewWrapper {
        PTextFieldPassword(
            fieldName = "Password",
            value = ""
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldsPasswordErrorPreview() {
    PreviewWrapper {
        PTextFieldPassword(
            fieldName = "Password",
            value = "",
            error = PredictException.WeakPassword
        ) {}
    }
}