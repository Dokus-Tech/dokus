package tech.dokus.features.cashflow.presentation.ledger.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.todayIn
import tech.dokus.domain.Money
import tech.dokus.domain.enums.CashflowDirection
import tech.dokus.domain.enums.CashflowEntryStatus
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.model.CashflowEntry
import tech.dokus.features.cashflow.presentation.common.utils.formatShortDate
import tech.dokus.features.cashflow.presentation.ledger.mvi.PaymentFormState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.layout.DokusExpandableAction

// UI dimension constants
private val FormContentPadding = 16.dp
private val FormContentSpacing = 16.dp
private val HeaderPaddingStart = 16.dp
private val HeaderPaddingEnd = 8.dp
private val HeaderPaddingTop = 16.dp
private val HeaderPaddingBottom = 8.dp
private val PaneMinWidth = 400.dp
private val PaneMaxWidth = 600.dp
private const val AnimationDurationMs = 200
private const val SlideAnimationDurationMs = 300
private const val ScrimAlpha = 0.32f
private const val PaneWidthFraction = 0.4f

/**
 * Detail pane for displaying and interacting with a cashflow entry.
 *
 * On desktop: slides in from the right with semi-transparent backdrop.
 * On mobile: full-screen overlay.
 *
 * @param isVisible Whether the pane is currently visible
 * @param entry The cashflow entry to display (null if not selected)
 * @param paymentFormState Current state of the payment form
 * @param isFullScreen Whether to display as full-screen (mobile)
 * @param onDismiss Callback when the pane should be dismissed
 * @param onPaymentDateChange Callback when payment date changes
 * @param onPaymentAmountTextChange Callback when payment amount text changes
 * @param onPaymentNoteChange Callback when payment note changes
 * @param onSubmitPayment Callback when payment form is submitted
 * @param onTogglePaymentOptions Callback to toggle options panel
 * @param onQuickMarkAsPaid Callback for one-click mark as paid
 * @param onCancelPaymentOptions Callback to cancel options and reset
 * @param onOpenDocument Callback when source document is clicked
 */
@Composable
internal fun CashflowDetailPane(
    isVisible: Boolean,
    entry: CashflowEntry?,
    paymentFormState: PaymentFormState,
    isFullScreen: Boolean,
    onDismiss: () -> Unit,
    onPaymentDateChange: (LocalDate) -> Unit,
    onPaymentAmountTextChange: (String) -> Unit,
    onPaymentNoteChange: (String) -> Unit,
    onSubmitPayment: () -> Unit,
    onTogglePaymentOptions: () -> Unit,
    onQuickMarkAsPaid: () -> Unit,
    onCancelPaymentOptions: () -> Unit,
    onOpenDocument: (DocumentId) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .onKeyEvent { event ->
                if (event.key == Key.Escape && isVisible) {
                    onDismiss()
                    true
                } else {
                    false
                }
            }
    ) {
        // Backdrop
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn(tween(AnimationDurationMs)),
            exit = fadeOut(tween(AnimationDurationMs))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = ScrimAlpha))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        // Side pane
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInHorizontally(
                initialOffsetX = { it },
                animationSpec = tween(SlideAnimationDurationMs)
            ) + fadeIn(tween(SlideAnimationDurationMs)),
            exit = slideOutHorizontally(
                targetOffsetX = { it },
                animationSpec = tween(SlideAnimationDurationMs)
            ) + fadeOut(tween(SlideAnimationDurationMs)),
            modifier = Modifier.align(Alignment.CenterEnd)
        ) {
            BoxWithConstraints {
                val paneWidth = if (isFullScreen) {
                    maxWidth
                } else {
                    (maxWidth * PaneWidthFraction).coerceIn(PaneMinWidth, PaneMaxWidth)
                }

                DokusCardSurface(
                    modifier = Modifier
                        .width(paneWidth)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Consume click to prevent backdrop dismissal */ }
                        ),
                    shape = if (isFullScreen) {
                        MaterialTheme.shapes.extraSmall
                    } else {
                        MaterialTheme.shapes.medium.copy(
                            topEnd = MaterialTheme.shapes.extraSmall.topEnd,
                            bottomEnd = MaterialTheme.shapes.extraSmall.bottomEnd
                        )
                    },
                ) {
                    if (entry != null) {
                        CashflowDetailContent(
                            entry = entry,
                            paymentFormState = paymentFormState,
                            onDismiss = onDismiss,
                            onPaymentDateChange = onPaymentDateChange,
                            onPaymentAmountTextChange = onPaymentAmountTextChange,
                            onPaymentNoteChange = onPaymentNoteChange,
                            onSubmitPayment = onSubmitPayment,
                            onTogglePaymentOptions = onTogglePaymentOptions,
                            onQuickMarkAsPaid = onQuickMarkAsPaid,
                            onCancelPaymentOptions = onCancelPaymentOptions,
                            onOpenDocument = onOpenDocument
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CashflowDetailContent(
    entry: CashflowEntry,
    paymentFormState: PaymentFormState,
    onDismiss: () -> Unit,
    onPaymentDateChange: (LocalDate) -> Unit,
    onPaymentAmountTextChange: (String) -> Unit,
    onPaymentNoteChange: (String) -> Unit,
    onSubmitPayment: () -> Unit,
    onTogglePaymentOptions: () -> Unit,
    onQuickMarkAsPaid: () -> Unit,
    onCancelPaymentOptions: () -> Unit,
    onOpenDocument: (DocumentId) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        CashflowDetailHeader(
            title = "Cashflow item",
            onClose = onDismiss
        )

        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = FormContentPadding),
            verticalArrangement = Arrangement.spacedBy(FormContentSpacing)
        ) {
            // Status banner
            CashflowStatusBanner(entry = entry)

            // Amount section
            CashflowAmountSection(entry = entry)

            HorizontalDivider()

            // Contact section
            CashflowContactSection(contactName = entry.contactName)

            // Details section
            CashflowDetailsSection(entry = entry)

            HorizontalDivider()

            // Breakdown section (always shown)
            CashflowBreakdownSection(entry = entry)

            // Source document card (conditional)
            entry.documentId?.let { docId ->
                CashflowSourceDocumentCard(
                    onClick = { onOpenDocument(docId) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }

        // Payment footer (if status != Paid)
        if (entry.status != CashflowEntryStatus.Paid) {
            HorizontalDivider()
            CashflowPaymentFooter(
                entry = entry,
                formState = paymentFormState,
                onToggleOptions = onTogglePaymentOptions,
                onQuickSubmit = onQuickMarkAsPaid,
                onDateChange = onPaymentDateChange,
                onAmountTextChange = onPaymentAmountTextChange,
                onNoteChange = onPaymentNoteChange,
                onSubmit = onSubmitPayment,
                onCancel = onCancelPaymentOptions,
                modifier = Modifier.padding(FormContentPadding)
            )
        }
    }
}

@Composable
private fun CashflowDetailHeader(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = HeaderPaddingStart,
                end = HeaderPaddingEnd,
                top = HeaderPaddingTop,
                bottom = HeaderPaddingBottom
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )

        IconButton(onClick = onClose) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CashflowStatusBanner(
    entry: CashflowEntry,
    modifier: Modifier = Modifier
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val daysUntilDue = today.daysUntil(entry.eventDate)
    val isPartiallyPaid = entry.status == CashflowEntryStatus.Open &&
        entry.remainingAmount < entry.amountGross

    val (backgroundColor, textColor, statusText) = when {
        entry.status == CashflowEntryStatus.Paid -> Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "Paid"
        )
        entry.status == CashflowEntryStatus.Cancelled -> Triple(
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
            "Cancelled"
        )
        entry.status == CashflowEntryStatus.Overdue -> Triple(
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            "Overdue - ${-daysUntilDue} days overdue"
        )
        isPartiallyPaid -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            "Partially paid - ${entry.currency.displaySign}${entry.remainingAmount.toDisplayString()} remaining"
        )
        else -> Triple(
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            if (daysUntilDue >= 0) "Open - Due in $daysUntilDue days" else "Open - Due"
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = backgroundColor,
                shape = MaterialTheme.shapes.small
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CashflowAmountSection(
    entry: CashflowEntry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = if (entry.direction == CashflowDirection.Out) "Payable" else "Receivable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "${entry.currency.displaySign}${entry.amountGross.toDisplayString()}",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Light
        )
    }
}

@Composable
private fun CashflowContactSection(
    contactName: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "Contact",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = contactName ?: "Unknown",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CashflowDetailsSection(
    entry: CashflowEntry,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Due Date
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "Due Date",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = formatShortDate(entry.eventDate),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Description
        entry.description?.let { description ->
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CashflowBreakdownSection(
    entry: CashflowEntry,
    modifier: Modifier = Modifier
) {
    val netAmount = Money(entry.amountGross.minor - entry.amountVat.minor)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Breakdown",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        BreakdownRow(
            label = "Net",
            value = "${entry.currency.displaySign}${netAmount.toDisplayString()}"
        )
        BreakdownRow(
            label = "VAT",
            value = "${entry.currency.displaySign}${entry.amountVat.toDisplayString()}"
        )
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
        BreakdownRow(
            label = "Total",
            value = "${entry.currency.displaySign}${entry.amountGross.toDisplayString()}",
            isBold = true
        )
    }
}

@Composable
private fun BreakdownRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (isBold) FontWeight.Medium else FontWeight.Normal
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isBold) FontWeight.Medium else FontWeight.Normal
        )
    }
}

@Composable
private fun CashflowSourceDocumentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Source Document",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedButton(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Description,
                contentDescription = null,
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("View document")
        }
    }
}

@Composable
private fun CashflowPaymentFooter(
    entry: CashflowEntry,
    formState: PaymentFormState,
    onToggleOptions: () -> Unit,
    onQuickSubmit: () -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onAmountTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val defaultSubtext = "${entry.remainingAmount.toDisplayString()} today"

    DokusExpandableAction(
        isExpanded = formState.isOptionsExpanded,
        onToggleExpand = onToggleOptions,
        modifier = modifier,
        subtext = {
            Text(
                text = defaultSubtext,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        primaryAction = {
            Button(
                onClick = { if (formState.isOptionsExpanded) onSubmit() else onQuickSubmit() },
                enabled = !formState.isSubmitting
            ) {
                if (formState.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .height(16.dp)
                            .width(16.dp),
                        strokeWidth = 2.dp
                    )
                }
                Text(if (formState.isOptionsExpanded) "Confirm payment" else "Mark as paid")
            }
        },
        expandedContent = {
            PaymentOptionsForm(
                formState = formState,
                maxAmount = entry.remainingAmount,
                onDateChange = onDateChange,
                onAmountTextChange = onAmountTextChange,
                onNoteChange = onNoteChange,
                onCancel = onCancel
            )
        }
    )
}

@Composable
private fun PaymentOptionsForm(
    formState: PaymentFormState,
    maxAmount: Money,
    onDateChange: (LocalDate) -> Unit,
    onAmountTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // TODO: Add date picker when available
        // For now, showing paidAt as read-only
        Text(
            text = "Date: ${formatShortDate(formState.paidAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Amount field - uses amountText, not parsed Money
        OutlinedTextField(
            value = formState.amountText,
            onValueChange = onAmountTextChange,
            label = { Text("Amount") },
            supportingText = {
                if (formState.amountError != null) {
                    Text(formState.amountError)
                } else {
                    Text("Max: ${maxAmount.toDisplayString()}")
                }
            },
            isError = formState.amountError != null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Note field
        OutlinedTextField(
            value = formState.note,
            onValueChange = onNoteChange,
            label = { Text("Note (optional)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        // Cancel button
        TextButton(onClick = onCancel) {
            Text("Cancel")
        }
    }
}
