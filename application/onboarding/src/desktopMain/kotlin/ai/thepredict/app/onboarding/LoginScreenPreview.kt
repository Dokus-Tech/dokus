package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.login.LoginForm
import ai.thepredict.app.onboarding.authentication.login.LoginScreen
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun LoginScreenPreview() {
    val colorScheme = createColorScheme(false)
    val screen = LoginScreen()

    MaterialTheme(colorScheme = colorScheme) {
        LoginForm(
            email = "",
            onEmailChange = { /*TODO*/ },
            password = "",
            onPasswordChange = { /*TODO*/ },
            fieldsError = null,
            onLoginClick = { /*TODO*/ },
            onRegisterClick = { /*TODO*/ },
            onConnectToServerClick = { /*TODO*/ }
        )
    }
}
