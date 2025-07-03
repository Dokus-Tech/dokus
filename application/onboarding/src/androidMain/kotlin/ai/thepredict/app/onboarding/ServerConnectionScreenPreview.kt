package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.server.ServerConnectionScreenContent
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ServerConnectionScreenPreview() {
    val colorScheme = createColorScheme(false)
    MaterialTheme(colorScheme = colorScheme) {
        ServerConnectionScreenContent()
    }
}