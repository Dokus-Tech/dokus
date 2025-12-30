package tech.dokus.foundation.aura.components.common

import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Generic top app bar that accepts two composable slots: one for search content (title area)
 * and one for actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PTopAppBarSearchAction(
    searchContent: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopAppBar(
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            ),
            title = {
                searchContent()
            },
            actions = {
                Spacer(Modifier.width(Constrains.Spacing.large))
                actions()
                Spacer(Modifier.width(Constrains.Spacing.large))
            }
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = Constrains.Stroke.thin
        )
    }
}
