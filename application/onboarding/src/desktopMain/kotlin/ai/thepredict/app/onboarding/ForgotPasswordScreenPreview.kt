package ai.thepredict.app.onboarding

import ai.thepredict.app.onboarding.authentication.restore.ForgotPasswordForm
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
fun ForgotPasswordScreenPreview() {
    val colorScheme = createColorScheme(false)

    MaterialTheme(colorScheme = colorScheme) {
        SloganWithBackgroundWithLeftContent(remember { BackgroundAnimationViewModel() }) {
            ForgotPasswordForm(
                focusManager = LocalFocusManager.current,
                email = "",
                onEmailChange = { /*TODO*/ },
                fieldsError = null,
                onSubmit = { /*TODO*/ },
                onBackPress = { /*TODO*/ },
                modifier = Modifier
            )
        }
    }
}
