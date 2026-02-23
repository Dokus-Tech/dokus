package tech.dokus.features.auth.presentation.auth.components.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.auth_onboarding_alt_headline_line_1
import tech.dokus.aura.resources.auth_onboarding_alt_headline_line_2
import tech.dokus.aura.resources.auth_onboarding_alt_subline_1
import tech.dokus.aura.resources.auth_onboarding_alt_subline_2
import tech.dokus.aura.resources.auth_onboarding_copyright_format
import tech.dokus.aura.resources.auth_onboarding_primary_headline_line_1
import tech.dokus.aura.resources.auth_onboarding_primary_headline_line_2
import tech.dokus.aura.resources.auth_onboarding_primary_subline_1
import tech.dokus.aura.resources.auth_onboarding_primary_subline_2
import tech.dokus.aura.resources.brand_motto
import tech.dokus.aura.resources.playfair_display_bold
import tech.dokus.aura.resources.playfair_display_regular
import tech.dokus.aura.resources.playfair_display_semibold
import tech.dokus.foundation.aura.components.DokusGlassSurface
import tech.dokus.foundation.aura.components.background.OnboardingBackground
import tech.dokus.foundation.aura.components.background.OnboardingScene
import tech.dokus.foundation.aura.components.text.AppNameText
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge

private val LeftPaneMaxWidth = 460.dp
private val CenterPaneMaxWidth = 520.dp
private val BrandPaneMaxWidth = 620.dp
private val GlassCardPadding = 36.dp
private val SplitPaneVerticalPadding = 28.dp
private val SplitPaneHorizontalPadding = 28.dp
private val CenteredHorizontalPadding = 24.dp
private val BrandHeadlineSizeDesktop = 66.sp
private val BrandHeadlineSizeMobile = 44.sp
private val BrandHeadlineLineHeightDesktop = 70.sp
private val BrandHeadlineLineHeightMobile = 48.sp
private val BrandSubtitleTopPadding = 28.dp
private val BrandSubtitleLineSpacing = 10.dp

internal enum class OnboardingBrandVariant {
    Primary,
    Alt,
}

private data class BrandCopy(
    val headlineLine1: String,
    val headlineLine2: String,
    val subline1: String,
    val subline2: String,
)

@Composable
internal fun OnboardingSplitShell(
    brandVariant: OnboardingBrandVariant,
    modifier: Modifier = Modifier,
    cardContent: @Composable ColumnScope.() -> Unit,
) {
    val isLarge = LocalScreenSize.isLarge
    val scrollState = rememberScrollState()

    Box(modifier = modifier.fillMaxSize()) {
        OnboardingBackground(scene = OnboardingScene.Split)

        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                        .padding(
                            horizontal = SplitPaneHorizontalPadding,
                            vertical = SplitPaneVerticalPadding,
                        )
                        .widthIn(max = LeftPaneMaxWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    OnboardingGlassCard(
                        modifier = Modifier.fillMaxWidth(),
                        content = cardContent,
                    )

                    Spacer(modifier = Modifier.height(Constraints.Spacing.large))

                    OnboardingBottomFooter(
                        modifier = Modifier.fillMaxWidth(),
                        alignCenter = !isLarge,
                    )
                }
            }

            if (isLarge) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(horizontal = Constraints.Spacing.xxLarge),
                    contentAlignment = Alignment.Center,
                ) {
                    OnboardingBrandPanel(
                        variant = brandVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = BrandPaneMaxWidth),
                    )
                }
            }
        }
    }
}

@Composable
internal fun OnboardingCenteredShell(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier = modifier.fillMaxSize()) {
        OnboardingBackground(scene = OnboardingScene.Centered)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = CenteredHorizontalPadding, vertical = Constraints.Spacing.xxLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            AppNameText()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = CenterPaneMaxWidth),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = content,
            )

            OnboardingYearFooter()
        }
    }
}

@Composable
internal fun OnboardingGlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    DokusGlassSurface(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlassCardPadding),
            horizontalAlignment = Alignment.Start,
            content = content,
        )
    }
}

@Composable
internal fun OnboardingBrandPanel(
    variant: OnboardingBrandVariant,
    modifier: Modifier = Modifier,
    alignCenter: Boolean = true,
) {
    val copy = when (variant) {
        OnboardingBrandVariant.Primary -> BrandCopy(
            headlineLine1 = stringResource(Res.string.auth_onboarding_primary_headline_line_1),
            headlineLine2 = stringResource(Res.string.auth_onboarding_primary_headline_line_2),
            subline1 = stringResource(Res.string.auth_onboarding_primary_subline_1),
            subline2 = stringResource(Res.string.auth_onboarding_primary_subline_2),
        )

        OnboardingBrandVariant.Alt -> BrandCopy(
            headlineLine1 = stringResource(Res.string.auth_onboarding_alt_headline_line_1),
            headlineLine2 = stringResource(Res.string.auth_onboarding_alt_headline_line_2),
            subline1 = stringResource(Res.string.auth_onboarding_alt_subline_1),
            subline2 = stringResource(Res.string.auth_onboarding_alt_subline_2),
        )
    }

    val headlineFont = rememberOnboardingHeadlineFontFamily()
    val isLarge = LocalScreenSize.isLarge
    val headlineStyle = TextStyle(
        fontFamily = headlineFont,
        fontWeight = FontWeight.Bold,
        fontSize = if (isLarge) BrandHeadlineSizeDesktop else BrandHeadlineSizeMobile,
        lineHeight = if (isLarge) BrandHeadlineLineHeightDesktop else BrandHeadlineLineHeightMobile,
        letterSpacing = (-0.02).sp,
    )
    val contentAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier,
        horizontalAlignment = contentAlignment,
    ) {
        Text(
            text = copy.headlineLine1,
            style = headlineStyle,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = textAlign,
        )
        Text(
            text = copy.headlineLine2,
            style = headlineStyle,
            color = MaterialTheme.colorScheme.primary,
            textAlign = textAlign,
        )

        Spacer(modifier = Modifier.height(BrandSubtitleTopPadding))

        Column(
            horizontalAlignment = contentAlignment,
            verticalArrangement = Arrangement.spacedBy(BrandSubtitleLineSpacing),
        ) {
            Text(
                text = copy.subline1,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
            )
            Text(
                text = copy.subline2,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = textAlign,
            )
        }
    }
}

@Composable
internal fun OnboardingBottomFooter(
    modifier: Modifier = Modifier,
    alignCenter: Boolean = false,
) {
    val year = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    }
    val horizontalAlignment = if (alignCenter) Alignment.CenterHorizontally else Alignment.Start
    val textAlign = if (alignCenter) TextAlign.Center else TextAlign.Start

    Column(
        modifier = modifier,
        horizontalAlignment = horizontalAlignment,
    ) {
        Text(
            text = stringResource(Res.string.brand_motto),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
        )
        Spacer(modifier = Modifier.height(Constraints.Spacing.xSmall))
        Text(
            text = stringResource(Res.string.auth_onboarding_copyright_format, year),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = textAlign,
        )
    }
}

@Composable
internal fun OnboardingYearFooter(modifier: Modifier = Modifier) {
    val year = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).year
    }
    Text(
        text = stringResource(Res.string.auth_onboarding_copyright_format, year),
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun rememberOnboardingHeadlineFontFamily(): FontFamily {
    val regular = Font(Res.font.playfair_display_regular, FontWeight.Normal)
    val semibold = Font(Res.font.playfair_display_semibold, FontWeight.SemiBold)
    val bold = Font(Res.font.playfair_display_bold, FontWeight.Bold)

    return remember(regular, semibold, bold) {
        FontFamily(regular, semibold, bold)
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun OnboardingSplitShellPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        OnboardingSplitShell(brandVariant = OnboardingBrandVariant.Primary) {
            AppNameText()
            Spacer(modifier = Modifier.height(Constraints.Spacing.large))
            Text(
                text = "Preview content",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun OnboardingCenteredShellPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class,
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters,
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        OnboardingCenteredShell {
            Text(
                text = "Centered content",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
