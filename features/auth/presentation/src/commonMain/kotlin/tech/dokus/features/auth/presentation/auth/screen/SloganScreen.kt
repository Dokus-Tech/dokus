package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.runtime.Composable
import tech.dokus.features.auth.presentation.auth.components.v2.OnboardingBrandPanel
import tech.dokus.features.auth.presentation.auth.components.v2.OnboardingBrandVariant

@Composable
internal fun SloganScreen(
    variant: OnboardingBrandVariant = OnboardingBrandVariant.Alt,
) {
    OnboardingBrandPanel(variant = variant)
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SloganPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        SloganScreen()
    }
}
