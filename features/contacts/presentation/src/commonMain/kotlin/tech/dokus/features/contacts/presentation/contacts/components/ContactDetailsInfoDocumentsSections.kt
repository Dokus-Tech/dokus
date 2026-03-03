package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_no_documents
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_recent_documents
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.InvoiceStatus
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.usecases.ContactInvoiceSnapshot
import tech.dokus.features.contacts.usecases.ContactRecentInvoice
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.components.common.ShimmerLine
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.greenSoft
import tech.dokus.foundation.aura.style.redSoft
import tech.dokus.foundation.aura.style.statusConfirmed
import tech.dokus.foundation.aura.style.statusError
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted

private val InfoLabelWidth = 120.dp

@Composable
internal fun ContactInfoSectionCompact(contact: ContactDto?) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
        variant = DokusCardVariant.Soft,
    ) {
        val rows = if (contact == null) {
            listOf(
                stringResource(Res.string.contacts_vat_number) to "—",
                stringResource(Res.string.contacts_address) to "—",
                stringResource(Res.string.contacts_email) to "—",
                stringResource(Res.string.contacts_payment_terms) to "—"
            )
        } else {
            listOf(
                stringResource(Res.string.contacts_vat_number) to (contact.vatNumber?.value ?: "—"),
                stringResource(Res.string.contacts_address) to formatAddress(contact),
                stringResource(Res.string.contacts_email) to (contact.email?.value ?: "—"),
                stringResource(Res.string.contacts_payment_terms) to "Net ${contact.defaultPaymentTerms}"
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { index, row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.first,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.textMuted,
                        modifier = Modifier.widthIn(min = InfoLabelWidth)
                    )
                    Text(
                        text = row.second,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (index == 0) {
                                MaterialTheme.typography.labelLarge.fontFamily
                            } else {
                                MaterialTheme.typography.bodyLarge.fontFamily
                            }
                        ),
                        color = if (row.second == "—") {
                            MaterialTheme.colorScheme.textFaint
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun RecentDocumentsSection(
    invoiceSnapshotState: DokusState<ContactInvoiceSnapshot>
) {
    Text(
        text = stringResource(Res.string.contacts_recent_documents),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )

    when (invoiceSnapshotState) {
        is DokusState.Loading, is DokusState.Idle -> {
            DokusCardSurface(variant = DokusCardVariant.Soft) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    repeat(3) {
                        ShimmerLine(modifier = Modifier.fillMaxWidth(), height = 14.dp)
                    }
                }
            }
        }

        is DokusState.Success -> {
            if (invoiceSnapshotState.data.recentDocuments.isEmpty()) {
                DokusCardSurface(variant = DokusCardVariant.Soft) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(Res.string.contacts_no_documents),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.textMuted
                        )
                    }
                }
            } else {
                DokusCardSurface(variant = DokusCardVariant.Soft) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        invoiceSnapshotState.data.recentDocuments.forEachIndexed { index, document ->
                            if (index > 0) {
                                Spacer(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(MaterialTheme.colorScheme.outlineVariant)
                                )
                            }
                            RecentDocumentRow(document = document)
                        }
                    }
                }
            }
        }

        is DokusState.Error -> {
            DokusCardSurface(variant = DokusCardVariant.Soft) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(Res.string.common_unknown),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentDocumentRow(document: ContactRecentInvoice) {
    val statusStyle = invoiceStatusStyle(document.status)
    val signedMinor = when (document.direction) {
        DocumentDirection.Inbound -> -document.totalAmount.minor
        else -> document.totalAmount.minor
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .background(statusStyle.color, CircleShape)
        )
        Text(
            text = formatMonthDay(document.issueDate.monthNumber, document.issueDate.dayOfMonth),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily
            ),
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.width(52.dp)
        )
        Text(
            text = "Invoice",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = document.status.localized,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = statusStyle.color,
            modifier = Modifier
                .background(statusStyle.background, MaterialTheme.shapes.small)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Text(
            text = formatEuro(signedMinor),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = statusStyle.color
        )
    }
}

private fun formatAddress(contact: ContactDto): String {
    val cityPostal = listOfNotNull(contact.postalCode, contact.city).joinToString(" ").trim()
    val parts = listOfNotNull(
        contact.addressLine1,
        contact.addressLine2,
        cityPostal.takeIf { it.isNotBlank() },
        contact.country
    )
    return if (parts.isEmpty()) "—" else parts.joinToString(", ")
}

private fun formatMonthDay(monthNumber: Int, day: Int): String {
    val month = when (monthNumber) {
        1 -> "Jan"
        2 -> "Feb"
        3 -> "Mar"
        4 -> "Apr"
        5 -> "May"
        6 -> "Jun"
        7 -> "Jul"
        8 -> "Aug"
        9 -> "Sep"
        10 -> "Oct"
        11 -> "Nov"
        else -> "Dec"
    }
    return "$month $day"
}

@Composable
private fun invoiceStatusStyle(status: InvoiceStatus): InvoiceStatusStyle {
    return when (status) {
        InvoiceStatus.Paid, InvoiceStatus.Refunded -> InvoiceStatusStyle(
            color = MaterialTheme.colorScheme.statusConfirmed,
            background = MaterialTheme.colorScheme.greenSoft
        )

        InvoiceStatus.Overdue -> InvoiceStatusStyle(
            color = MaterialTheme.colorScheme.statusError,
            background = MaterialTheme.colorScheme.redSoft
        )

        else -> InvoiceStatusStyle(
            color = MaterialTheme.colorScheme.statusWarning,
            background = MaterialTheme.colorScheme.amberSoft
        )
    }
}

@Immutable
private data class InvoiceStatusStyle(
    val color: Color,
    val background: Color
)
