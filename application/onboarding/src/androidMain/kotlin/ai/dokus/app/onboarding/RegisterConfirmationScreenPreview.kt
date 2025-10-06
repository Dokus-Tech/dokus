package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.authentication.register.RegistrationConfirmationForm
import ai.dokus.foundation.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun RegisterConfirmationScreenPreview() {
    Themed {
        RegistrationConfirmationForm {

        }
    }
}
