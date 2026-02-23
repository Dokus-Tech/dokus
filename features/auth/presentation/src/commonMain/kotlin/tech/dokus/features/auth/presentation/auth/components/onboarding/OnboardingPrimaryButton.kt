package tech.dokus.features.auth.presentation.auth.components.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun OnboardingPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    onClick: () -> Unit,
) {
    val interactive = enabled && !isLoading
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val glowColor = primary.copy(alpha = if (interactive) 0.32f else 0.16f)

    Button(
        onClick = onClick,
        modifier = modifier
            .shadow(
                elevation = if (interactive) 10.dp else 4.dp,
                shape = MaterialTheme.shapes.small,
                clip = false,
                ambientColor = glowColor,
                spotColor = glowColor,
            )
            .height(Constraints.Height.button),
        shape = MaterialTheme.shapes.small,
        enabled = interactive,
        colors = ButtonDefaults.buttonColors(
            containerColor = primary,
            contentColor = onPrimary,
            disabledContainerColor = primary.copy(alpha = 0.68f),
            disabledContentColor = onPrimary.copy(alpha = 0.74f),
        ),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(Constraints.IconSize.buttonLoading),
                color = onPrimary,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingPrimaryButtonEnabledPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        Column {
            OnboardingPrimaryButton(
                text = "Sign In",
                enabled = true,
                onClick = {},
            )
        }
    }
}

@Preview
@Composable
private fun OnboardingPrimaryButtonDisabledPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        Column {
            OnboardingPrimaryButton(
                text = "Sign In",
                enabled = false,
                onClick = {},
            )
        }
    }
}
