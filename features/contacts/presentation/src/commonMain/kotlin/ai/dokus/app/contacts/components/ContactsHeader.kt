package ai.dokus.app.contacts.components

import ai.dokus.foundation.design.components.PButton
import ai.dokus.foundation.design.components.PButtonVariant
import ai.dokus.foundation.design.components.PIconPosition
import ai.dokus.foundation.design.components.common.PSearchFieldCompact
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search

/**
 * Search content for the Contacts screen top app bar.
 *
 * Displays a search field that can be collapsed on mobile and expanded on desktop.
 * On mobile, shows a search icon button that expands to reveal the search field.
 *
 * @param searchQuery The current search query text
 * @param onSearchQueryChange Callback when search query changes
 * @param isSearchExpanded Whether the search field is expanded (always true on desktop)
 * @param isLargeScreen Whether the screen is in desktop/large mode
 * @param onExpandSearch Callback when search should be expanded (mobile only)
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactsHeaderSearch(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    isSearchExpanded: Boolean,
    isLargeScreen: Boolean,
    onExpandSearch: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Show search icon button on mobile when search is collapsed
        if (!isLargeScreen && !isSearchExpanded) {
            IconButton(
                onClick = onExpandSearch,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = FeatherIcons.Search,
                    contentDescription = "Search contacts"
                )
            }
        }

        // Animated search field
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandHorizontally(expandFrom = Alignment.Start) + fadeIn(),
            exit = shrinkHorizontally(shrinkTowards = Alignment.Start) + fadeOut()
        ) {
            PSearchFieldCompact(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = "Search contacts...",
                modifier = if (isLargeScreen) Modifier else Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Action buttons for the Contacts screen top app bar.
 *
 * Displays an "Add contact" button that navigates to the contact creation form.
 *
 * @param onAddContactClick Callback when add contact button is clicked
 * @param isOffline Whether the device is currently offline (disables create actions)
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactsHeaderActions(
    onAddContactClick: () -> Unit,
    isOffline: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add Contact button (primary action)
        // Disabled when offline since creating contacts requires network
        PButton(
            text = "Add contact",
            variant = PButtonVariant.Outline,
            icon = Icons.Default.Add,
            iconPosition = PIconPosition.Trailing,
            onClick = onAddContactClick,
            isEnabled = !isOffline
        )
    }
}
