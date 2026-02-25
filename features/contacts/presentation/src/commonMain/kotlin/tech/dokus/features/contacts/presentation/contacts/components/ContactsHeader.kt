package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_add_contact
import tech.dokus.foundation.app.network.rememberIsOnline
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIconPosition

/**
 * Action buttons for the Contacts screen top app bar.
 *
 * Displays an "Add contact" button that navigates to the contact creation form.
 *
 * Automatically disables buttons when server is unreachable using [rememberIsOnline].
 *
 * @param onAddContactClick Callback when add contact button is clicked
 * @param modifier Optional modifier for the component
 */
@Composable
internal fun ContactsHeaderActions(
    onAddContactClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOnline = rememberIsOnline()

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Add Contact button (primary action)
        // Disabled when server is unreachable since creating contacts requires network
        PButton(
            text = stringResource(Res.string.contacts_add_contact),
            variant = PButtonVariant.Outline,
            icon = Icons.Default.Add,
            iconPosition = PIconPosition.Trailing,
            onClick = onAddContactClick,
            isEnabled = isOnline
        )
    }
}
