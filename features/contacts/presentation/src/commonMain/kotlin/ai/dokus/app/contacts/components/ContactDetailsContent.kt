package ai.dokus.app.contacts.components

import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.components.common.ShimmerLine
import ai.dokus.foundation.domain.model.ContactActivitySummary
import ai.dokus.foundation.domain.model.ContactDto
import ai.dokus.foundation.domain.model.ContactNoteDto
import ai.dokus.foundation.domain.model.DerivedContactRoles
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tech.dokus.foundation.app.state.DokusState

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
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Contact Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                fontWeight = FontWeight.Bold,
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
                label = "Email",
                value = it.value
            )
        }

        contact.phone?.let {
            ContactInfoRow(
                icon = Icons.Default.Phone,
                label = "Phone",
                value = it
            )
        }

        contact.contactPerson?.let {
            ContactInfoRow(
                icon = Icons.Default.Person,
                label = "Contact Person",
                value = it
            )
        }

        // Business Information
        if (contact.vatNumber != null || contact.companyNumber != null) {
            HorizontalDivider()

            Text(
                text = "Business Information",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            contact.vatNumber?.let {
                ContactInfoRow(
                    icon = Icons.Default.Receipt,
                    label = "VAT Number",
                    value = it.value
                )
            }

            contact.companyNumber?.let {
                ContactInfoRow(
                    icon = Icons.Default.Business,
                    label = "Company Number",
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
                text = "Address",
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
                label = "Address",
                value = addressLines.joinToString("\n")
            )
        }

        // Peppol Settings
        HorizontalDivider()

        Text(
            text = "Peppol Settings",
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
                        text = "Peppol Enabled",
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
            text = "Payment Defaults",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        ContactInfoRow(
            icon = Icons.Default.Schedule,
            label = "Payment Terms",
            value = "${contact.defaultPaymentTerms} days"
        )

        contact.defaultVatRate?.let { rate ->
            ContactInfoRow(
                icon = Icons.Default.Payments,
                label = "Default VAT Rate",
                value = "${rate.value}%"
            )
        }

        // Tags
        contact.tags?.let { tagsString ->
            val tags = tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    text = "Tags",
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
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Activity Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
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
                title = "Invoices",
                count = activity.invoiceCount.toString(),
                total = activity.invoiceTotal,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )

            ActivityStatCard(
                icon = Icons.Default.Receipt,
                title = "Bills",
                count = activity.billCount.toString(),
                total = activity.billTotal,
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )

            ActivityStatCard(
                icon = Icons.Default.ShoppingCart,
                title = "Expenses",
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
                    text = "Last activity: ${formatDateTime(lastActivity)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Pending approvals
        if (activity.pendingApprovalCount > 0) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activity.pendingApprovalCount} pending approval${if (activity.pendingApprovalCount > 1) "s" else ""}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
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
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
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
                fontWeight = FontWeight.Bold,
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
            Surface(
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
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
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                TextButton(onClick = onAddNote) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Note")
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
                                text = "by $author",
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
                            contentDescription = "Edit note",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete note",
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
                text = "No notes yet",
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
        MaterialTheme.colorScheme.primary to "Active"
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant to "Inactive"
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
            text = "Customer",
            color = MaterialTheme.colorScheme.primary,
            modifier = modifier
        )
    }
    if (roles.isSupplier) {
        ContactRoleBadge(
            text = "Supplier",
            color = MaterialTheme.colorScheme.tertiary,
            modifier = modifier
        )
    }
    if (roles.isVendor) {
        ContactRoleBadge(
            text = "Vendor",
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
