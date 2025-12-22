package ai.dokus.app.cashflow.viewmodel.model

import ai.dokus.foundation.domain.model.ContactDto
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

// ============================================================================
// DELIVERY METHOD
// ============================================================================

/**
 * Available delivery methods for sending invoices.
 */
enum class InvoiceDeliveryMethod {
    PDF_EXPORT,
    PEPPOL,
    EMAIL
}

// ============================================================================
// UI STATE
// ============================================================================

/**
 * UI state for the interactive invoice editor.
 * Separate from form data to keep concerns isolated.
 */
data class CreateInvoiceUiState(
    val expandedItemId: String? = null,
    val isClientPanelOpen: Boolean = false,
    val clientSearchQuery: String = "",
    val selectedDeliveryMethod: InvoiceDeliveryMethod = InvoiceDeliveryMethod.PDF_EXPORT,
    val isDatePickerOpen: DatePickerTarget? = null,
    val currentStep: InvoiceCreationStep = InvoiceCreationStep.EDIT_INVOICE
)

/**
 * Which date picker is currently open.
 */
enum class DatePickerTarget {
    ISSUE_DATE,
    DUE_DATE
}

/**
 * Steps in the invoice creation flow (for mobile).
 */
enum class InvoiceCreationStep {
    EDIT_INVOICE,
    SEND_OPTIONS
}

// ============================================================================
// FORM STATE
// ============================================================================

/**
 * State representing the invoice creation form data.
 */
data class CreateInvoiceFormState(
    val selectedClient: ContactDto? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val notes: String = "",
    val items: List<InvoiceLineItem> = listOf(InvoiceLineItem()),
    val isSaving: Boolean = false,
    val errors: Map<String, String> = emptyMap()
) {
    val subtotal: String
        get() {
            val total = items.sumOf { it.lineTotalDouble }
            return formatMoney(total)
        }

    val vatAmount: String
        get() {
            val total = items.sumOf { it.vatAmountDouble }
            return formatMoney(total)
        }

    val total: String
        get() {
            val subtotalVal = items.sumOf { it.lineTotalDouble }
            val vatVal = items.sumOf { it.vatAmountDouble }
            return formatMoney(subtotalVal + vatVal)
        }

    val isValid: Boolean
        get() = selectedClient != null && items.any { it.isValid }

    /**
     * Check if selected client is Belgian (for Peppol validation).
     */
    val isClientBelgian: Boolean
        get() = selectedClient?.country?.equals("BE", ignoreCase = true) == true ||
                selectedClient?.country?.equals("Belgium", ignoreCase = true) == true

    /**
     * Check if selected client has Peppol ID configured.
     */
    val clientHasPeppolId: Boolean
        get() = selectedClient?.peppolId != null

    /**
     * Show Peppol warning for Belgian clients without Peppol ID.
     */
    val showPeppolWarning: Boolean
        get() = isClientBelgian && !clientHasPeppolId && selectedClient != null

    companion object {
        /**
         * Create initial form state with today's date and default due date.
         */
        @OptIn(ExperimentalTime::class)
        fun createInitial(expandedItemId: (String) -> Unit): CreateInvoiceFormState {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val firstItem = InvoiceLineItem()
            expandedItemId(firstItem.id)
            return CreateInvoiceFormState(
                issueDate = today,
                dueDate = today.plus(30, DateTimeUnit.DAY),
                items = listOf(firstItem)
            )
        }
    }
}

/**
 * Represents a line item in the invoice form.
 */
data class InvoiceLineItem(
    val id: String = Random.nextLong().toString(),
    val description: String = "",
    val quantity: Double = 1.0,
    val unitPrice: String = "",  // Stored as string for form input
    val vatRatePercent: Int = 21  // 21%, 12%, 6%, 0%
) {
    val unitPriceDouble: Double
        get() = unitPrice.toDoubleOrNull() ?: 0.0

    val lineTotalDouble: Double
        get() = unitPriceDouble * quantity

    val vatRateDecimal: Double
        get() = vatRatePercent / 100.0

    val vatAmountDouble: Double
        get() = lineTotalDouble * vatRateDecimal

    val lineTotal: String
        get() = formatMoney(lineTotalDouble)

    val isValid: Boolean
        get() = description.isNotBlank() && quantity > 0 && unitPriceDouble > 0

    val isEmpty: Boolean
        get() = description.isBlank() && unitPrice.isBlank()
}

/**
 * Format a double value as money (e.g., "€123.45").
 * Multiplatform-compatible formatting.
 */
fun formatMoney(value: Double): String {
    val rounded = round(value * 100) / 100
    val isNegative = rounded < 0
    val absValue = rounded.absoluteValue
    val intPart = absValue.toLong()
    val decPart = ((absValue - intPart) * 100 + 0.5).toInt()
    val sign = if (isNegative) "-" else ""
    return "$sign€$intPart.${decPart.toString().padStart(2, '0')}"
}
