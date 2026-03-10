package tech.dokus.features.contacts.presentation.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Month
import kotlinx.datetime.number
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_unknown
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_no_documents
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_recent_documents
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.contacts_website
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

private enum class InfoRowStyle { Plain, Link }

@Immutable
private data class InfoRow(
    val label: String,
    val value: String,
    val style: InfoRowStyle = InfoRowStyle.Plain,
)

@Composable
internal fun ContactInfoSectionCompact(contact: ContactDto?) {
    val uriHandler = LocalUriHandler.current

    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default,
        variant = DokusCardVariant.Soft,
    ) {
        val rows = if (contact == null) {
            listOf(
                InfoRow(stringResource(Res.string.contacts_vat_number), "—"),
                InfoRow(stringResource(Res.string.contacts_address), "—"),
                InfoRow(stringResource(Res.string.contacts_email), "—"),
                InfoRow(stringResource(Res.string.contacts_website), "—"),
                InfoRow(stringResource(Res.string.contacts_payment_terms), "—"),
            )
        } else {
            listOf(
                InfoRow(stringResource(Res.string.contacts_vat_number), contact.vatNumber?.value ?: "—"),
                InfoRow(stringResource(Res.string.contacts_address), formatAddress(contact)),
                InfoRow(stringResource(Res.string.contacts_email), contact.email?.value ?: "—"),
                InfoRow(
                    label = stringResource(Res.string.contacts_website),
                    value = contact.websiteUrl ?: "—",
                    style = if (contact.websiteUrl != null) InfoRowStyle.Link else InfoRowStyle.Plain,
                ),
                InfoRow(stringResource(Res.string.contacts_payment_terms), "Net ${contact.defaultPaymentTerms}"),
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rows.forEachIndexed { index, row ->
                val isLink = row.style == InfoRowStyle.Link
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (isLink) {
                                Modifier.clickable {
                                    try {
                                        uriHandler.openUri(row.value)
                                    } catch (_: Exception) {
                                        // Malformed URL — ignore
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.textMuted,
                        modifier = Modifier.widthIn(min = InfoLabelWidth)
                    )
                    Text(
                        text = row.value,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = if (index == 0) {
                                MaterialTheme.typography.labelLarge.fontFamily
                            } else {
                                MaterialTheme.typography.bodyLarge.fontFamily
                            }
                        ),
                        color = when {
                            isLink -> MaterialTheme.colorScheme.primary
                            row.value == "—" -> MaterialTheme.colorScheme.textFaint
                            else -> MaterialTheme.colorScheme.onSurface
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
    val textContent = resolveRecentDocumentText(
        document = document,
        unknownLabel = stringResource(Res.string.common_unknown)
    )
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
            text = formatMonthDay(document.issueDate.month.number, document.issueDate.day),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = MaterialTheme.typography.labelLarge.fontFamily
            ),
            color = MaterialTheme.colorScheme.textMuted,
            modifier = Modifier.width(52.dp)
        )
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = textContent.primary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            textContent.secondary?.let { secondary ->
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily
                    ),
                    color = MaterialTheme.colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
    val month = Month(monthNumber).name
        .take(3)
        .lowercase()
        .replaceFirstChar { it.uppercase() }
    return "$month $day"
}

internal fun resolveRecentDocumentText(
    document: ContactRecentInvoice,
    unknownLabel: String,
): RecentDocumentText {
    val summary = document.summary?.takeIf { it.isNotBlank() }
    val reference = document.reference?.takeIf { it.isNotBlank() } ?: unknownLabel
    val secondary = summary?.let {
        reference.takeIf { resolvedReference ->
            !resolvedReference.equals(it, ignoreCase = true)
        }
    }

    return RecentDocumentText(
        primary = summary ?: reference,
        secondary = secondary
    )
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

@Immutable
internal data class RecentDocumentText(
    val primary: String,
    val secondary: String?,
)
