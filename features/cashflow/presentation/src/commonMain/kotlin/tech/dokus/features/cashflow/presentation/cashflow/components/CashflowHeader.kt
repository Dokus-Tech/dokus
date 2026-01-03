package tech.dokus.features.cashflow.presentation.cashflow.components

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import compose.icons.feathericons.UploadCloud
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.cashflow_create_invoice
import tech.dokus.aura.resources.cashflow_search_placeholder
import tech.dokus.aura.resources.cashflow_upload_document
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIconPosition
import tech.dokus.foundation.aura.components.common.PSearchFieldCompact

/**
 * Search content for the Cashflow screen top app bar.
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
fun CashflowHeaderSearch(
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
                    contentDescription = stringResource(Res.string.action_search)
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
                placeholder = stringResource(Res.string.cashflow_search_placeholder),
                modifier = if (isLargeScreen) Modifier else Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Action buttons for the Cashflow screen top app bar.
 *
 * Displays upload and create invoice actions. The upload button opens
 * the sidebar on desktop or navigates to the add document screen on mobile.
 *
 * Automatically disables buttons when server is unreachable using [rememberIsOnline].
 *
 * @param onUploadClick Callback when upload button is clicked
 * @param onCreateInvoiceClick Callback when create invoice button is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
fun CashflowHeaderActions(
    onUploadClick: () -> Unit,
    onCreateInvoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = rememberIsOnline()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Upload icon button (secondary action - drag & drop is primary)
        // Disabled when server is unreachable since uploads require network
        IconButton(
            onClick = onUploadClick,
            enabled = isOnline
        ) {
            Icon(
                imageVector = FeatherIcons.UploadCloud,
                contentDescription = stringResource(Res.string.cashflow_upload_document),
                tint = if (!isOnline) {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }

        // Create Invoice button (primary action)
        // Disabled when server is unreachable since creating invoices requires network
        PButton(
            text = stringResource(Res.string.cashflow_create_invoice),
            variant = PButtonVariant.Outline,
            icon = Icons.Default.Add,
            iconPosition = PIconPosition.Trailing,
            onClick = onCreateInvoiceClick,
            isEnabled = isOnline
        )
    }
}
