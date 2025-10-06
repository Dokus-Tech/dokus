package ai.dokus.app.app.onboarding

import ai.dokus.app.app.onboarding.server.ServerConnectionContent
import ai.dokus.foundation.ui.Themed
import ai.dokus.foundation.ui.theme.createColorScheme
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