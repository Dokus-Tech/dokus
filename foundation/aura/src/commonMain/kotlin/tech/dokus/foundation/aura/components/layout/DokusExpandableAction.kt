package tech.dokus.foundation.aura.components.layout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_collapse
import tech.dokus.aura.resources.action_expand
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

/**
 * A slot-based layout component for primary actions with expandable options.
 *
 * Provides a row with a primary action area, an expand toggle, and animated
 * expandable content. All visual elements (buttons, text, icons) are provided
 * via slots, making the component fully flexible.
 *
 * @param isExpanded Whether the expandable content is currently visible
 * @param onToggleExpand Callback when the expand toggle is clicked
 * @param modifier Modifier for the root Column
 * @param subtext Optional composable shown below the action row when collapsed
 * @param expandToggle Optional custom expand toggle; if null, uses default chevron IconButton
 * @param primaryAction Slot for the primary action area (typically a Button)
 * @param expandedContent Slot for the expandable content
 */
@Composable
fun DokusExpandableAction(
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    subtext: (@Composable () -> Unit)? = null,
    expandToggle: (@Composable () -> Unit)? = null,
    primaryAction: @Composable RowScope.() -> Unit,
    expandedContent: @Composable () -> Unit
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Primary action slot (caller provides Button, loading state, etc.)
            Row(modifier = Modifier.weight(1f)) {
                primaryAction()
            }
            // Expand toggle slot (default: chevron IconButton)
            if (expandToggle != null) {
                expandToggle()
            } else {
                IconButton(onClick = onToggleExpand) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = stringResource(
                            if (isExpanded) Res.string.action_collapse else Res.string.action_expand
                        )
                    )
                }
            }
        }
        // Subtext (only when collapsed)
        if (subtext != null && !isExpanded) {
            subtext()
        }
        // Expanded content with animation
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            expandedContent()
        }
    }
}

@Preview
@Composable
private fun DokusExpandableActionPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        DokusExpandableAction(
            isExpanded = true,
            onToggleExpand = {},
            primaryAction = { Text("Primary Action") },
            expandedContent = { Text("Expanded content here") }
        )
    }
}
