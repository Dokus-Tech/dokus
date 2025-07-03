package ai.thepredict.app.onboarding.authentication.register

import ai.thepredict.app.core.constrains.isLargeScreen
import ai.thepredict.app.core.di
import ai.thepredict.ui.PPrimaryButton
import ai.thepredict.ui.brandsugar.BackgroundAnimationViewModel
import ai.thepredict.ui.text.SectionTitle
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.kodein.di.instance

internal class RegisterConfirmationScreen : Screen {
    @Composable
    override fun Content() {
        val backgroundAnimationViewModel by di.instance<BackgroundAnimationViewModel>()

        val navigator = LocalNavigator.currentOrThrow

        Scaffold { contentPadding ->
            Box(Modifier.padding(contentPadding)) {
                if (isLargeScreen) {
                    Column {
                        RegistrationConfirmationForm {
                            // TODO: Complete the code here
                        }
                    }
                } else {
                    RegistrationConfirmationForm {
                        // TODO: Complete the code here
                    }
                }
            }
        }
    }
}

@Composable
internal fun RegistrationConfirmationForm(
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
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