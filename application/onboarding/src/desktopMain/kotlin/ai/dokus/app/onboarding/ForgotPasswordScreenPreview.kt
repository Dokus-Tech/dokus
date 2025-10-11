package ai.dokus.app.onboarding

import ai.dokus.app.onboarding.authentication.restore.ForgotPasswordForm
import ai.dokus.foundation.domain.Email
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
fun ForgotPasswordScreenPreview() {
    Themed {
        SloganWithBackgroundWithLeftContent(remember { BackgroundAnimationViewModel() }) {
            ForgotPasswordForm(
                focusManager = LocalFocusManager.current,
                email = Email(""),
                onEmailChange = { /*TODO*/ },
                fieldsError = null,
                onSubmit = { /*TODO*/ },
                onBackPress = { /*TODO*/ },
                modifier = Modifier
            )
        }
    }
}
