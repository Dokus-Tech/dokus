package ai.dokus.app.navigation.secondary

import ai.dokus.app.navigation.DokusNavHost
import ai.dokus.foundation.design.local.LocalReduceMotion
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.local.LocalSecondaryNavController
import ai.dokus.foundation.navigation.local.LocalSecondaryNavigationState
import ai.dokus.foundation.navigation.local.SecondaryPanelType
import ai.dokus.foundation.navigation.local.rememberSecondaryNavigationState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

/**
 * Container for dual-panel navigation.
 * Shows primary navigation on the left (50%) and secondary navigation on the right (50%)
 * when the secondary panel is visible and screen is large enough.
 */
@Composable
fun DualPanelNavigationContainer(
    navigationProviders: List<NavigationProvider>,
    onPrimaryNavHostReady: suspend (NavController) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val largeScreen = LocalScreenSize.isLarge
    val reduceMotion = LocalReduceMotion.current
    val navController = rememberNavController()
    val secondaryNavigationState = rememberSecondaryNavigationState()
    val secondaryNavController = rememberNavController()
    val isPanelVisible by secondaryNavigationState.isPanelVisible.collectAsState()
    val panelType by secondaryNavigationState.panelType.collectAsState()

    val targetSecondaryFraction = when {
        largeScreen && isPanelVisible -> panelType.widthFraction
        else -> 0f // collapse completely when hidden or on small screens
    }
    val animatedSecondaryFraction by animateFloatAsState(
        targetValue = targetSecondaryFraction,
        animationSpec = if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = 320, easing = FastOutSlowInEasing)
        },
        label = "secondary_panel_width_fraction"
    )

    LaunchedEffect(largeScreen) {
        if (!largeScreen && isPanelVisible) {
            secondaryNavigationState.hidePanel()
        }
    }

    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalSecondaryNavigationState provides secondaryNavigationState,
        LocalSecondaryNavController provides secondaryNavController
    ) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Measure available width once so we can drive a width-based animation
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val maxWidthDp = this.maxWidth
                val rawSecondaryWidthDp = maxWidthDp * animatedSecondaryFraction
                // Apply width clamping based on panel type (memoized to avoid recalculation)
                val secondaryWidthDp = remember(rawSecondaryWidthDp.value, panelType, isPanelVisible, largeScreen) {
                    panelType.clampWidthDp(
                        rawWidthDp = rawSecondaryWidthDp.value,
                        isShowing = isPanelVisible && largeScreen
                    ).dp
                }

                // Primary + Secondary arranged in a Row; secondary has animated fixed width,
                // primary fills the remaining space smoothly.
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    ) {
                        DokusNavHost(
                            navController = navController,
                            navigationProvider = navigationProviders,
                            onNavHostReady = onPrimaryNavHostReady
                        )
                    }

                    // Single always-composed secondary panel; width animates to 0 when hidden
                    SecondaryPanel(
                        isPanelVisible = isPanelVisible,
                        largeScreen = largeScreen,
                        panelType = panelType,
                        secondaryNavController = secondaryNavController,
                        navigationProviders = navigationProviders,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(secondaryWidthDp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SecondaryPanel(
    isPanelVisible: Boolean,
    largeScreen: Boolean,
    panelType: SecondaryPanelType,
    secondaryNavController: NavHostController,
    navigationProviders: List<NavigationProvider>,
    modifier: Modifier,
) {
    val showPanel = isPanelVisible && largeScreen
    val reduceMotion = LocalReduceMotion.current

    // Drive a subtle fade + slide while keeping content in composition
    val visibilityProgress by animateFloatAsState(
        targetValue = if (showPanel) 1f else 0f,
        animationSpec = if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            tween(
                durationMillis = if (showPanel) 240 else 200,
                easing = FastOutSlowInEasing
            )
        },
        label = "secondary_panel_visibility"
    )

    // Elevation only when panel type has levitate enabled
    val targetElevation = if (panelType.levitate && showPanel) 8.dp else 0.dp
    val panelElevation: Dp by animateDpAsState(
        targetValue = targetElevation,
        animationSpec = if (reduceMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "secondary_panel_elevation"
    )

    // Only apply levitation offset when panel is showing, levitate is enabled, and motion is not reduced
    val shouldLevitate = panelType.levitate && showPanel && !reduceMotion

    // Levitation animation runs continuously for smoothness
    val levitationTransition = rememberInfiniteTransition(label = "secondary_panel_levitation")
    val levitationOffsetRaw by levitationTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "secondary_panel_levitation_offset"
    )

    // Only apply offset when appropriate (controlled by shouldLevitate)
    val levitationOffsetDp = remember(shouldLevitate, levitationOffsetRaw, visibilityProgress) {
        if (shouldLevitate) (levitationOffsetRaw * visibilityProgress).dp else 0.dp
    }

    val slidePx = with(LocalDensity.current) { 24.dp.toPx() }

    Surface(
        modifier = modifier
            // Edge-to-edge when not levitating; otherwise detach from edges
            .padding(if (panelType.levitate && !reduceMotion) 12.dp * visibilityProgress else 0.dp)
            .graphicsLayer {
                // slight slide-in effect; translation scales with visibility
                translationX = if (panelType.levitate && !reduceMotion) (1f - visibilityProgress) * slidePx else 0f
                alpha = if (panelType.levitate && !reduceMotion) visibilityProgress else 1f
            }
            .offset(y = levitationOffsetDp),
        shape = if (panelType.levitate) MaterialTheme.shapes.large else RoundedCornerShape(0.dp),
        tonalElevation = 0.dp,
        shadowElevation = panelElevation
    ) {
        SecondaryNavHost(
            navController = secondaryNavController,
            navigationProvider = navigationProviders
        )
    }
}