package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.login.LoginScreen
import ai.thepredict.app.onboarding.authentication.login.LoginScreenMobileContent
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun LoginScreenPreview() {
    val colorScheme = createColorScheme(false)
    val screen = LoginScreen()

    MaterialTheme(colorScheme = colorScheme) {
        LoginScreenMobileContent(
            focusManager = LocalFocusManager.current,
            email = "",
            onEmailChange = { /*TODO*/ },
            password = "",
            onPasswordChange = { /*TODO*/ },
            fieldsError = null,
            onLoginClick = { /*TODO*/ },
            onRegisterClick = { /*TODO*/ },
            onConnectToServerClick = { /*TODO*/ },
        )
    }
}
