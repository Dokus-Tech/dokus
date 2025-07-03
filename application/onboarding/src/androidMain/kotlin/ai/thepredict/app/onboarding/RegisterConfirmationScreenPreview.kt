package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.register.RegistrationConfirmationForm
import ai.thepredict.ui.Themed
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
