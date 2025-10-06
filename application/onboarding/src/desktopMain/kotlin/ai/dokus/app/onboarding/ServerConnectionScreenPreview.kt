package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.server.ServerConnectionContent
import ai.dokus.foundation.ui.Themed
import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import ai.dokus.foundation.ui.brandsugar.SloganWithBackgroundWithLeftContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun ServerConnectionScreenPreview() {
    Themed {
        SloganWithBackgroundWithLeftContent(remember { BackgroundAnimationViewModel() }) {
            ServerConnectionContent(
                onBackPress = {},
            )
        }
    }
}