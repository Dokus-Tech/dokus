package ai.dokus.app.navigation.secondary

import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import ai.dokus.foundation.navigation.NavigationProvider
import ai.dokus.foundation.navigation.local.LocalNavController
import ai.dokus.foundation.navigation.local.LocalSecondaryNavController
import ai.dokus.foundation.navigation.local.LocalSecondaryNavigationState
import ai.dokus.foundation.navigation.local.rememberSecondaryNavigationState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import ai.dokus.app.navigation.DokusNavHost

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
    val navController = rememberNavController()
    val secondaryNavigationState = rememberSecondaryNavigationState()
    val secondaryNavController = rememberNavController()
    val isPanelVisible by secondaryNavigationState.isPanelVisible.collectAsState()
    val complimentary by secondaryNavigationState.complimentary.collectAsState()

    val primaryWeight by remember(largeScreen, isPanelVisible, complimentary) {
        derivedStateOf {
            when {
                largeScreen && isPanelVisible && !complimentary -> 0.5f
                largeScreen && isPanelVisible && complimentary -> 0.55f
                else -> 1.0f
            }
        }
    }
    val secondaryWeight by remember(largeScreen, isPanelVisible, complimentary) {
        derivedStateOf {
            when {
                largeScreen && isPanelVisible && !complimentary -> 0.5f
                largeScreen && isPanelVisible && complimentary -> 0.45f
                else -> 1.0f
            }
        }
    }

    // Auto-hide secondary panel on small screens
    LaunchedEffect(largeScreen) {
        if (!largeScreen && isPanelVisible) {
            secondaryNavigationState.hidePanel()
        }
    }

    // Provide secondary navigation state and controller to children
    CompositionLocalProvider(
        LocalNavController provides navController,
        LocalSecondaryNavigationState provides secondaryNavigationState,
        LocalSecondaryNavController provides secondaryNavController
    ) {
        Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            // Main content in a Row
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .weight(primaryWeight)
                        .fillMaxHeight()
                ) {
                    DokusNavHost(
                        navController = navController,
                        navigationProvider = navigationProviders,
                        onNavHostReady = onPrimaryNavHostReady
                    )
                }

                if (complimentary && isPanelVisible) {
                    SecondaryPanel(
                        isPanelVisible,
                        largeScreen,
                        secondaryNavController,
                        navigationProviders,
                        modifier = Modifier.weight(secondaryWeight)
                    )
                } else if (isPanelVisible) {
                    Surface(
                        modifier = Modifier
                            .weight(secondaryWeight)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        SecondaryNavHost(
                            navController = secondaryNavController,
                            navigationProvider = navigationProviders
                        )
                    }
                }

                // Hidden SecondaryNavHost to ensure navigation graph is always set
                // This is positioned off-screen so it doesn't interfere with the UI
                if (!isPanelVisible || !largeScreen) {
                    Box(
                        modifier = Modifier.size(0.dp)
                    ) {
                        SecondaryNavHost(
                            navController = secondaryNavController,
                            navigationProvider = navigationProviders
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowScope.SecondaryPanel(
    isPanelVisible: Boolean,
    largeScreen: Boolean,
    secondaryNavController: NavHostController,
    navigationProviders: List<NavigationProvider>,
    modifier: Modifier,
) {
    val showPanel = isPanelVisible && largeScreen

    // Animate the elevation when the complementary panel appears
    val panelElevation: Dp by animateDpAsState(
        targetValue = if (showPanel) 8.dp else 0.dp,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "secondary_panel_elevation"
    )

    // Complementary panel with animated reveal and elevation
    AnimatedVisibility(
        modifier = modifier.fillMaxHeight(),
        visible = showPanel,
        enter = fadeIn(animationSpec = tween(180)) +
                slideInHorizontally(
                    animationSpec = tween(
                        durationMillis = 320,
                        easing = FastOutSlowInEasing
                    ),
                    initialOffsetX = { it / 6 }
                ),
        exit = slideOutHorizontally(
            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing),
            targetOffsetX = { it / 6 }
        ) + fadeOut(animationSpec = tween(160))
    ) {
        // Gentle continuous levitation for the complementary panel
        val levitationTransition =
            rememberInfiniteTransition(label = "secondary_panel_levitation")
        val levitationOffsetRaw by levitationTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "secondary_panel_levitation_offset"
        )
        val levitationOffsetDp = levitationOffsetRaw.dp

        Surface(
            modifier = Modifier
                .padding(12.dp) // detach from screen edges
                .offset(y = levitationOffsetDp)
                .fillMaxSize(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 0.dp,
            shadowElevation = panelElevation
        ) {
            SecondaryNavHost(
                navController = secondaryNavController,
                navigationProvider = navigationProviders
            )
        }
    }
}