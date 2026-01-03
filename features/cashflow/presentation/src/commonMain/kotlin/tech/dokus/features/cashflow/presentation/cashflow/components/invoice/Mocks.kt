package tech.dokus.features.cashflow.presentation.cashflow.components.invoice

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.City
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.PhoneNumber
import tech.dokus.domain.ids.ContactId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceFormState
import tech.dokus.features.cashflow.mvi.model.CreateInvoiceUiState
import tech.dokus.features.cashflow.mvi.model.InvoiceLineItem

// Mock date constants
private const val MockYear = 2024
private const val MockMonth = 12
private const val MockDay = 13
private const val MockHour = 12
private const val MockMinute = 0

// Mock invoice line item values
private const val WebDevQuantity = 40.0
private const val WebDevUnitPrice = "85.00"
private const val UiUxQuantity = 16.0
private const val UiUxUnitPrice = "75.00"
private const val ConsultingQuantity = 8.0
private const val ConsultingUnitPrice = "120.00"
private const val DefaultVatRatePercent = 21

// Due date offset
private const val DueDateYear = 2025
private const val DueDateMonth = 1

/**
 * Mock data for invoice component previews.
 */
object Mocks {

    private val now = LocalDateTime(MockYear, MockMonth, MockDay, MockHour, MockMinute)

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    val sampleClient = ContactDto(
        id = ContactId.generate(),
        tenantId = TenantId.generate(),
        name = Name("Acme Corporation"),
        email = Email("billing@acme.com"),
        vatNumber = VatNumber("BE0123456789"),
        peppolId = "0208:0123456789",
        peppolEnabled = true,
        addressLine1 = "123 Business Street",
        city = City("Brussels"),
        postalCode = "1000",
        country = "BE",
        phone = PhoneNumber("+32 2 123 4567"),
        createdAt = now,
        updatedAt = now
    )

    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    val sampleClientWithoutPeppol = ContactDto(
        id = ContactId.generate(),
        tenantId = TenantId.generate(),
        name = Name("Local Business BVBA"),
        email = Email("contact@localbusiness.be"),
        vatNumber = VatNumber("BE9876543210"),
        peppolId = null,
        peppolEnabled = false,
        addressLine1 = "456 Commerce Lane",
        city = City("Antwerp"),
        postalCode = "2000",
        country = "BE",
        phone = null,
        createdAt = now,
        updatedAt = now
    )

    val sampleLineItems = listOf(
        InvoiceLineItem(
            id = "1",
            description = "Web Development Services",
            quantity = WebDevQuantity,
            unitPrice = WebDevUnitPrice,
            vatRatePercent = DefaultVatRatePercent
        ),
        InvoiceLineItem(
            id = "2",
            description = "UI/UX Design",
            quantity = UiUxQuantity,
            unitPrice = UiUxUnitPrice,
            vatRatePercent = DefaultVatRatePercent
        )
    )

    val sampleSingleLineItem = listOf(
        InvoiceLineItem(
            id = "1",
            description = "Consulting Services",
            quantity = ConsultingQuantity,
            unitPrice = ConsultingUnitPrice,
            vatRatePercent = DefaultVatRatePercent
        )
    )

    val sampleFormState = CreateInvoiceFormState(
        selectedClient = sampleClient,
        issueDate = LocalDate(MockYear, MockMonth, MockDay),
        dueDate = LocalDate(DueDateYear, DueDateMonth, MockDay),
        items = sampleLineItems,
        notes = "Thank you for your business!"
    )

    val sampleFormStateWithWarning = CreateInvoiceFormState(
        selectedClient = sampleClientWithoutPeppol,
        issueDate = LocalDate(MockYear, MockMonth, MockDay),
        dueDate = LocalDate(DueDateYear, DueDateMonth, MockDay),
        items = sampleSingleLineItem,
        notes = ""
    )

    val emptyFormState = CreateInvoiceFormState()

    val sampleUiState = CreateInvoiceUiState()
}
