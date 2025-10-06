package ai.dokus.app.app.onboarding

import ai.dokus.app.app.onboarding.authentication.login.LoginScreenMobileContent
import ai.dokus.foundation.ui.Themed
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun LoginScreenPreview() {
    Themed {
        LoginScreenMobileContent(
            focusManager = LocalFocusManager.current,
            email = "",
            onEmailChange = { /*TODO*/ },
            password = "",
            onPasswordChange = { /*TODO*/ },
            fieldsError = null,
            onLoginClick = { /*TODO*/ },
            onRegisterClick = { /*TODO*/ },
            onForgetPasswordClick = { /*TODO*/ },
            onConnectToServerClick = { /*TODO*/ },
        )
    }
}
