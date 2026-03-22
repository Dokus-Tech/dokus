package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.ImageLoader
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_doc_count_plural
import tech.dokus.aura.resources.contacts_doc_count_single
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.surfaceHover
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.components.badges.ContactRole as UiContactRole

private val ListItemSpacing = Constraints.Spacing.medium

/**
 * The actual list content showing contact items.
 * Desktop: compact rows with avatar. Mobile: cards.
 */
@Composable
internal fun ContactsListContent(
    contacts: List<ContactDto>,
    listState: LazyListState,
    isLoadingMore: Boolean,
    onContactClick: (ContactDto) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    selectedContactId: ContactId? = null,
    isDesktop: Boolean = false,
) {
    val imageLoader = rememberAuthenticatedImageLoader()
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = if (isDesktop) Arrangement.Top else Arrangement.spacedBy(
            ListItemSpacing
        )
    ) {
        items(
            items = contacts,
            key = { it.id.toString() }
        ) { contact ->
            if (isDesktop) {
                ContactListItem(
                    contact = contact,
                    isSelected = contact.id == selectedContactId,
                    onClick = { onContactClick(contact) },
                    imageLoader = imageLoader,
                )
            } else {
                ContactCard(
                    contact = contact,
                    imageLoader = imageLoader,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onContactClick(contact) }
                )
            }
        }

        // Loading more indicator
        if (isLoadingMore) {
            item {
                ContactsLoadingMoreIndicator()
            }
        }
    }
}

// =============================================================================
// Desktop List Item
// =============================================================================

/**
 * Desktop master list row matching the v16 split-pane structure.
 */
@Composable
internal fun ContactListItem(
    contact: ContactDto,
    isSelected: Boolean,
    onClick: () -> Unit,
    imageLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val avatarUrl = rememberResolvedApiUrl(contact.avatar?.small)

    val surfaceHover = MaterialTheme.colorScheme.surfaceHover
    val borderAmberColor = MaterialTheme.colorScheme.borderAmber

    val bgColor = when {
        isSelected || isHovered -> surfaceHover
        else -> Color.Transparent
    }

    val initials = remember(contact.name.value) { extractInitials(contact.name.value) }
    val uiRole = remember(contact.derivedRoles) { mapToUiRole(contact.derivedRoles) }
    val docCount = contact.invoiceCount + contact.inboundInvoiceCount + contact.expenseCount

    Row(
        modifier = modifier
            .fillMaxWidth()
            .hoverable(interactionSource = interactionSource)
            .pointerHoverIcon(PointerIcon.Hand)
            .clickable(onClick = onClick)
            .background(bgColor)
            .then(
                if (isSelected) {
                    Modifier.drawWithContent {
                        drawContent()
                        drawRect(
                            color = borderAmberColor,
                            topLeft = Offset(size.width - 2.dp.toPx(), 0f),
                            size = Size(2.dp.toPx(), size.height)
                        )
                    }
                } else Modifier
            )
            .padding(horizontal = Constraints.Spacing.medium, vertical = Constraints.Spacing.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
    ) {
        CompanyAvatarImage(
            avatarUrl = avatarUrl,
            initial = initials,
            size = AvatarSize.Small,
            shape = AvatarShape.RoundedSquare,
            imageLoader = imageLoader
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    fontSize = 13.sp,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val roleName = when (uiRole) {
                UiContactRole.Vendor -> stringResource(Res.string.contacts_vendor)
                UiContactRole.Bank, UiContactRole.Accountant -> null
                null -> null
            }
            val docLabel = if (docCount == 1L) {
                stringResource(Res.string.contacts_doc_count_single)
            } else {
                stringResource(Res.string.contacts_doc_count_plural)
            }
            val meta = buildString {
                if (roleName != null) append(roleName)
                if (docCount > 0L) {
                    if (isNotEmpty()) append(" \u00b7 ")
                    append("$docCount $docLabel")
                }
            }
            if (meta.isNotEmpty()) {
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
