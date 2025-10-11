package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.authentication.login.LoginScreenMobileContent
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
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
            email = Email(""),
            onEmailChange = { /*TODO*/ },
            password = Password(""),
            onPasswordChange = { /*TODO*/ },
            fieldsError = null,
            onLoginClick = { /*TODO*/ },
            onRegisterClick = { /*TODO*/ },
            onForgetPasswordClick = { /*TODO*/ },
            onConnectToServerClick = { /*TODO*/ },
        )
    }
}
