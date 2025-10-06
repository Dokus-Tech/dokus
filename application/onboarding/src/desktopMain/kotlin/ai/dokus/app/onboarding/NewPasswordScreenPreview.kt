package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.authentication.restore.NewPasswordForm
import ai.dokus.foundation.ui.Themed
import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import ai.dokus.foundation.ui.brandsugar.SloganWithBackgroundWithLeftContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun NewPasswordScreenPreview() {
    Themed {
        SloganWithBackgroundWithLeftContent(remember { BackgroundAnimationViewModel() }) {
            NewPasswordForm(
                focusManager = LocalFocusManager.current,
                password = "",
                onPasswordChange = { /*TODO*/ },
                passwordConfirmation = "",
                onPasswordConfirmationChange = { /*TODO*/ },
                onContinueClick = { /*TODO*/ },
                fieldsError = null,
            )
        }
    }
}
