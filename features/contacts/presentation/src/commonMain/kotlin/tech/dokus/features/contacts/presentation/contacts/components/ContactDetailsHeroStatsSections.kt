package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Merge
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.X
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.contacts_documents
import tech.dokus.aura.resources.contacts_no_peppol
import tech.dokus.aura.resources.contacts_outstanding
import tech.dokus.aura.resources.contacts_total_volume
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.foundation.app.network.rememberAuthenticatedImageLoader
import tech.dokus.foundation.app.network.rememberResolvedApiUrl
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.AvatarShape
import tech.dokus.foundation.aura.components.AvatarSize
import tech.dokus.foundation.aura.components.CompanyAvatarImage
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.badges.RoleBadge
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.local.isLarge
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.greenSoft
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted
import kotlin.math.abs

@Composable
internal fun ContactHeroSection(
    contactState: DokusState<ContactDto>,
    peppolStatusState: DokusState<PeppolStatusResponse>,
    showInlineActions: Boolean,
    hasEnrichmentSuggestions: Boolean,
    isEditing: Boolean,
    isSavingEdit: Boolean = false,
    isOnline: Boolean,
    onEditContact: () -> Unit,
    onSaveEdit: () -> Unit = {},
    onCancelEdit: () -> Unit = {},
    onMergeContact: () -> Unit,
    onShowEnrichment: () -> Unit,
) {
    val imageLoader = rememberAuthenticatedImageLoader()
    DokusCardSurface(accent = true) {
        when (contactState) {
            is DokusState.Loading, is DokusState.Idle -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerLine(modifier = Modifier.width(160.dp), height = 20.dp)
                    ShimmerLine(modifier = Modifier.width(120.dp), height = 14.dp)
                }
            }

            is DokusState.Success -> {
                val contact = contactState.data
                val initials = remember(contact.name.value) { extractInitials(contact.name.value) }
                val uiRole = remember(contact.derivedRoles) { mapToUiRole(contact.derivedRoles) }
                val avatarUrl = rememberResolvedApiUrl(contact.avatar?.medium ?: contact.avatar?.small)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CompanyAvatarImage(
                        avatarUrl = avatarUrl,
                        initial = initials,
                        size = AvatarSize.Large,
                        shape = AvatarShape.RoundedSquare,
                        imageLoader = imageLoader
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = contact.name.value,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (uiRole != null) {
                                RoleBadge(role = uiRole)
                            }
                            contact.vatNumber?.let { vat ->
                                Text(
                                    text = vat.value,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                                    ),
                                    color = MaterialTheme.colorScheme.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    PeppolBadge(state = peppolStatusState)
                }

                if (showInlineActions) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing) {
                            // Cancel edit
                            IconButton(
                                onClick = onCancelEdit,
                                enabled = !isSavingEdit
                            ) {
                                Icon(
                                    imageVector = Lucide.X,
                                    contentDescription = null
                                )
                            }
                            // Save edit (check icon)
                            IconButton(
                                onClick = onSaveEdit,
                                enabled = !isSavingEdit
                            ) {
                                Icon(
                                    imageVector = Lucide.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            if (hasEnrichmentSuggestions) {
                                IconButton(
                                    onClick = onShowEnrichment,
                                    enabled = isOnline
                                ) {
                                    Icon(
                                        imageVector = Lucide.Sparkles,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.statusWarning
                                    )
                                }
                            }

                            IconButton(
                                onClick = onMergeContact,
                                enabled = isOnline
                            ) {
                                Icon(
                                    imageVector = Lucide.Merge,
                                    contentDescription = null
                                )
                            }

                            IconButton(
                                onClick = onEditContact,
                                enabled = isOnline
                            ) {
                                Icon(
                                    imageVector = Lucide.Pencil,
                                    contentDescription = null
                                )
                            }
                        }
                    }
                }
            }

            is DokusState.Error -> Unit
        }
    }
}

@Composable
private fun PeppolBadge(state: DokusState<PeppolStatusResponse>) {
    val badge = when (state) {
        is DokusState.Success -> when (state.data.status) {
            PeppolStatusResponse.STATUS_FOUND -> PeppolBadgeStyle(
                label = "PEPPOL",
                color = MaterialTheme.colorScheme.statusConfirmed,
                background = MaterialTheme.colorScheme.greenSoft
            )

            PeppolStatusResponse.STATUS_NOT_FOUND -> PeppolBadgeStyle(
                label = stringResource(Res.string.contacts_no_peppol),
                color = MaterialTheme.colorScheme.textMuted,
                background = MaterialTheme.colorScheme.surfaceVariant
            )

            else -> PeppolBadgeStyle(
                label = stringResource(Res.string.common_unknown),
                color = MaterialTheme.colorScheme.textMuted,
                background = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        else -> PeppolBadgeStyle(
            label = stringResource(Res.string.common_unknown),
            color = MaterialTheme.colorScheme.textMuted,
            background = MaterialTheme.colorScheme.surfaceVariant
        )
    }

    Row(
        modifier = Modifier
            .background(
                color = badge.background,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(badge.color, CircleShape)
        )
        Text(
            text = badge.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = badge.color
        )
    }
}

@Composable
internal fun ContactStatsSection(
    invoiceSnapshotState: DokusState<ContactInvoiceSnapshot>
) {
    val isLargeScreen = LocalScreenSize.isLarge
    val snapshot = (invoiceSnapshotState as? DokusState.Success)?.data
    val stats = listOf(
        ContactStatCardModel(
            label = stringResource(Res.string.contacts_total_volume),
            value = snapshot?.totalVolume?.let { formatEuro(it.minor) } ?: "—",
            valueColor = MaterialTheme.colorScheme.onSurface
        ),
        ContactStatCardModel(
            label = stringResource(Res.string.contacts_outstanding),
            value = snapshot?.outstanding?.let { formatEuro(it.minor) } ?: "—",
            valueColor = MaterialTheme.colorScheme.statusError
        ),
        ContactStatCardModel(
            label = stringResource(Res.string.contacts_documents),
            value = snapshot?.documentsCount?.toString() ?: "—",
            valueColor = MaterialTheme.colorScheme.onSurface
        ),
    )

    if (invoiceSnapshotState is DokusState.Loading || invoiceSnapshotState is DokusState.Idle) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            repeat(3) {
                DokusCardSurface(modifier = Modifier.weight(1f), variant = DokusCardVariant.Soft) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShimmerLine(modifier = Modifier.width(80.dp), height = 10.dp)
                        ShimmerLine(modifier = Modifier.width(100.dp), height = 16.dp)
                    }
                }
            }
        }
        return
    }

    if (isLargeScreen) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            stats.forEach { stat ->
                ContactStatCard(stat = stat, modifier = Modifier.weight(1f))
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ContactStatCard(stat = stats[0], modifier = Modifier.weight(1f))
                ContactStatCard(stat = stats[1], modifier = Modifier.weight(1f))
            }
            ContactStatCard(stat = stats[2], modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ContactStatCard(
    stat: ContactStatCardModel,
    modifier: Modifier = Modifier
) {
    DokusCardSurface(
        modifier = modifier,
        variant = DokusCardVariant.Soft
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.textMuted
            )
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = MaterialTheme.typography.labelLarge.fontFamily
                ),
                color = stat.valueColor
            )
        }
    }
}

internal fun formatEuro(minor: Long): String {
    val sign = if (minor < 0) "\u2212" else ""
    val absolute = abs(minor)
    val integerPart = absolute / 100
    val decimalPart = absolute % 100
    val grouped = integerPart
        .toString()
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()
    return "$sign\u20ac$grouped,${decimalPart.toString().padStart(2, '0')}"
}

@Immutable
private data class ContactStatCardModel(
    val label: String,
    val value: String,
    val valueColor: Color
)

@Immutable
private data class PeppolBadgeStyle(
    val label: String,
    val color: Color,
    val background: Color
)
