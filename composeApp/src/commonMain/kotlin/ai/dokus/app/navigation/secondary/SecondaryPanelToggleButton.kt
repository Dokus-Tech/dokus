package ai.dokus.app.navigation.secondary

import ai.dokus.foundation.navigation.local.LocalSecondaryNavigationState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier

/**
 * Toggle button for showing/hiding the secondary navigation panel.
 * Shows different icons based on panel visibility state.
 */
@Composable
fun SecondaryPanelToggleButton(
    modifier: Modifier = Modifier
) {
    val secondaryNavState = LocalSecondaryNavigationState.current
    val isPanelVisible by secondaryNavState.isPanelVisible.collectAsState()

    IconButton(
        onClick = { secondaryNavState.togglePanel() },
        modifier = modifier
    ) {
        AnimatedVisibility(
            visible = !isPanelVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                contentDescription = "Show help panel",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        AnimatedVisibility(
            visible = isPanelVisible,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Help,
                contentDescription = "Hide help panel",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Extended FAB version of the toggle button for more prominent placement.
 */
@Composable
fun SecondaryPanelToggleFAB(
    modifier: Modifier = Modifier
) {
    val secondaryNavState = LocalSecondaryNavigationState.current
    val isPanelVisible by secondaryNavState.isPanelVisible.collectAsState()

    ExtendedFloatingActionButton(
        onClick = { secondaryNavState.togglePanel() },
        modifier = modifier,
        containerColor = if (isPanelVisible) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.secondaryContainer
        },
        contentColor = if (isPanelVisible) {
            MaterialTheme.colorScheme.onPrimary
        } else {
            MaterialTheme.colorScheme.onSecondaryContainer
        }
    ) {
        Icon(
            imageVector = if (isPanelVisible) {
                Icons.AutoMirrored.Filled.Help
            } else {
                Icons.AutoMirrored.Filled.HelpOutline
            },
            contentDescription = if (isPanelVisible) {
                "Hide help panel"
            } else {
                "Show help panel"
            }
        )
    }
}