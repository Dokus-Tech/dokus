package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.authentication.restore.NewPasswordScreenMobileContent
import ai.dokus.foundation.domain.Password
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
            password = Password(""),
            onPasswordChange = { /*TODO*/ },
            passwordConfirmation = Password(""),
            onPasswordConfirmationChange = { /*TODO*/ },
            onContinueClick = { /*TODO*/ },
            fieldsError = null,
        )
    }
}
