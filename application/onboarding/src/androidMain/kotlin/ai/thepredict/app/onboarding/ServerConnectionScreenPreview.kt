package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.server.ServerConnectionContent
import ai.thepredict.ui.Themed
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ServerConnectionScreenPreview() {
    Themed {
        ServerConnectionContent(
            onBackPress = { /* Handle back press */ }
        )
    }
}