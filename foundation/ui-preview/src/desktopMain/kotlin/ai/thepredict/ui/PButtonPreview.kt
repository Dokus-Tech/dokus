package ai.thepredict.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable

@Composable
@Preview
fun PButtonPreview() {
    PreviewWrapper {
        PButton("I'm a button") {}
    }
}