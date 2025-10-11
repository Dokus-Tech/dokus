package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.authentication.login.LoginForm
import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.ui.Themed
import ai.dokus.foundation.ui.brandsugar.BackgroundAnimationViewModel
import ai.dokus.foundation.ui.brandsugar.SloganWithBackgroundWithLeftContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun LoginScreenPreview() {
    Themed {
        SloganWithBackgroundWithLeftContent(remember { BackgroundAnimationViewModel() }) {
            LoginForm(
                focusManager = LocalFocusManager.current,
                email = Email(""),
                onEmailChange = { /*TODO*/ },
                password = Password(""),
                onPasswordChange = { /*TODO*/ },
                fieldsError = null,
                onLoginClick = { /*TODO*/ },
                onRegisterClick = { /*TODO*/ },
                onConnectToServerClick = { /*TODO*/ },
                onForgetPasswordClick = { /*TODO*/ },
                modifier = Modifier
            )
        }
    }
}
