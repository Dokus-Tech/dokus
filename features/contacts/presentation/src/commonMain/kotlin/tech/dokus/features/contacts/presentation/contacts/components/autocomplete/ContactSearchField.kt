package tech.dokus.features.contacts.presentation.contacts.components.autocomplete

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import compose.icons.FeatherIcons
import compose.icons.feathericons.Search
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_clear
import tech.dokus.aura.resources.action_search
import tech.dokus.aura.resources.contacts_selected
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.constrains.Constraints

/**
 * The text input field for the autocomplete component.
 */
@Composable
internal fun ContactSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    enabled: Boolean,
    selectedContact: ContactDto?,
    onClear: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.small
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        selectedContact != null -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }
    val textStyle = LocalTextStyle.current.copy(
        fontSize = FontSizeDefault,
        color = if (enabled) {
            MaterialTheme.colorScheme.onSurface
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = DisabledAlpha)
        }
    )

    Row(
        modifier = modifier
            .height(Constraints.Height.button)
            .border(Constraints.Stroke.thin, borderColor, shape)
            .background(MaterialTheme.colorScheme.surface, shape)
            .padding(horizontal = Constraints.Spacing.medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
    ) {
        // Search icon
        PIcon(
            icon = FeatherIcons.Search,
            description = stringResource(Res.string.action_search),
            modifier = Modifier.size(Constraints.IconSize.small)
        )

        // Text input
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.CenterStart),
                    text = placeholder,
                    style = textStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = textStyle,
                singleLine = true,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.CenterStart)
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                    },
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner -> inner() }
            )
        }

        // Selected contact indicator or clear button
        if (selectedContact != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(SelectedBadgeCornerRadius)
            ) {
                Text(
                    text = stringResource(Res.string.contacts_selected),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = BadgePaddingHorizontal, vertical = BadgePaddingVertical)
                )
            }
        }

        if (value.isNotEmpty()) {
            IconButton(
                onClick = onClear,
                modifier = Modifier.size(ClearButtonSize)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = stringResource(Res.string.action_clear),
                    modifier = Modifier.size(ClearIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
