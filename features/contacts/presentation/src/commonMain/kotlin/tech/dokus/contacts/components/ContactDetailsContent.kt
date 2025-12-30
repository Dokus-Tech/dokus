package tech.dokus.contacts.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.contacts_active
import tech.dokus.aura.resources.contacts_activity_summary
import tech.dokus.aura.resources.contacts_add_note
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_bills
import tech.dokus.aura.resources.contacts_business_info
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_info
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_customer
import tech.dokus.aura.resources.contacts_default_vat_rate
import tech.dokus.aura.resources.contacts_delete_note
import tech.dokus.aura.resources.contacts_edit_note
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_expenses
import tech.dokus.aura.resources.contacts_inactive
import tech.dokus.aura.resources.contacts_invoices
import tech.dokus.aura.resources.contacts_last_activity_value
import tech.dokus.aura.resources.contacts_no_notes
import tech.dokus.aura.resources.contacts_note_by
import tech.dokus.aura.resources.contacts_notes
import tech.dokus.aura.resources.contacts_payment_defaults
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_payment_terms_value
import tech.dokus.aura.resources.contacts_pending_approval_plural
import tech.dokus.aura.resources.contacts_pending_approval_single
import tech.dokus.aura.resources.contacts_peppol_enabled
import tech.dokus.aura.resources.contacts_peppol_settings
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_supplier
import tech.dokus.aura.resources.contacts_tags
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.contacts_vendor
import tech.dokus.domain.model.contact.ContactActivitySummary
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.domain.model.contact.ContactNoteDto
import tech.dokus.domain.model.contact.DerivedContactRoles
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.ShimmerLine

// ============================================================================
// CONTACT INFO SECTION
// ============================================================================

/**
 * Contact information section displaying all contact details.
 * Handles loading/error states.
 *
 * @param state The DokusState containing contact data
 * @param onPeppolToggle Callback when Peppol toggle is changed
 * @param isTogglingPeppol Whether Peppol toggle is in progress
 * @param modifier Optional modifier
 */
@Composable
internal fun ContactInfoSection(
    state: DokusState<ContactDto>,
    onPeppolToggle: (Boolean) -> Unit,
    isTogglingPeppol: Boolean,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.contacts_contact_info),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    ContactInfoSkeleton()
                }

                is DokusState.Success -> {
                    ContactInfoContent(
                        contact = state.data,
                        onPeppolToggle = onPeppolToggle,
                        isTogglingPeppol = isTogglingPeppol
                    )
                }

                is DokusState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusErrorContent(
                            exception = state.exception,
                            retryHandler = state.retryHandler
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ContactInfoContent(
    contact: ContactDto,
    onPeppolToggle: (Boolean) -> Unit,
    isTogglingPeppol: Boolean
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Name and status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = contact.name.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            ContactStatusLabel(isActive = contact.isActive)
        }

        // Role badges
        contact.derivedRoles?.let { roles ->
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ContactRoleBadges(roles = roles)
            }
        }

        HorizontalDivider()

        // Contact details grid
        contact.email?.let {
            ContactInfoRow(
                icon = Icons.Default.Email,
                label = stringResource(Res.string.contacts_email),
                value = it.value
            )
        }

        contact.phone?.let {
            ContactInfoRow(
                icon = Icons.Default.Phone,
                label = stringResource(Res.string.contacts_phone),
                value = it
            )
        }

        contact.contactPerson?.let {
            ContactInfoRow(
                icon = Icons.Default.Person,
                label = stringResource(Res.string.contacts_contact_person),
                value = it
            )
        }

        // Business Information
        if (contact.vatNumber != null || contact.companyNumber != null) {
            HorizontalDivider()

            Text(
                text = stringResource(Res.string.contacts_business_info),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            contact.vatNumber?.let {
                ContactInfoRow(
                    icon = Icons.Default.Receipt,
                    label = stringResource(Res.string.contacts_vat_number),
                    value = it.value
                )
            }

            contact.companyNumber?.let {
                ContactInfoRow(
                    icon = Icons.Default.Business,
                    label = stringResource(Res.string.contacts_company_number),
                    value = it
                )
            }
        }

        // Address
        val hasAddress = listOfNotNull(
            contact.addressLine1,
            contact.addressLine2,
            contact.city,
            contact.postalCode,
            contact.country
        ).isNotEmpty()

        if (hasAddress) {
            HorizontalDivider()

            Text(
                text = stringResource(Res.string.contacts_address),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            val addressLines = buildList {
                contact.addressLine1?.let { add(it) }
                contact.addressLine2?.let { add(it) }
                val cityPostal = listOfNotNull(
                    contact.postalCode,
                    contact.city
                ).joinToString(" ")
                if (cityPostal.isNotBlank()) add(cityPostal)
                contact.country?.let { add(it) }
            }

            ContactInfoRow(
                icon = Icons.Default.LocationOn,
                label = stringResource(Res.string.contacts_address),
                value = addressLines.joinToString("\n")
            )
        }

        // Peppol Settings
        HorizontalDivider()

        Text(
            text = stringResource(Res.string.contacts_peppol_settings),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column {
                    Text(
                        text = stringResource(Res.string.contacts_peppol_enabled),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    contact.peppolId?.let { id ->
                        Text(
                            text = id,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Switch(
                checked = contact.peppolEnabled,
                onCheckedChange = onPeppolToggle,
                enabled = !isTogglingPeppol && contact.peppolId != null
            )
        }

        // Payment defaults
        HorizontalDivider()

        Text(
            text = stringResource(Res.string.contacts_payment_defaults),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ContactInfoRow(
            icon = Icons.Default.Schedule,
            label = stringResource(Res.string.contacts_payment_terms),
            value = stringResource(Res.string.contacts_payment_terms_value, contact.defaultPaymentTerms)
        )

        contact.defaultVatRate?.let { rate ->
            ContactInfoRow(
                icon = Icons.Default.Payments,
                label = stringResource(Res.string.contacts_default_vat_rate),
                value = stringResource(Res.string.common_percent_value, rate.toDisplayString())
            )
        }

        // Tags
        contact.tags?.let { tagsString ->
            val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    text = stringResource(Res.string.contacts_tags),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { tag ->
                        ContactTagBadge(text = tag)
                    }
                }
            }
        }
    }
}

@Composable
private fun ContactInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ContactInfoSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShimmerLine(modifier = Modifier.fillMaxWidth(0.6f), height = 28.dp)
        Spacer(modifier = Modifier.height(4.dp))

        repeat(4) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ShimmerLine(modifier = Modifier.width(20.dp), height = 20.dp)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerLine(modifier = Modifier.width(60.dp), height = 12.dp)
                    ShimmerLine(modifier = Modifier.width(150.dp), height = 16.dp)
                }
            }
        }
    }
}

// ============================================================================
// ACTIVITY SUMMARY SECTION
// ============================================================================

/**
 * Activity summary section showing invoice/bill/expense counts and totals.
 * Handles loading/error states.
 *
 * @param state The DokusState containing activity summary data
 * @param modifier Optional modifier
 */
@Composable
internal fun ActivitySummarySection(
    state: DokusState<ContactActivitySummary>,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(Res.string.contacts_activity_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    ActivitySummarySkeleton()
                }

                is DokusState.Success -> {
                    ActivitySummaryContent(activity = state.data)
                }

                is DokusState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusErrorContent(
                            exception = state.exception,
                            retryHandler = state.retryHandler
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivitySummaryContent(
    activity: ContactActivitySummary
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Statistics row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ActivityStatCard(
                icon = Icons.Default.Description,
                title = stringResource(Res.string.contacts_invoices),
                count = activity.invoiceCount.toString(),
                total = activity.invoiceTotal,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            ActivityStatCard(
                icon = Icons.Default.Receipt,
                title = stringResource(Res.string.contacts_bills),
                count = activity.billCount.toString(),
                total = activity.billTotal,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            ActivityStatCard(
                icon = Icons.Default.ShoppingCart,
                title = stringResource(Res.string.contacts_expenses),
                count = activity.expenseCount.toString(),
                total = activity.expenseTotal,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
        }

        // Last activity
        activity.lastActivityDate?.let { lastActivity ->
            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(Res.string.contacts_last_activity_value, formatDateTime(lastActivity)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pending approvals
        if (activity.pendingApprovalCount > 0) {
            val pendingCount = activity.pendingApprovalCount
            val pendingText = if (pendingCount == 1L) {
                stringResource(Res.string.contacts_pending_approval_single, pendingCount)
            } else {
                stringResource(Res.string.contacts_pending_approval_plural, pendingCount)
            }
            DokusCardSurface(
                variant = DokusCardVariant.Soft,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = pendingText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityStatCard(
    icon: ImageVector,
    title: String,
    count: String,
    total: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier,
        padding = DokusCardPadding.Dense,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                text = count,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = total,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ActivitySummarySkeleton() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            DokusCardSurface(
                modifier = Modifier.weight(1f),
                variant = DokusCardVariant.Soft,
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ShimmerLine(modifier = Modifier.size(24.dp), height = 24.dp)
                    ShimmerLine(modifier = Modifier.width(40.dp), height = 28.dp)
                    ShimmerLine(modifier = Modifier.width(50.dp), height = 12.dp)
                    ShimmerLine(modifier = Modifier.width(60.dp), height = 12.dp)
                }
            }
        }
    }
}

// ============================================================================
// NOTES SECTION
// ============================================================================

/**
 * Notes section displaying contact notes in a timeline format.
 * Handles loading/error states.
 *
 * @param state The DokusState containing notes list
 * @param onAddNote Callback to add a new note
 * @param onEditNote Callback to edit an existing note
 * @param onDeleteNote Callback to delete a note
 * @param modifier Optional modifier
 */
@Composable
internal fun NotesSection(
    state: DokusState<List<ContactNoteDto>>,
    onAddNote: () -> Unit,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit,
    modifier: Modifier = Modifier
) {
    DokusCard(
        modifier = modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(Res.string.contacts_notes),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )

                TextButton(onClick = onAddNote) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(Res.string.contacts_add_note))
                }
            }

            when (state) {
                is DokusState.Loading, is DokusState.Idle -> {
                    NotesSkeleton()
                }

                is DokusState.Success -> {
                    if (state.data.isEmpty()) {
                        NotesEmptyState()
                    } else {
                        NotesContent(
                            notes = state.data,
                            onEditNote = onEditNote,
                            onDeleteNote = onDeleteNote
                        )
                    }
                }

                is DokusState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DokusErrorContent(
                            exception = state.exception,
                            retryHandler = state.retryHandler
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesContent(
    notes: List<ContactNoteDto>,
    onEditNote: (ContactNoteDto) -> Unit,
    onDeleteNote: (ContactNoteDto) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        notes.forEach { note ->
            NoteItem(
                note = note,
                onEdit = { onEditNote(note) },
                onDelete = { onDeleteNote(note) }
            )
        }
    }
}

@Composable
private fun NoteItem(
    note: ContactNoteDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Note,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatDateTime(note.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        note.authorName?.let { author ->
                            Text(
                                text = stringResource(Res.string.contacts_note_by, author),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Row {
                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(Res.string.contacts_edit_note),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(Res.string.contacts_delete_note),
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NotesEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Note,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(Res.string.contacts_no_notes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun NotesSkeleton() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(2) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ShimmerLine(modifier = Modifier.width(120.dp), height = 12.dp)
                    ShimmerLine(modifier = Modifier.fillMaxWidth(), height = 16.dp)
                    ShimmerLine(modifier = Modifier.fillMaxWidth(0.8f), height = 16.dp)
                }
            }
        }
    }
}

// ============================================================================
// HELPER COMPONENTS
// ============================================================================

@Composable
private fun ContactStatusLabel(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, text) = if (isActive) {
        MaterialTheme.colorScheme.primary to stringResource(Res.string.contacts_active)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant to stringResource(Res.string.contacts_inactive)
    }

    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ContactRoleBadges(
    roles: DerivedContactRoles,
    modifier: Modifier = Modifier
) {
    if (roles.isCustomer) {
        ContactRoleBadge(
            text = stringResource(Res.string.contacts_customer),
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
    if (roles.isSupplier) {
        ContactRoleBadge(
            text = stringResource(Res.string.contacts_supplier),
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier
        )
    }
    if (roles.isVendor) {
        ContactRoleBadge(
            text = stringResource(Res.string.contacts_vendor),
            color = MaterialTheme.colorScheme.secondary,
            modifier = modifier
        )
    }
}

@Composable
private fun ContactRoleBadge(
    text: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ContactTagBadge(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
        shape = RoundedCornerShape(4.dp),
        modifier = modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

// ============================================================================
// UTILITY FUNCTIONS
// ============================================================================

/**
 * Format a LocalDateTime to a human-readable string.
 */
private fun formatDateTime(dateTime: LocalDateTime): String {
    val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
    return "${dateTime.dayOfMonth} $month ${dateTime.year}, ${dateTime.hour.toString().padStart(2, '0')}:${dateTime.minute.toString().padStart(2, '0')}"
}
