package ai.dokus.app.app.onboarding

import ai.dokus.app.app.onboarding.authentication.register.RegisterConfirmationFormDesktop
import ai.dokus.foundation.ui.Themed
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
