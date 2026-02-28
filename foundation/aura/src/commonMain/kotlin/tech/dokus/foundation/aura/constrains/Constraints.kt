package tech.dokus.foundation.aura.constrains

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.style.DefaultDokusRadii
import tech.dokus.foundation.aura.style.DefaultDokusSizing
import tech.dokus.foundation.aura.style.DefaultDokusSpacing

object Constraints {
    val largeScreenWidth = DefaultDokusSizing.largeScreenWidth
    val largeScreenDefaultWidth = DefaultDokusSizing.largeScreenDefaultWidth
    val largeScreenHeight = DefaultDokusSizing.largeScreenHeight
    val centeredContentMaxWidth = DefaultDokusSizing.centeredContentMaxWidth

    object Breakpoint {
        const val SMALL = 600
        const val LARGE = 1200
    }

    object Spacing {
        val xxSmall = DefaultDokusSpacing.xxSmall
        val xSmall = DefaultDokusSpacing.xSmall
        val small = DefaultDokusSpacing.small
        val medium = DefaultDokusSpacing.medium
        val large = DefaultDokusSpacing.large
        val xLarge = DefaultDokusSpacing.xLarge
        val xxLarge = DefaultDokusSpacing.xxLarge
        val xxxLarge = DefaultDokusSpacing.xxxLarge
    }

    object CornerRadius {
        val badge = DefaultDokusRadii.badge
        val input = DefaultDokusRadii.input
        val button = DefaultDokusRadii.button
        val card = DefaultDokusRadii.card
        val window = DefaultDokusRadii.window
    }

    object Shell {
        val padding = DefaultDokusSizing.shellPadding
        val gap = DefaultDokusSizing.shellGap
        val sidebarWidth = DefaultDokusSizing.shellSidebarWidth
        val contentPaddingV = DefaultDokusSizing.shellContentPaddingV
        val contentPaddingH = DefaultDokusSizing.shellContentPaddingH
    }

    object DocumentDetail {
        val queueWidth = DefaultDokusSizing.documentQueueWidth
        val inspectorWidth = DefaultDokusSizing.documentInspectorWidth
        val previewMaxWidth = DefaultDokusSizing.documentPreviewMaxWidth
    }

    object StatusDot {
        val size = DefaultDokusSizing.statusDotSize
        const val pulseDuration = 2000
    }

    object Table {
        val rowMinHeight = DefaultDokusSizing.tableRowMinHeight
        val headerPaddingH = DefaultDokusSizing.tableHeaderPaddingH
    }

    object IconSize {
        val xSmall = DefaultDokusSizing.iconXSmall
        val small = DefaultDokusSizing.iconSmall
        val smallMedium = DefaultDokusSizing.iconSmallMedium
        val medium = DefaultDokusSizing.iconMedium
        val large = DefaultDokusSizing.iconLarge
        val xLarge = DefaultDokusSizing.iconXLarge
        val xxLarge = DefaultDokusSizing.iconXXLarge
        val buttonLoading = DefaultDokusSizing.buttonLoadingIcon
    }

    object Height {
        val button = DefaultDokusSizing.buttonHeight
        val input = DefaultDokusSizing.inputHeight
        val navigationBar = DefaultDokusSizing.navigationBarHeight
        val shimmerLine = DefaultDokusSizing.shimmerLineHeight
    }

    object Elevation {
        val none = DefaultDokusSizing.elevationNone
        val modal = DefaultDokusSizing.elevationModal
    }

    object AvatarSize {
        val extraSmall = DefaultDokusSizing.avatarExtraSmall
        val small = DefaultDokusSizing.avatarSmall
        val medium = DefaultDokusSizing.avatarMedium
        val tile = DefaultDokusSizing.avatarTile
        val large = DefaultDokusSizing.avatarLarge
        val extraLarge = DefaultDokusSizing.avatarExtraLarge
    }

    object Stroke {
        val thin = DefaultDokusSizing.strokeThin
        val dashWidth = DefaultDokusSizing.strokeDashWidth
        val cropGuide = DefaultDokusSizing.strokeCropGuide
    }

    object DialogSize {
        val maxWidth = DefaultDokusSizing.dialogMaxWidth
        val cropAreaMax = DefaultDokusSizing.dialogCropAreaMax
    }

    object CropGuide {
        val cornerLength = DefaultDokusSizing.cropGuideCornerLength
    }

    object Navigation {
        val fabSize = DefaultDokusSizing.navigationFabSize
        val indicatorWidth = DefaultDokusSizing.navigationIndicatorWidth
        val indicatorHeight = DefaultDokusSizing.navigationIndicatorHeight
    }

    object SearchField {
        val minWidth = DefaultDokusSizing.searchFieldMinWidth
        val maxWidth = DefaultDokusSizing.searchFieldMaxWidth
    }

    object OperatorForm {
        val maxWidth = DefaultDokusSizing.operatorFormMaxWidth
    }
}

@Stable
fun Modifier.limitWidth(): Modifier = widthIn(max = Constraints.largeScreenWidth)

@Stable
fun Modifier.limitWidthCenteredContent(): Modifier =
    widthIn(max = Constraints.centeredContentMaxWidth)

@Stable
fun Modifier.limitWidthOperatorForm(): Modifier =
    widthIn(max = Constraints.OperatorForm.maxWidth)

@Stable
fun Modifier.withVerticalPadding(): Modifier =
    then(Modifier.padding(vertical = Constraints.Spacing.xxLarge))

@Composable
fun Modifier.withContentPaddingForScrollable(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(
            Modifier.padding(top = Constraints.Shell.contentPaddingV)
                .then(Modifier.padding(horizontal = Constraints.Shell.contentPaddingH))
        )
    }
    return then(Modifier.padding(horizontal = Constraints.Spacing.large))
}

@Composable
fun Modifier.withContentPadding(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(
            Modifier.padding(
                vertical = Constraints.Shell.contentPaddingV,
                horizontal = Constraints.Shell.contentPaddingH
            )
        )
    }
    return then(Modifier.padding(horizontal = Constraints.Spacing.large))
}

@Composable
fun Modifier.withHorizontalPadding(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(Modifier.padding(horizontal = Constraints.Spacing.xxLarge))
    }
    return then(Modifier.padding(horizontal = Constraints.Spacing.large))
}

@Composable
fun Modifier.withExtraTopPaddingMobile(): Modifier {
    if (LocalScreenSize.isLarge) return this
    return then(Modifier.padding(top = Constraints.Spacing.large))
}

@Stable
fun Modifier.withContentPadding(
    innerPadding: PaddingValues,
    layoutDirection: LayoutDirection,
    withBottom: Boolean = false
): Modifier = padding(
    bottom = innerPadding.calculateBottomPadding().takeIf { withBottom } ?: 0.dp,
    start = innerPadding.calculateStartPadding(layoutDirection),
    end = innerPadding.calculateEndPadding(layoutDirection),
    top = innerPadding.calculateTopPadding(),
)

@Composable
fun ContentPaddingVertical() {
    Spacer(modifier = Modifier.padding(vertical = Constraints.Spacing.large))
}
