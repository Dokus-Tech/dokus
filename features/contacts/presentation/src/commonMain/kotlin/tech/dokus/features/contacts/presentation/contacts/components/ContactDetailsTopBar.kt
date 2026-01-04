package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Badge
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_back
import tech.dokus.aura.resources.contacts_contact_details
import tech.dokus.aura.resources.contacts_edit_contact
import tech.dokus.aura.resources.contacts_enrichment_available
import tech.dokus.aura.resources.contacts_merge
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.common.ShimmerLine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactDetailsTopBar(
    contactState: DokusState<ContactDto>,
    showBackButton: Boolean,
    hasEnrichmentSuggestions: Boolean,
    onBackClick: () -> Unit,
    onEditClick: () -> Unit,
    onEnrichmentClick: () -> Unit,
    onMergeClick: () -> Unit,
    isOnline: Boolean = true
) {
    Column {
        TopAppBar(
            navigationIcon = {
                if (showBackButton) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back)
                        )
                    }
                }
            },
            title = {
                when (contactState) {
                    is DokusState.Success -> {
                        Text(
                            text = contactState.data.name.value,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    is DokusState.Loading -> {
                        ShimmerLine(modifier = androidx.compose.ui.Modifier.width(150.dp), height = 24.dp)
                    }
                    else -> {
                        Text(
                            text = stringResource(Res.string.contacts_contact_details),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            },
            actions = {
                if (hasEnrichmentSuggestions) {
                    IconButton(onClick = onEnrichmentClick) {
                        Box {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = stringResource(Res.string.contacts_enrichment_available),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Badge(
                                modifier = androidx.compose.ui.Modifier.align(Alignment.TopEnd),
                                containerColor = MaterialTheme.colorScheme.tertiary
                            ) { }
                        }
                    }
                }

                IconButton(
                    onClick = onMergeClick,
                    enabled = isOnline
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.MergeType,
                        contentDescription = stringResource(Res.string.contacts_merge)
                    )
                }

                IconButton(
                    onClick = onEditClick,
                    enabled = isOnline
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(Res.string.contacts_edit_contact)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                navigationIconContentColor = Color.Unspecified,
                titleContentColor = Color.Unspecified,
                actionIconContentColor = Color.Unspecified
            )
        )

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        )
    }
}
