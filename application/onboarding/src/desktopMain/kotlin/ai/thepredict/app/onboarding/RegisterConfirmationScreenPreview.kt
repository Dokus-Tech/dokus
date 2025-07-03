package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.register.RegisterConfirmationFormDesktop
import ai.thepredict.ui.Themed
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun RegisterConfirmationScreenPreview() {
    Themed {
        RegisterConfirmationFormDesktop {

        }
    }
}
