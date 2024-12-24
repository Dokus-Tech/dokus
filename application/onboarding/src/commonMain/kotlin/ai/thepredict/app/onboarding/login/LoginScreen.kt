package ai.thepredict.app.onboarding.login

import ai.thepredict.ui.Title
import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen

internal class LoginScreen : Screen {

    @Composable
    override fun Content() {
        Title("LoginScreen")
    }
}