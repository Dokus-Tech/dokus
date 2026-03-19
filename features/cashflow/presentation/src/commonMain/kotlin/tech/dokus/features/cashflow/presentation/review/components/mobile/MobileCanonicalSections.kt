package tech.dokus.features.cashflow.presentation.review.components.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Lucide
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_needed_to_complete
import tech.dokus.aura.resources.document_payment_awaiting
import tech.dokus.aura.resources.mobile_confirm_document
import tech.dokus.aura.resources.mobile_label_due
import tech.dokus.aura.resources.mobile_label_invoice
import tech.dokus.aura.resources.mobile_label_issued
import tech.dokus.aura.resources.mobile_unknown_vendor
import tech.dokus.aura.resources.payment_auto_paid
import tech.dokus.aura.resources.payment_confidence
import tech.dokus.aura.resources.payment_loading_automation
import tech.dokus.aura.resources.payment_record_title
import tech.dokus.aura.resources.payment_undo_auto
import tech.dokus.aura.resources.payment_undoing
import tech.dokus.domain.enums.AutoMatchStatus
import tech.dokus.domain.model.AutoPaymentStatus
import tech.dokus.features.cashflow.presentation.review.models.DocumentUiData
import tech.dokus.features.cashflow.presentation.review.DocumentReviewIntent
import tech.dokus.features.cashflow.presentation.review.DocumentReviewState
import tech.dokus.features.cashflow.presentation.review.ReviewFinancialStatus
import tech.dokus.features.cashflow.presentation.review.dotType
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.features.cashflow.presentation.review.overdueInlineLocalized
import tech.dokus.features.cashflow.presentation.review.paidHeadlineLocalized
import tech.dokus.features.cashflow.presentation.review.paidMethodLocalized
import tech.dokus.features.cashflow.presentation.review.paymentDueLocalized
import tech.dokus.features.cashflow.presentation.review.statusBadgeLocalized
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.PButton
import tech.dokus.foundation.aura.components.PButtonVariant
import tech.dokus.foundation.aura.components.PIcon
import tech.dokus.foundation.aura.components.status.StatusDot
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.aura.style.amberWhisper
import tech.dokus.foundation.aura.style.greenSoft
import tech.dokus.foundation.aura.style.redSoft
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.features.cashflow.presentation.review.colorized as financialStatusColorized

private val HeroCardCorner = RoundedCornerShape(14.dp)
private val HeroAccentWidth = 3.5.dp

@Composable
internal fun MobileCanonicalHeader(
    state: DocumentReviewState,
) {
    val contactName = when (val c = state.effectiveContact) {
        is ResolvedContact.Linked -> c.name
        is ResolvedContact.Suggested -> c.name
        is ResolvedContact.Detected -> c.name
        is ResolvedContact.Unknown -> null
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall),
    ) {
        Text(
            text = contactName ?: stringResource(Res.string.mobile_unknown_vendor),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
internal fun MobileAmountHeroCard(
    state: DocumentReviewState,
    uiData: DocumentUiData,
) {
    // Only financial types with amounts get the hero card
    when (uiData) {
        is DocumentUiData.Invoice,
        is DocumentUiData.CreditNote,
        is DocumentUiData.Receipt -> { /* continue */ }
        is DocumentUiData.BankStatement,
        is DocumentUiData.ProForma,
        is DocumentUiData.Quote,
        is DocumentUiData.OrderConfirmation,
        is DocumentUiData.DeliveryNote,
        is DocumentUiData.Reminder,
        is DocumentUiData.StatementOfAccount,
        is DocumentUiData.PurchaseOrder,
        is DocumentUiData.ExpenseClaim,
        is DocumentUiData.BankFee,
        is DocumentUiData.InterestStatement,
        is DocumentUiData.PaymentConfirmation,
        is DocumentUiData.VatReturn,
        is DocumentUiData.VatListing,
        is DocumentUiData.VatAssessment,
        is DocumentUiData.IcListing,
        is DocumentUiData.OssReturn,
        is DocumentUiData.CorporateTax,
        is DocumentUiData.CorporateTaxAdvance,
        is DocumentUiData.TaxAssessment,
        is DocumentUiData.PersonalTax,
        is DocumentUiData.WithholdingTax,
        is DocumentUiData.SocialContribution,
        is DocumentUiData.SocialFund,
        is DocumentUiData.SelfEmployedContribution,
        is DocumentUiData.Vapz,
        is DocumentUiData.SalarySlip,
        is DocumentUiData.PayrollSummary,
        is DocumentUiData.EmploymentContract,
        is DocumentUiData.Dimona,
        is DocumentUiData.C4,
        is DocumentUiData.HolidayPay,
        is DocumentUiData.Contract,
        is DocumentUiData.Lease,
        is DocumentUiData.Loan,
        is DocumentUiData.Insurance,
        is DocumentUiData.Dividend,
        is DocumentUiData.ShareholderRegister,
        is DocumentUiData.CompanyExtract,
        is DocumentUiData.AnnualAccounts,
        is DocumentUiData.BoardMinutes,
        is DocumentUiData.Subsidy,
        is DocumentUiData.Fine,
        is DocumentUiData.Permit,
        is DocumentUiData.CustomsDeclaration,
        is DocumentUiData.Intrastat,
        is DocumentUiData.DepreciationSchedule,
        is DocumentUiData.Inventory,
        is DocumentUiData.Other -> return
    }

    val currencySign = when (uiData) {
        is DocumentUiData.Invoice -> uiData.currencySign
        is DocumentUiData.CreditNote -> uiData.currencySign
        is DocumentUiData.Receipt -> uiData.currencySign
    }
    val primaryDescription = when (uiData) {
        is DocumentUiData.Invoice -> uiData.primaryDescription
        is DocumentUiData.CreditNote -> uiData.primaryDescription
        is DocumentUiData.Receipt -> uiData.primaryDescription
    }
    val amount = state.totalAmount?.toDisplayString() ?: "\u2014"

    DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(HeroAccentWidth)
                    .background(
                        color = state.financialStatus.financialStatusColorized.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(
                            topStart = 14.dp,
                            bottomStart = 14.dp,
                        ),
                    )
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(Constraints.Spacing.medium),
                verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Text(
                    text = "$currencySign$amount",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = primaryDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.textMuted,
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium),
                ) {
                    when (uiData) {
                        is DocumentUiData.Invoice -> {
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_issued), value = uiData.issueDate ?: "\u2014")
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_due), value = uiData.dueDate ?: "\u2014")
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_invoice), value = uiData.invoiceNumber ?: "\u2014")
                        }
                        is DocumentUiData.CreditNote -> {
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_issued), value = uiData.issueDate ?: "\u2014")
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_invoice), value = uiData.creditNoteNumber ?: "\u2014")
                        }
                        is DocumentUiData.Receipt -> {
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_issued), value = uiData.date ?: "\u2014")
                            MobileMetaCell(label = stringResource(Res.string.mobile_label_invoice), value = uiData.receiptNumber ?: "\u2014")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun MobilePaymentStateCard(
    state: DocumentReviewState,
    isAccountantReadOnly: Boolean,
    onIntent: (DocumentReviewIntent) -> Unit,
) {
    val autoPaymentStatus = (state.autoPaymentStatus as? DokusState.Success<*>)?.data as? AutoPaymentStatus
    val (title, subtitle) = when (state.financialStatus) {
        ReviewFinancialStatus.Paid -> {
            state.paidHeadlineLocalized to state.paidMethodLocalized
        }

        ReviewFinancialStatus.Overdue -> {
            val line = listOfNotNull(state.paymentDueLocalized, state.overdueInlineLocalized)
                .joinToString(" \u00b7 ")
            state.statusBadgeLocalized to line
        }

        ReviewFinancialStatus.Unpaid -> {
            val dueText = state.paymentDueLocalized
            stringResource(Res.string.document_payment_awaiting) to
                (dueText ?: stringResource(Res.string.cashflow_needed_to_complete))
        }

        ReviewFinancialStatus.Review -> {
            state.statusBadgeLocalized to stringResource(Res.string.cashflow_needed_to_complete)
        }
    }

    val backgroundColor = when (state.financialStatus) {
        ReviewFinancialStatus.Paid -> MaterialTheme.colorScheme.greenSoft.copy(alpha = 0.35f)
        ReviewFinancialStatus.Overdue -> MaterialTheme.colorScheme.redSoft.copy(alpha = 0.33f)
        ReviewFinancialStatus.Unpaid -> MaterialTheme.colorScheme.surface
        ReviewFinancialStatus.Review -> MaterialTheme.colorScheme.amberWhisper
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, HeroCardCorner),
        color = backgroundColor,
        shape = HeroCardCorner,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                        )
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (state.financialStatus == ReviewFinancialStatus.Paid) {
                        PIcon(
                            icon = Lucide.Check,
                            description = null,
                            tint = state.financialStatus.financialStatusColorized,
                        )
                    } else {
                        StatusDot(type = state.financialStatus.dotType, size = 8.dp)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = state.financialStatus.financialStatusColorized,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }

            when (state.financialStatus) {
                ReviewFinancialStatus.Review -> {
                    if (!isAccountantReadOnly) {
                        PButton(
                            text = stringResource(Res.string.mobile_confirm_document),
                            isEnabled = state.canConfirm,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onIntent(DocumentReviewIntent.Confirm) },
                        )
                    }
                }

                ReviewFinancialStatus.Unpaid,
                ReviewFinancialStatus.Overdue -> {
                    if (!isAccountantReadOnly && state.canRecordPayment) {
                        PButton(
                            text = stringResource(Res.string.payment_record_title),
                            variant = PButtonVariant.Outline,
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onIntent(DocumentReviewIntent.OpenPaymentSheet) },
                        )
                    }
                }

                ReviewFinancialStatus.Paid -> Unit
            }

            if (state.financialStatus == ReviewFinancialStatus.Paid) {
                when (state.autoPaymentStatus) {
                    is DokusState.Loading -> {
                        Text(
                            text = stringResource(Res.string.payment_loading_automation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.textMuted,
                        )
                    }

                    is DokusState.Success -> {
                        if (autoPaymentStatus is AutoPaymentStatus.AutoPaid) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Text(
                                text = stringResource(Res.string.payment_auto_paid),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            val confidenceText = autoPaymentStatus.confidenceScore
                                ?.let { score -> "${(score * 100).toInt()}%" }
                                ?: "\u2014"
                            Text(
                                text = stringResource(Res.string.payment_confidence, confidenceText),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.textMuted,
                            )
                            if (autoPaymentStatus.reasons.isNotEmpty()) {
                                Text(
                                    text = autoPaymentStatus.reasons.joinToString(", ") { formatMatchReason(it) },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.textMuted,
                                )
                            }
                            if (autoPaymentStatus.canUndo) {
                                PButton(
                                    text = if (state.isUndoingAutoPayment) stringResource(Res.string.payment_undoing) else stringResource(Res.string.payment_undo_auto),
                                    variant = PButtonVariant.OutlineMuted,
                                    isEnabled = !state.isUndoingAutoPayment,
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onIntent(DocumentReviewIntent.UndoAutoPayment()) },
                                )
                            }
                        }
                    }

                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun MobileMetaCell(label: String, value: String) {
    Column(
        modifier = Modifier.width(94.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.textMuted,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun formatMatchReason(reason: String): String = when (reason) {
    "structured_reference_match" -> "Structured reference match"
    "exact_amount" -> "Exact amount"
    "date_proximity" -> "Date proximity"
    "iban_match" -> "IBAN match"
    "name_similarity" -> "Name similarity"
    "vat_match" -> "VAT number match"
    "invoice_number_match" -> "Invoice number match"
    else -> reason.replace('_', ' ').replaceFirstChar { it.uppercase() }
}
