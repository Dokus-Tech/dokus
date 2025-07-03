package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.login.LoginForm
import ai.thepredict.ui.Themed
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.brandsugar.SloganWithBackgroundWithLeftContent
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
                email = "",
                onEmailChange = { /*TODO*/ },
                password = "",
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
