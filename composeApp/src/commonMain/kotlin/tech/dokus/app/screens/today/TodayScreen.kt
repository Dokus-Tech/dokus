package tech.dokus.app.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.app.navigation.local.LocalHomeNavController
import tech.dokus.app.viewmodel.TodayAction
import tech.dokus.app.viewmodel.TodayContainer
import tech.dokus.app.viewmodel.TodayIntent
import tech.dokus.app.viewmodel.TodayState
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.home_today
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData
import tech.dokus.foundation.app.mvi.container
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.badges.SourceBadge
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.components.status.StatusDotType
import tech.dokus.foundation.aura.components.text.Amt
import tech.dokus.foundation.aura.components.text.DokusLabel
import tech.dokus.foundation.aura.components.text.MobilePageTitle
import tech.dokus.foundation.aura.components.text.SectionTitle
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textFaint
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.navigation.destinations.AuthDestination
import tech.dokus.navigation.destinations.CashFlowDestination
import tech.dokus.navigation.destinations.HomeDestination
import tech.dokus.navigation.local.LocalNavController
import tech.dokus.navigation.navigateTo
import tech.dokus.foundation.aura.components.badges.DocumentSource as UiDocumentSource

@Composable
internal fun TodayScreen(
    container: TodayContainer = container()
) {
    val navController = LocalNavController.current
    val homeNavController = LocalHomeNavController.current
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val isLargeScreen = LocalScreenSize.current.isLarge

    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    val state by container.store.subscribe { action ->
        when (action) {
            is TodayAction.NavigateToDocument -> {
                navController.navigateTo(CashFlowDestination.DocumentReview(action.documentId))
            }

            TodayAction.NavigateToCashflow -> {
                navController.navigateTo(CashFlowDestination.CashflowLedger())
            }

            TodayAction.NavigateToWorkspaceSelect -> {
                navController.navigateTo(AuthDestination.WorkspaceSelect)
            }

            is TodayAction.ShowError -> {
                pendingError = action.error
            }
        }
    }

    LaunchedEffect(Unit) {
        container.store.intent(TodayIntent.RefreshTenant)
    }

    val contentState = state as? TodayState.Content
    val documents = contentState?.allPendingDocuments ?: emptyList()
    val spacing = if (isLargeScreen) Constrains.Spacing.xLarge else Constrains.Spacing.large

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(spacing),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            MobilePageTitle(title = stringResource(Res.string.home_today))

            // Stat cards (2-column grid)
            TodayStatCards(documents = documents)

            // Attention card (first pending document)
            val attentionDoc = documents.firstOrNull()
            if (attentionDoc != null) {
                TodayAttentionCard(
                    document = attentionDoc,
                    onReviewClick = {
                        navController.navigateTo(
                            CashFlowDestination.DocumentReview(attentionDoc.document.id.toString())
                        )
                    }
                )
            }

            // Recent documents section
            if (documents.isNotEmpty()) {
                TodayRecentSection(
                    documents = documents.take(5),
                    onDocumentClick = { doc ->
                        navController.navigateTo(
                            CashFlowDestination.DocumentReview(doc.document.id.toString())
                        )
                    },
                    onViewAllClick = {
                        homeNavController?.navigateTo(HomeDestination.Documents)
                    }
                )
            }
        }
    }
}

// ── Stat Cards ──────────────────────────────────────────────────────────

@Composable
private fun TodayStatCards(documents: List<DocumentRecordDto>) {
    val totalAmount = remember(documents) {
        documents.sumOf { it.extractedTotalMinor() }
    }
    val count = documents.size

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Overdue card (accent)
        DokusCardSurface(
            modifier = Modifier.weight(1f),
            accent = true,
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DokusLabel(
                    text = "Needs review",
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = formatStatAmount(totalAmount),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        letterSpacing = (-0.04).em,
                        lineHeight = 28.sp,
                    ),
                    color = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "$count ${if (count == 1) "invoice" else "invoices"} pending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }

        // Due this week card
        DokusCardSurface(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(20.dp)) {
                DokusLabel(
                    text = "Documents",
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                        letterSpacing = (-0.04).em,
                        lineHeight = 28.sp,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "awaiting processing",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
    }
}

// ── Attention Card ──────────────────────────────────────────────────────

@Composable
private fun TodayAttentionCard(
    document: DocumentRecordDto,
    onReviewClick: () -> Unit,
) {
    val vendorName = document.vendorName()
    val dateText = document.formattedDate()
    val description = document.draft?.aiDescription ?: "Document needs review"

    DokusCardSurface(accent = true) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            StatusDot(
                type = StatusDotType.Warning,
                size = 8.dp,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = vendorName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                    maxLines = 1,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (dateText != null) {
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                            ),
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                        Text(
                            text = "\u00b7",
                            color = MaterialTheme.colorScheme.textFaint,
                        )
                    }
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted,
                        maxLines = 1,
                    )
                }
            }
            Button(
                onClick = onReviewClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = MaterialTheme.shapes.small,
            ) {
                Text(
                    text = "Review",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

// ── Recent Documents Section ────────────────────────────────────────────

@Composable
private fun TodayRecentSection(
    documents: List<DocumentRecordDto>,
    onDocumentClick: (DocumentRecordDto) -> Unit,
    onViewAllClick: () -> Unit,
) {
    Column {
        SectionTitle(
            text = "Recent",
            right = {
                Text(
                    text = "View all \u2192",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted,
                    modifier = Modifier.clickable(onClick = onViewAllClick),
                )
            },
        )
        Spacer(Modifier.height(8.dp))
        DokusCardSurface {
            Column {
                documents.forEachIndexed { index, doc ->
                    TodayRecentRow(
                        document = doc,
                        onClick = { onDocumentClick(doc) },
                    )
                    if (index < documents.lastIndex) {
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayRecentRow(
    document: DocumentRecordDto,
    onClick: () -> Unit,
) {
    val source = document.document.source.toUiSource()
    val vendorName = document.vendorName()
    val dateText = document.formattedDate()
    val amount = document.extractedTotalDouble()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SourceBadge(source = source)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = vendorName,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium,
                ),
                maxLines = 1,
            )
            if (dateText != null) {
                Text(
                    text = dateText,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = MaterialTheme.typography.labelLarge.fontFamily,
                    ),
                    color = MaterialTheme.colorScheme.textMuted,
                )
            }
        }
        if (amount != null) {
            Amt(value = amount, size = 12.sp)
        }
    }
}

// ── Helpers ─────────────────────────────────────────────────────────────

private fun DocumentRecordDto.vendorName(): String {
    val snapshot = draft?.counterpartySnapshot?.name
    if (!snapshot.isNullOrBlank()) return snapshot
    return when (val data = draft?.extractedData) {
        is InvoiceDraftData -> data.seller.name ?: data.buyer.name ?: document.filename
        is CreditNoteDraftData -> data.counterpartyName ?: document.filename
        is ReceiptDraftData -> data.merchantName ?: document.filename
        null -> document.filename
    }
}

private fun DocumentRecordDto.formattedDate(): String? {
    val date = when (val data = draft?.extractedData) {
        is InvoiceDraftData -> data.issueDate
        is CreditNoteDraftData -> data.issueDate
        is ReceiptDraftData -> data.date
        null -> null
    } ?: return null
    val months = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    return "${months[date.month.ordinal]} ${date.day}"
}

private fun DocumentRecordDto.extractedTotalMinor(): Long {
    return when (val data = draft?.extractedData) {
        is InvoiceDraftData -> data.totalAmount?.minor ?: 0L
        is CreditNoteDraftData -> data.totalAmount?.minor ?: 0L
        is ReceiptDraftData -> data.totalAmount?.minor ?: 0L
        null -> 0L
    }
}

private fun DocumentRecordDto.extractedTotalDouble(): Double? {
    return when (val data = draft?.extractedData) {
        is InvoiceDraftData -> data.totalAmount?.toDouble()
        is CreditNoteDraftData -> data.totalAmount?.toDouble()
        is ReceiptDraftData -> data.totalAmount?.toDouble()
        null -> null
    }
}

private fun tech.dokus.domain.enums.DocumentSource.toUiSource(): UiDocumentSource {
    return when (this) {
        tech.dokus.domain.enums.DocumentSource.Peppol -> UiDocumentSource.Peppol
        else -> UiDocumentSource.Pdf
    }
}

private fun formatStatAmount(totalMinor: Long): String {
    val negative = totalMinor < 0
    val abs = if (negative) -totalMinor else totalMinor
    val euros = abs / 100
    val cents = (abs % 100).toInt()
    val intStr = euros.toString()
    val withDots = buildString {
        var count = 0
        for (i in intStr.lastIndex downTo 0) {
            if (count > 0 && count % 3 == 0) append('.')
            append(intStr[i])
            count++
        }
    }.reversed()
    val decStr = cents.toString().padStart(2, '0')
    return buildString {
        if (negative) append('\u2212')
        append('\u20ac')
        append(withDots)
        append(',')
        append(decStr)
    }
}
