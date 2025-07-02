package ai.thepredict.ui.fields

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun PTextFieldPreview() {
    PTextFieldEmail(
        fieldName = "Email address",
        value = "",
        onValueChange = { /* Handle value change */ }
    )
}