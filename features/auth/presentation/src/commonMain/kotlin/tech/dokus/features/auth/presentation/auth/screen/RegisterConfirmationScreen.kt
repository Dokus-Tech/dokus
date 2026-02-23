package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.auth_register_success_title
import tech.dokus.aura.resources.registration_success_message
import tech.dokus.features.auth.presentation.auth.components.v2.OnboardingCenteredShell
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.constrains.Constraints

@Composable
fun RegisterConfirmationScreen() {
    OnboardingCenteredShell {
        RegistrationConfirmationForm(
            modifier = Modifier.fillMaxWidth(),
            onContinueClick = {},
        )
    }
}

@Composable
internal fun RegistrationConfirmationForm(
    modifier: Modifier = Modifier,
    onContinueClick: () -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.auth_register_success_title),
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.medium))

        Text(
            text = stringResource(Res.string.registration_success_message),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(Constraints.Spacing.xLarge))

        PPrimaryButton(
            text = stringResource(Res.string.action_continue),
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueClick,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun RegistrationConfirmationFormPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        OnboardingCenteredShell {
            RegistrationConfirmationForm(
                modifier = Modifier.fillMaxWidth(),
                onContinueClick = {},
            )
        }
    }
}
