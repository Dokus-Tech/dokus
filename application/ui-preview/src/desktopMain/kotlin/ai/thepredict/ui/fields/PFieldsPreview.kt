package ai.thepredict.ui.fields

import ai.thepredict.domain.exceptions.PredictException
import ai.thepredict.ui.PreviewWrapper
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Composable
@Preview
fun PTextFieldEmailPreview() {
    PreviewWrapper {
        PTextFieldEmail(
            fieldName = "Email",
            value = ""
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldEmailErrorPreview() {
    PreviewWrapper {
        PTextFieldEmail(
            fieldName = "Email",
            value = "",
            error = PredictException.InvalidEmail
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldNamePreview() {
    PreviewWrapper {
        PTextFieldName(
            fieldName = "Name",
            value = ""
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldNameErrorPreview() {
    PreviewWrapper {
        PTextFieldName(
            fieldName = "Name",
            value = "",
            error = PredictException.InvalidName
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldPasswordPreview() {
    PreviewWrapper {
        PTextFieldPassword(
            fieldName = "Password",
            value = ""
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldPasswordErrorPreview() {
    PreviewWrapper {
        PTextFieldPassword(
            fieldName = "Password",
            value = "",
            error = PredictException.WeakPassword
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldFreePreview() {
    PreviewWrapper {
        PTextFieldFree(
            fieldName = "Note",
            value = ""
        ) {}
    }
}

@Composable
@Preview
fun PTextFieldFreeErrorPreview() {
    PreviewWrapper {
        PTextFieldFree(
            fieldName = "Note",
            value = "",
            error = PredictException.InternalError("Please use valid something")
        ) {}
    }
}