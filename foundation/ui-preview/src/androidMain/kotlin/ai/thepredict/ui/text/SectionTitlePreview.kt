package ai.thepredict.ui.text

import ai.thepredict.ui.PreviewWrapper
import ai.thepredict.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun SectionTitleWithBackButtonPreview() {
    Themed {
        SectionTitle(
            text = "Forgot password"
        ) {

        }
    }
}

@Preview
@Composable
fun SectionTitleNoBackButtonPreview() {
    Themed {
        SectionTitle(
            text = "Forgot password"
        )
    }
}