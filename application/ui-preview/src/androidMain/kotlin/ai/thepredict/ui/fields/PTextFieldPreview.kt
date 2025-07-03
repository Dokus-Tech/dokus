package ai.thepredict.ui.fields

import ai.thepredict.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun PTextFieldPreview() {
    Themed {
        PTextFieldEmail(
            fieldName = "Email address",
            value = "",
            onValueChange = { /* Handle value change */ }
        )
    }
}