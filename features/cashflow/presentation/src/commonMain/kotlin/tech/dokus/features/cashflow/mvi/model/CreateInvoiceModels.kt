package tech.dokus.features.cashflow.mvi.model

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import tech.dokus.domain.Money
import tech.dokus.domain.VatRate
import tech.dokus.domain.enums.InvoiceDeliveryMethod
import tech.dokus.domain.enums.InvoiceDueDateMode
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolStatusResponse
import tech.dokus.domain.model.contact.ContactDto
import kotlin.math.round
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

private const val DefaultDueDateOffsetDays = 30
private const val DefaultQuantity = 1.0
private const val DefaultVatRatePercent = 21
private const val VatRateMultiplier = 100

enum class DatePickerTarget {
    IssueDate,
    DueDate
}

enum class InvoiceSection {
    Client,
    LineItems,
    PaymentDelivery,
    DatesTerms
}

enum class InvoiceResolvedAction {
    Peppol,
    PdfExport
}

data class DeliveryResolution(
    val action: InvoiceResolvedAction,
    val reason: String? = null
)

data class LatestInvoiceSuggestion(
    val issueDate: LocalDate,
    val lines: List<InvoiceLineItem>
)

data class ExternalClientCandidate(
    val name: String,
    val vatNumber: VatNumber?,
    val enterpriseNumber: String,
    val prefillAddress: String? = null
)

sealed interface ClientSuggestion {
    data class LocalContact(val contact: ContactDto) : ClientSuggestion
    data class ExternalCompany(val candidate: ExternalClientCandidate) : ClientSuggestion
    data class CreateManual(val query: String) : ClientSuggestion
}

data class ClientLookupState(
    val query: String = "",
    val isExpanded: Boolean = false,
    val localResults: List<ContactDto> = emptyList(),
    val externalResults: List<ExternalClientCandidate> = emptyList(),
    val isLocalLoading: Boolean = false,
    val isExternalLoading: Boolean = false,
    val mergedSuggestions: List<ClientSuggestion> = emptyList(),
    val errorHint: String? = null
) {
    val isLoading: Boolean
        get() = isLocalLoading || isExternalLoading
}

data class CreateInvoiceUiState(
    val expandedItemId: String? = null,
    val clientLookupState: ClientLookupState = ClientLookupState(),
    val senderCompanyName: String = "",
    val senderCompanyVat: String? = null,
    val isDatePickerOpen: DatePickerTarget? = null,
    val expandedSections: Set<InvoiceSection> = setOf(InvoiceSection.Client),
    val suggestedSection: InvoiceSection? = null,
    val selectedDeliveryPreference: InvoiceDeliveryMethod = InvoiceDeliveryMethod.Peppol,
    val resolvedDeliveryAction: DeliveryResolution = DeliveryResolution(
        action = InvoiceResolvedAction.PdfExport,
        reason = "Select a client to enable PEPPOL."
    ),
    val latestInvoiceSuggestion: LatestInvoiceSuggestion? = null,
    val isPreviewVisible: Boolean = false,
    val defaultsLoaded: Boolean = false
)

data class CreateInvoiceFormState(
    val selectedClient: ContactDto? = null,
    val issueDate: LocalDate? = null,
    val dueDate: LocalDate? = null,
    val paymentTermsDays: Int = DefaultDueDateOffsetDays,
    val dueDateMode: InvoiceDueDateMode = InvoiceDueDateMode.Terms,
    val structuredCommunication: String = "",
    val senderIban: String = "",
    val senderBic: String = "",
    val notes: String = "",
    val items: List<InvoiceLineItem> = listOf(InvoiceLineItem()),
    val peppolStatus: PeppolStatusResponse? = null,
    val peppolStatusLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errors: Map<String, DokusException> = emptyMap()
) {
    val subtotalMoney: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.lineTotalMoney }

    val vatAmountMoney: Money
        get() = items.fold(Money.ZERO) { acc, item -> acc + item.vatAmountMoney }

    val totalMoney: Money
        get() = subtotalMoney + vatAmountMoney

    val subtotal: String
        get() = formatMoney(subtotalMoney)

    val vatAmount: String
        get() = formatMoney(vatAmountMoney)

    val total: String
        get() = formatMoney(totalMoney)

    val isValid: Boolean
        get() = selectedClient != null &&
            issueDate != null &&
            dueDate != null &&
            items.any { it.isValid }

    companion object {
        @OptIn(ExperimentalTime::class)
        fun createInitial(): CreateInvoiceFormState {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return CreateInvoiceFormState(
                issueDate = today,
                dueDate = today.plus(DefaultDueDateOffsetDays, DateTimeUnit.DAY),
                items = listOf(InvoiceLineItem())
            )
        }
    }
}

data class InvoiceLineItem(
    val id: String = Random.nextLong().toString(),
    val description: String = "",
    val quantity: Double = DefaultQuantity,
    val unitPrice: String = "",
    val vatRatePercent: Int = DefaultVatRatePercent
) {
    val unitPriceMoney: Money
        get() = Money.parse(unitPrice) ?: Money.ZERO

    val lineTotalMoney: Money
        get() = Money(round(unitPriceMoney.minor.toDouble() * quantity).toLong())

    val vatRate: VatRate
        get() = VatRate(vatRatePercent * VatRateMultiplier)

    val vatAmountMoney: Money
        get() = vatRate.applyTo(lineTotalMoney)

    val lineTotal: String
        get() = formatMoney(lineTotalMoney)

    val isValid: Boolean
        get() = description.isNotBlank() && quantity > 0 && unitPriceMoney.isPositive

    val isEmpty: Boolean
        get() = description.isBlank() && unitPrice.isBlank()
}

fun formatMoney(amount: Money): String = "€${amount.toDisplayString()}"
