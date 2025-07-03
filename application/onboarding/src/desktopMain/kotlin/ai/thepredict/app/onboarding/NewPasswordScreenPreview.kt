package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.restore.NewPasswordForm
import ai.thepredict.ui.Themed
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
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
