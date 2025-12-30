package tech.dokus.foundation.aura.components.common

import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FeatherIcons
import compose.icons.feathericons.Briefcase

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PSearchActionTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    actionText: String,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
    actionIcon: ImageVector? = FeatherIcons.Briefcase,
) {
    CenterAlignedTopAppBar(
        modifier = modifier,
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        title = {
            PSearchFieldCompact(
                value = query,
                onValueChange = onQueryChange,
                placeholder = searchPlaceholder
            )
        },
        actions = {
            PButton(
                text = actionText,
                variant = PButtonVariant.Outline,
                icon = actionIcon,
                contentDescription = null,
                onClick = onActionClick
            )
        }
    )
}
