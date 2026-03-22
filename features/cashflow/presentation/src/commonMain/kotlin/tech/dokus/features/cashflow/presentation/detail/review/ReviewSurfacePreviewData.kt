@file:Suppress("LongMethod", "MagicNumber")

package tech.dokus.features.cashflow.presentation.detail.review

import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.enums.DocumentStatus
import tech.dokus.domain.model.contact.ContactSuggestionDto
import tech.dokus.domain.model.contact.ResolvedContact
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.cashflow.presentation.detail.DocumentDetailState
import tech.dokus.features.cashflow.presentation.detail.components.previewReviewContentState

/**
 * Preview helpers for the review surface composables.
 *
 * All dates are fixed for deterministic Roborazzi snapshots.
 */

// === State factories ===

/** Clean document — no issues, ready for one-click confirm. */
internal fun previewCleanReviewState(): DocumentDetailState =
    previewReviewContentState(
        documentStatus = DocumentStatus.NeedsReview,
        entryStatus = null,
    )

/** Document with a suggested contact — contact issue active. */
internal fun previewContactSuggestedState(): DocumentDetailState {
    val base = previewReviewContentState(
        documentStatus = DocumentStatus.NeedsReview,
        entryStatus = null,
    )
    // The default state has Detected contact (not Linked), so it will produce a contact issue
    return base
}

/** Document with unknown contact — no suggestions. */
internal fun previewContactUnknownState(): DocumentDetailState {
    val base = previewReviewContentState(
        documentStatus = DocumentStatus.NeedsReview,
        entryStatus = null,
    )
    // Override the resolved contact to Unknown by clearing the contact
    return base.copy(
        selectedContactOverride = null,
    )
}

// === Issue card data ===

internal val previewContactIssueSuggested = ReviewIssue.ContactIssue(
    contact = ResolvedContact.Suggested(
        contactId = ContactId.parse("00000000-0000-0000-0000-000000000002"),
        name = "Sky Hotel Bruges",
        vatNumber = "BE0456789123",
    ),
    suggestions = listOf(
        ContactSuggestionDto(
            contactId = ContactId.parse("00000000-0000-0000-0000-000000000002"),
            name = "Sky Hotel Bruges",
            vatNumber = "BE0456789123",
        ),
    ),
)

internal val previewContactIssueUnknown = ReviewIssue.ContactIssue(
    contact = ResolvedContact.Unknown,
    suggestions = emptyList(),
)

internal val previewContactIssueDetected = ReviewIssue.ContactIssue(
    contact = ResolvedContact.Detected(
        name = "Sodexo Belgium NV",
        vatNumber = "BE0403273047",
        iban = null,
        address = null,
    ),
    suggestions = emptyList(),
)

internal val previewAmountIssue = ReviewIssue.AmountIssue(
    missingTotal = true,
    missingSubtotal = false,
    missingVat = true,
)

internal val previewDateIssueDueBefore = ReviewIssue.DateIssue(
    missingIssueDate = false,
    missingDueDate = false,
    dueDateBeforeIssueDate = true,
    dueDateFarOut = false,
    issueDate = kotlinx.datetime.LocalDate(2026, 3, 18),
    dueDate = kotlinx.datetime.LocalDate(2025, 3, 18),
)

internal val previewDateIssueFarOut = ReviewIssue.DateIssue(
    missingIssueDate = false,
    missingDueDate = false,
    dueDateBeforeIssueDate = false,
    dueDateFarOut = true,
    issueDate = kotlinx.datetime.LocalDate(2026, 3, 18),
    dueDate = kotlinx.datetime.LocalDate(2027, 3, 18),
)

internal val previewDateIssueMissing = ReviewIssue.DateIssue(
    missingIssueDate = false,
    missingDueDate = true,
    dueDateBeforeIssueDate = false,
    dueDateFarOut = false,
    issueDate = kotlinx.datetime.LocalDate(2026, 3, 18),
    dueDate = null,
)

internal val previewDirectionIssue = ReviewIssue.DirectionIssue(
    currentDirection = DocumentDirection.Unknown,
)
