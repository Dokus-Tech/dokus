package tech.dokus.foundation.aura.components.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import tech.dokus.foundation.aura.constrains.Constrains

private const val SELECTED_ALPHA = 0.08f
private const val FOCUS_RING_ALPHA = 0.12f

/**
 * A text-only selectable row for use in dialogs and selection lists.
 * Follows the same selection pattern as SelectableCard (8% primary alpha).
 *
 * @param text The text to display in the row
 * @param isSelected Whether this row is currently selected
 * @param onClick Called when the row is clicked
 * @param modifier Modifier for the row container
 * @param enabled Whether the row is enabled for interaction
 * @param showCheckmark Whether to show a checkmark icon when selected
 */
@Composable
fun DokusSelectableRow(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    showCheckmark: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = SELECTED_ALPHA)
            isFocused -> MaterialTheme.colorScheme.primary.copy(alpha = FOCUS_RING_ALPHA)
            else -> MaterialTheme.colorScheme.surface.copy(alpha = 0f)
        },
        label = "backgroundColor"
    )

    val textColor = if (enabled) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick
            )
            .focusable(interactionSource = interactionSource)
            .padding(
                horizontal = Constrains.Spacing.large,
                vertical = Constrains.Spacing.medium
            )
            .semantics { selected = isSelected },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.weight(1f)
        )

        if (showCheckmark && isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(Constrains.IconSize.small)
            )
        }
    }
}

/**
 * A group of selectable rows with keyboard navigation support.
 * Supports arrow key navigation and Enter/Space to select.
 *
 * @param items List of items to display
 * @param selectedItem Currently selected item
 * @param onItemSelected Called when an item is selected
 * @param itemText Composable function to extract display text from an item
 * @param modifier Modifier for the column container
 * @param enabled Whether the group is enabled for interaction
 * @param requestInitialFocus Whether to request focus on the selected item when first composed
 */
@Composable
fun <T> DokusSelectableRowGroup(
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    itemText: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    requestInitialFocus: Boolean = false,
) {
    val focusRequesters = remember(items.size) {
        List(items.size) { FocusRequester() }
    }

    var focusedIndex by remember(items, selectedItem) {
        mutableIntStateOf(items.indexOf(selectedItem).coerceAtLeast(0))
    }

    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus && focusRequesters.isNotEmpty()) {
            focusRequesters[focusedIndex].requestFocus()
        }
    }

    Column(
        modifier = modifier
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyDown && enabled) {
                    when (event.key) {
                        Key.DirectionDown -> {
                            val newIndex = (focusedIndex + 1).coerceAtMost(items.lastIndex)
                            if (newIndex != focusedIndex) {
                                focusedIndex = newIndex
                                focusRequesters[focusedIndex].requestFocus()
                            }
                            true
                        }
                        Key.DirectionUp -> {
                            val newIndex = (focusedIndex - 1).coerceAtLeast(0)
                            if (newIndex != focusedIndex) {
                                focusedIndex = newIndex
                                focusRequesters[focusedIndex].requestFocus()
                            }
                            true
                        }
                        Key.Enter, Key.Spacebar -> {
                            onItemSelected(items[focusedIndex])
                            true
                        }
                        else -> false
                    }
                } else {
                    false
                }
            },
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
    ) {
        items.forEachIndexed { index, item ->
            DokusSelectableRow(
                text = itemText(item),
                isSelected = item == selectedItem,
                onClick = { onItemSelected(item) },
                enabled = enabled,
                modifier = Modifier.focusRequester(focusRequesters[index])
            )
        }
    }
}
