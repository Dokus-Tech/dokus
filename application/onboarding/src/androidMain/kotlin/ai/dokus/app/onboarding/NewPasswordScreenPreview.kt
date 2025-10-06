package ai.dokus.app.app.onboarding

import ai.dokus.app.app.onboarding.authentication.restore.NewPasswordScreenMobileContent
import ai.dokus.foundation.ui.Themed
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
