package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.runtime.Composable
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandPanel
import tech.dokus.features.auth.presentation.auth.components.onboarding.OnboardingBrandVariant
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@Composable
internal fun SloganScreen(
    variant: OnboardingBrandVariant = OnboardingBrandVariant.Alt,
) {
    OnboardingBrandPanel(variant = variant)
}

@Preview
@Composable
private fun SloganPreview(
    @PreviewParameter(
        PreviewParametersProvider::class,
    ) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        SloganScreen()
    }
}
