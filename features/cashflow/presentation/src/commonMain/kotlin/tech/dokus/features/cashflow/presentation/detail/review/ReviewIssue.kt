package tech.dokus.features.cashflow.presentation.detail.review

import androidx.compose.runtime.Immutable
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.DocDto
import tech.dokus.domain.model.contact.ContactSuggestionDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.model.hasKnownDirectionForConfirmation
import tech.dokus.domain.model.hasRequiredDates
import tech.dokus.domain.model.hasRequiredSubtotalForConfirmation
import tech.dokus.domain.model.hasRequiredTotalForConfirmation
import tech.dokus.domain.model.hasRequiredVatForConfirmation
import tech.dokus.domain.model.isContactRequired
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.features.cashflow.presentation.detail.ContactMatchStatus
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState

/**
 * Issues derived from [DocumentDetailState] for the review decision stream.
 *
 * Each issue represents something the user must resolve before confirming.
 * Issues are ordered by priority (lower number = higher priority).
 * The review surface renders only the first issue as "active" and dims the rest.
 */
@Immutable
sealed interface ReviewIssue {
    val priority: Int

    /**
     * Contact is not linked — user must accept a suggestion, search, or create.
     *
     * @param contact The current resolved contact state (Suggested, Detected, or Unknown)
     * @param suggestions Available contact suggestions from extraction
     */
    data class ContactIssue(
        val contact: ResolvedContact,
        val suggestions: List<ContactSuggestionDto>,
    ) : ReviewIssue {
        override val priority: Int = 0
    }

    /**
     * Document direction (Inbound/Outbound) could not be resolved.
     *
     * @param currentDirection The current direction (typically Unknown)
     */
    data class DirectionIssue(
        val currentDirection: DocumentDirection,
    ) : ReviewIssue {
        override val priority: Int = 1
    }

    /**
     * Required financial amounts are missing (total, VAT, subtotal).
     *
     * @param missingTotal Total amount is required but missing
     * @param missingSubtotal Subtotal is required but missing
     * @param missingVat VAT amount is required but missing
     */
    data class AmountIssue(
        val missingTotal: Boolean,
        val missingSubtotal: Boolean,
        val missingVat: Boolean,
    ) : ReviewIssue {
        override val priority: Int = 2
    }

    /**
     * Required dates are missing or suspicious.
     *
     * @param missingIssueDate Issue date is required but not extracted
     * @param missingDueDate Due date is missing on an invoice
     * @param dueDateBeforeIssueDate Due date is before issue date (anomaly)
     * @param dueDateFarOut Due date is >365 days after issue date (anomaly)
     * @param issueDate Extracted issue date for display (null if missing)
     * @param dueDate Extracted due date for display (null if missing)
     */
    data class DateIssue(
        val missingIssueDate: Boolean,
        val missingDueDate: Boolean,
        val dueDateBeforeIssueDate: Boolean,
        val dueDateFarOut: Boolean,
        val issueDate: LocalDate?,
        val dueDate: LocalDate?,
    ) : ReviewIssue {
        override val priority: Int = 3
    }
}

/**
 * Maximum allowed days between issue date and due date before flagging as suspicious.
 */
private const val MAX_DUE_DATE_DAYS = 365

/**
 * Derive the list of review issues from the current document detail state.
 *
 * Issues are ordered by priority and represent all blockers/warnings
 * that the user should resolve before confirming the document.
 *
 * Returns an empty list when the document is clean and ready for one-click confirm.
 */
fun DocumentDetailState.deriveReviewIssues(): List<ReviewIssue> {
    val content = draftData ?: return emptyList()
    return buildList {
        deriveContactIssue(content)?.let { add(it) }
        deriveDirectionIssue(content)?.let { add(it) }
        deriveAmountIssue(content)?.let { add(it) }
        deriveDateIssue(content)?.let { add(it) }
    }.sortedBy { it.priority }
}

private fun DocumentDetailState.deriveContactIssue(content: DocDto): ReviewIssue.ContactIssue? {
    if (!content.isContactRequired) return null
    return when (contactMatchStatus) {
        ContactMatchStatus.MissingButRequired,
        ContactMatchStatus.Uncertain -> ReviewIssue.ContactIssue(
            contact = effectiveContact,
            suggestions = contactSuggestions,
        )
        ContactMatchStatus.Matched,
        ContactMatchStatus.NotRequired -> null
    }
}

private fun deriveDirectionIssue(content: DocDto): ReviewIssue.DirectionIssue? {
    if (content.hasKnownDirectionForConfirmation) return null
    val direction = when (content) {
        is DocDto.Invoice -> content.direction
        is DocDto.CreditNote -> content.direction
        is DocDto.Receipt,
        is DocDto.BankStatement,
        is DocDto.ClassifiedDoc -> return null
    }
    return ReviewIssue.DirectionIssue(currentDirection = direction)
}

private fun deriveAmountIssue(content: DocDto): ReviewIssue.AmountIssue? {
    val missingTotal = !content.hasRequiredTotalForConfirmation
    val missingSubtotal = !content.hasRequiredSubtotalForConfirmation
    val missingVat = !content.hasRequiredVatForConfirmation
    if (!missingTotal && !missingSubtotal && !missingVat) return null
    return ReviewIssue.AmountIssue(
        missingTotal = missingTotal,
        missingSubtotal = missingSubtotal,
        missingVat = missingVat,
    )
}

private fun deriveDateIssue(content: DocDto): ReviewIssue.DateIssue? {
    val missingIssueDate = !content.hasRequiredDates

    val invoiceData = content as? DocDto.Invoice
    val issueDate = invoiceData?.issueDate
    val dueDate = invoiceData?.dueDate

    val missingDueDate = invoiceData != null && dueDate == null
    val dueDateBeforeIssueDate = issueDate != null && dueDate != null && dueDate < issueDate
    val dueDateFarOut = issueDate != null && dueDate != null &&
        issueDate.daysUntil(dueDate) > MAX_DUE_DATE_DAYS

    if (!missingIssueDate && !missingDueDate && !dueDateBeforeIssueDate && !dueDateFarOut) return null

    return ReviewIssue.DateIssue(
        missingIssueDate = missingIssueDate,
        missingDueDate = missingDueDate,
        dueDateBeforeIssueDate = dueDateBeforeIssueDate,
        dueDateFarOut = dueDateFarOut,
        issueDate = issueDate,
        dueDate = dueDate,
    )
}

/**
 * Whether the document is in review mode (should show ReviewSurface instead of inspector).
 *
 * True when: NeedsReview status, not a bank statement, and no pending match review.
 * Documents with pending match review show the comparison surface instead.
 */
val DocumentDetailState.isReviewMode: Boolean
    get() = documentStatus == DocumentStatus.NeedsReview &&
        !isDocumentConfirmed &&
        !isDocumentRejected &&
        !isDocumentUnsupported &&
        !shouldShowPendingMatchComparison &&
        draftData !is DocDto.BankStatement
