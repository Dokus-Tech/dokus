package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.login.LoginForm
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
import ai.thepredict.ui.theme.createColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import org.jetbrains.compose.ui.tooling.preview.Preview

@Preview
@Composable
fun LoginScreenPreview() {
    val colorScheme = createColorScheme(false)

    MaterialTheme(colorScheme = colorScheme) {
        SloganWithBackgroundWithLeftContent(remember { BackgroundAnimationViewModel() }) {
            LoginForm(
                focusManager = LocalFocusManager.current,
                email = "",
                onEmailChange = { /*TODO*/ },
                password = "",
                onPasswordChange = { /*TODO*/ },
                fieldsError = null,
                onLoginClick = { /*TODO*/ },
                onRegisterClick = { /*TODO*/ },
                onConnectToServerClick = { /*TODO*/ },
                modifier = Modifier
            )
        }
    }
}
