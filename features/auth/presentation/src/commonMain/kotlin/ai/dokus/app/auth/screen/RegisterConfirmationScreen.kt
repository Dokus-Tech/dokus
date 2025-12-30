package ai.dokus.app.auth.screen

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_continue
import ai.dokus.app.resources.generated.auth_register_success_title
import ai.dokus.app.resources.generated.registration_success_message
import ai.dokus.foundation.design.components.PPrimaryButton
import ai.dokus.foundation.design.components.text.AppNameText
import ai.dokus.foundation.design.components.text.CopyRightText
import ai.dokus.foundation.design.components.text.SectionTitle
import ai.dokus.foundation.design.local.LocalScreenSize
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
import org.jetbrains.compose.resources.stringResource

@Composable
fun RegisterConfirmationScreen() {
    Scaffold { contentPadding ->
        Box(Modifier.padding(contentPadding)) {
            if (LocalScreenSize.current.isLarge) {
                RegisterConfirmationFormDesktop {
//                    navigator.navigateToHome()
                }
            } else {
                RegistrationConfirmationForm {
//                    navigator.navigateToHome()
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
        SectionTitle(
            text = stringResource(Res.string.auth_register_success_title),
            horizontalArrangement = Arrangement.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(Res.string.registration_success_message),
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground,
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(modifier = Modifier.height(24.dp))

        PPrimaryButton(
            text = stringResource(Res.string.action_continue),
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClick
        )

        Spacer(modifier = Modifier.height(24.dp))
    }
}
