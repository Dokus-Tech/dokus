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

object Constrains {
    val largeScreenWidth = 980.dp
    val largeScreenDefaultWidth = 1280.dp
    val largeScreenHeight = 840.dp
    val centeredContentMaxWidth = 360.dp

    /**
     * Breakpoint values for responsive layouts.
     * Used to determine screen size categories.
     */
    object Breakpoint {
        const val SMALL = 600 // Mobile breakpoint
        const val LARGE = 1200 // Desktop breakpoint
    }

    /**
     * Spacing values for margins, padding, and gaps.
     * Based on 4dp baseline rhythm for consistent visual harmony.
     */
    object Spacing {
        val xxSmall = 2.dp // Minimal spacing
        val xSmall = 4.dp // Very tight spacing
        val small = 8.dp // Tight spacing
        val medium = 12.dp // Default spacing
        val large = 16.dp // Comfortable spacing
        val xLarge = 24.dp // Generous spacing
        val xxLarge = 32.dp // Section spacing
        val xxxLarge = 48.dp // Major section spacing
    }

    /**
     * Corner radius values (Design System v1).
     * Locked at 2dp/4dp/6dp - large radii are not allowed.
     * - xs (2dp): rare, tiny elements
     * - sm (4dp): panels, surfaces (default)
     * - md (6dp): inputs, buttons, modals
     */
    object CornerRadius {
        val xs = 2.dp   // Rare, tiny elements
        val sm = 4.dp   // Panels, surfaces (default)
        val md = 6.dp   // Inputs, buttons, modals
    }

    /**
     * Icon size values for consistent iconography.
     * Based on Material Design 3 icon sizing guidelines.
     */
    object IconSize {
        val xSmall = 16.dp // Inline icons
        val small = 18.dp // List item icons
        val smallMedium = 20.dp // Between small and medium
        val medium = 24.dp // Standard icons
        val large = 32.dp // Prominent icons
        val xLarge = 48.dp // Featured icons
        val xxLarge = 64.dp // Hero icons
        val buttonLoading = 20.dp // Loading indicator inside buttons
    }

    /**
     * Standard component height values.
     * Ensures consistent vertical sizing across interactive elements.
     */
    object Height {
        val button = 42.dp // Standard button height
        val input = 56.dp // Input field height
        val navigationBar = 60.dp // Bottom navigation bar height
        val shimmerLine = 14.dp // Default shimmer text line height
    }

    /**
     * Elevation values (Design System v1).
     * No shadows by default - use borders for separation.
     * Modal elevation only for dialogs/sheets.
     */
    object Elevation {
        val none = 0.dp   // Default everywhere
        val modal = 1.dp  // Modals only (very subtle)
    }

    /**
     * Avatar size values for profile images and icons.
     * Matches the AvatarSize enum values in CompanyAvatarImage.kt.
     */
    object AvatarSize {
        val extraSmall = 24.dp // Compact avatars (lists, inline)
        val small = 32.dp // Small avatars
        val medium = 64.dp // Standard avatars
        val tile = 72.dp // Tile avatars (company tiles)
        val large = 128.dp // Profile avatars
        val extraLarge = 256.dp // Featured avatars
    }

    /**
     * Stroke and border width values.
     * For lines, borders, and divider components.
     */
    object Stroke {
        val thin = 1.dp // Standard divider/border thickness
        val dashWidth = 6.dp // Dashed divider dash width
        val cropGuide = 3.dp // Crop overlay guide stroke width
    }

    /**
     * Dialog-specific size constraints.
     * Used for modal dialogs and overlays.
     */
    object DialogSize {
        val maxWidth = 400.dp // Maximum width for standard dialogs
        val cropAreaMax = 320.dp // Maximum size for image crop areas
    }

    /**
     * Crop overlay guide dimensions.
     * Used for image cropper corner guides.
     */
    object CropGuide {
        val cornerLength = 40.dp // Length of corner guide lines
    }

    /**
     * Navigation component dimensions.
     * Used for NavigationBar and NavigationRail components.
     */
    object Navigation {
        val fabSize = 46.dp // FAB button size in nav bar
        val indicatorWidth = 24.dp // Selected item indicator width
        val indicatorHeight = 2.dp // Selected item indicator height
    }

    /**
     * Search field dimensions.
     * Used for compact search fields in top bars.
     */
    object SearchField {
        val minWidth = 200.dp // Minimum width for search field
        val maxWidth = 360.dp // Maximum width for search field
    }
}

@Stable
fun Modifier.limitWidth(): Modifier = widthIn(max = Constrains.largeScreenWidth)

@Stable
fun Modifier.limitWidthCenteredContent(): Modifier =
    widthIn(max = Constrains.centeredContentMaxWidth)

@Stable
fun Modifier.withVerticalPadding(): Modifier =
    then(Modifier.padding(vertical = Constrains.Spacing.xxLarge))

@Composable
fun Modifier.withContentPaddingForScrollable(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(
            Modifier.padding(
                top = Constrains.Spacing.large
            ).then(Modifier.padding(horizontal = Constrains.Spacing.xxLarge))
        )
    }
    return then(Modifier.padding(horizontal = Constrains.Spacing.large))
}

@Composable
fun Modifier.withContentPadding(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(
            Modifier.padding(
                vertical = Constrains.Spacing.large,
                horizontal = Constrains.Spacing.xxLarge
            )
        )
    }
    return then(Modifier.padding(horizontal = Constrains.Spacing.large))
}

@Composable
fun Modifier.withHorizontalPadding(): Modifier {
    if (LocalScreenSize.isLarge) {
        return then(Modifier.padding(horizontal = Constrains.Spacing.xxLarge))
    }
    return then(Modifier.padding(horizontal = Constrains.Spacing.large))
}

@Composable
fun Modifier.withExtraTopPaddingMobile(): Modifier {
    if (LocalScreenSize.isLarge) return this
    return then(Modifier.padding(top = Constrains.Spacing.large))
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
    Spacer(modifier = Modifier.padding(vertical = Constrains.Spacing.large))
}
