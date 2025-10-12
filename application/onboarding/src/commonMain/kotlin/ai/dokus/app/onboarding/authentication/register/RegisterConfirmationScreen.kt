package ai.dokus.app.onboarding.authentication.register

import ai.dokus.foundation.ui.constrains.isLargeScreen
import ai.dokus.foundation.navigation.AppNavigator
import ai.dokus.foundation.ui.PPrimaryButton
import ai.dokus.foundation.ui.text.AppNameText
import ai.dokus.foundation.ui.text.CopyRightText
import ai.dokus.foundation.ui.text.SectionTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun RegisterConfirmationScreen(navigator: AppNavigator) {
    Scaffold { contentPadding ->
        Box(Modifier.padding(contentPadding)) {
            if (isLargeScreen) {
                RegisterConfirmationFormDesktop {
                    navigator.navigateToHome()
                }
            } else {
                RegistrationConfirmationForm {
                    navigator.navigateToHome()
                }
            }
        }
    }
}

@Composable
internal fun RegisterConfirmationFormDesktop(
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            modifier = modifier.widthIn(max = 480.dp).fillMaxHeight().align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            AppNameText()

            RegistrationConfirmationForm(
                modifier = Modifier.fillMaxWidth(),
                onContinueClick = onContinueClick
            )

            CopyRightText()
        }
    }
}

@Composable
internal fun RegistrationConfirmationForm(
    modifier: Modifier = Modifier.fillMaxSize(),
    onContinueClick: () -> Unit,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SectionTitle("Great news", horizontalArrangement = Arrangement.Center)

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "You have successfully registered, and now you can continue to explore all the exciting features we have to offer.",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = "Continue",
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}