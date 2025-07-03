package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.server.ServerConnectionContent
import ai.thepredict.ui.Themed
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
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