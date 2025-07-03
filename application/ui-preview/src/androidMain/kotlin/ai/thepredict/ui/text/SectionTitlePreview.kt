package ai.thepredict.ui.text

import ai.thepredict.ui.PreviewWrapper
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun SectionTitleWithBackButtonPreview() {
    PreviewWrapper {
        SectionTitle(
            text = "Forgot password"
        ) {

        }
    }
}

@Preview
@Composable
fun SectionTitleNoBackButtonPreview() {
    PreviewWrapper {
        SectionTitle(
            text = "Forgot password"
        )
    }
}