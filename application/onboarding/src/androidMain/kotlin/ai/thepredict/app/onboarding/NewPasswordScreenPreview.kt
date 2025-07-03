package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.restore.NewPasswordScreenMobileContent
import ai.thepredict.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun NewPasswordScreenPreview() {
    Themed {
        NewPasswordScreenMobileContent(
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
