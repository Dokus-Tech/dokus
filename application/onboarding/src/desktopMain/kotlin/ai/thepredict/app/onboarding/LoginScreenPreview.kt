package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.login.LoginScreenDesktopContent
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun LoginScreenPreview() {
    val colorScheme = createColorScheme(false)

    MaterialTheme(colorScheme = colorScheme) {
        LoginScreenDesktopContent(
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
