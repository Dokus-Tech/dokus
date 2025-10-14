package ai.dokus.foundation.database

import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.enums.*
import ai.dokus.foundation.domain.model.*
import kotlinx.datetime.*

/**
 * Test fixtures for creating test data
 * Provides consistent test data across all test suites
 */
object TestFixtures {
    // Test tenant IDs
    val tenant1Id = TenantId("00000000-0000-0000-0000-000000000001")
    val tenant2Id = TenantId("00000000-0000-0000-0000-000000000002")

    // Test client IDs
    val client1Id = ClientId("10000000-0000-0000-0000-000000000001")
    val client2Id = ClientId("10000000-0000-0000-0000-000000000002")

    // Test invoice IDs
    val invoice1Id = InvoiceId("20000000-0000-0000-0000-000000000001")
    val invoice2Id = InvoiceId("20000000-0000-0000-0000-000000000002")

    // Test expense IDs
    val expense1Id = ExpenseId("30000000-0000-0000-0000-000000000001")
    val expense2Id = ExpenseId("30000000-0000-0000-0000-000000000002")

    // Test payment IDs
    val payment1Id = PaymentId("40000000-0000-0000-0000-000000000001")
    val payment2Id = PaymentId("40000000-0000-0000-0000-000000000002")

    // Common test values
    val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val today: LocalDate = now.date
    val tomorrow: LocalDate = today.plus(1, DateTimeUnit.DAY)
    val yesterday: LocalDate = today.minus(1, DateTimeUnit.DAY)

    fun createTestClient(
        id: ClientId = client1Id,
        tenantId: TenantId = tenant1Id,
        name: String = "Acme Corporation",
        email: Email? = Email("contact@acme.com"),
        vatNumber: VatNumber? = VatNumber("BE0123456789"),
        addressLine1: String? = "123 Main Street",
        city: String? = "Brussels",
        postalCode: String? = "1000",
        country: String? = "BE",
        peppolId: String? = "0208:BE0123456789",
        peppolEnabled: Boolean = true,
        isActive: Boolean = true
    ): Client = Client(
        id = id,
        tenantId = tenantId,
        name = name,
        email = email,
        vatNumber = vatNumber,
        addressLine1 = addressLine1,
        addressLine2 = null,
        city = city,
        postalCode = postalCode,
        country = country,
        contactPerson = "John Doe",
        phone = "+32 2 123 4567",
        companyNumber = "0123456789",
        defaultPaymentTerms = 30,
        defaultVatRate = VatRate.STANDARD_BE,
        peppolId = peppolId,
        peppolEnabled = peppolEnabled,
        tags = "vip,enterprise",
        notes = "Important client",
        isActive = isActive,
        createdAt = now,
        updatedAt = now
    )

    fun createTestInvoice(
        id: InvoiceId = invoice1Id,
        tenantId: TenantId = tenant1Id,
        clientId: ClientId = client1Id,
        invoiceNumber: InvoiceNumber = InvoiceNumber("INV-2025-001"),
        issueDate: LocalDate = today,
        dueDate: LocalDate = tomorrow,
        subtotalAmount: Money = Money("1000.00"),
        vatAmount: Money = Money("210.00"),
        totalAmount: Money = Money("1210.00"),
        paidAmount: Money = Money.ZERO,
        status: InvoiceStatus = InvoiceStatus.Draft,
        currency: Currency = Currency.Eur,
        notes: String? = "Test invoice"
    ): Invoice = Invoice(
        id = id,
        tenantId = tenantId,
        clientId = clientId,
        invoiceNumber = invoiceNumber,
        issueDate = issueDate,
        dueDate = dueDate,
        subtotalAmount = subtotalAmount,
        vatAmount = vatAmount,
        totalAmount = totalAmount,
        paidAmount = paidAmount,
        status = status,
        currency = currency,
        notes = notes,
        termsAndConditions = "Payment within 30 days",
        items = emptyList(),
        peppolId = null,
        peppolSentAt = null,
        peppolStatus = null,
        paymentLink = null,
        paymentLinkExpiresAt = null,
        paidAt = null,
        paymentMethod = null,
        createdAt = now,
        updatedAt = now
    )

    fun createTestInvoiceItem(
        id: InvoiceItemId? = InvoiceItemId("50000000-0000-0000-0000-000000000001"),
        invoiceId: InvoiceId? = invoice1Id,
        description: String = "Software Development Services",
        quantity: Quantity = Quantity("10.00"),
        unitPrice: Money = Money("100.00"),
        vatRate: VatRate = VatRate.STANDARD_BE,
        lineTotal: Money = Money("1000.00"),
        vatAmount: Money = Money("210.00"),
        sortOrder: Int = 0
    ): InvoiceItem = InvoiceItem(
        id = id,
        invoiceId = invoiceId,
        description = description,
        quantity = quantity,
        unitPrice = unitPrice,
        vatRate = vatRate,
        lineTotal = lineTotal,
        vatAmount = vatAmount,
        sortOrder = sortOrder
    )

    fun createTestExpense(
        id: ExpenseId = expense1Id,
        tenantId: TenantId = tenant1Id,
        date: LocalDate = today,
        merchant: String = "Office Supplies Inc",
        amount: Money = Money("250.00"),
        vatAmount: Money? = Money("52.50"),
        vatRate: VatRate? = VatRate.STANDARD_BE,
        category: ExpenseCategory = ExpenseCategory.OfficeSupplies,
        description: String? = "Monthly office supplies",
        receiptUrl: String? = "https://s3.example.com/receipts/receipt-001.pdf",
        receiptFilename: String? = "receipt-001.pdf",
        isDeductible: Boolean = true,
        deductiblePercentage: Percentage = Percentage.FULL,
        paymentMethod: PaymentMethod? = PaymentMethod.CreditCard,
        isRecurring: Boolean = false,
        notes: String? = "Regular purchase"
    ): Expense = Expense(
        id = id,
        tenantId = tenantId,
        date = date,
        merchant = merchant,
        amount = amount,
        vatAmount = vatAmount,
        vatRate = vatRate,
        category = category,
        description = description,
        receiptUrl = receiptUrl,
        receiptFilename = receiptFilename,
        isDeductible = isDeductible,
        deductiblePercentage = deductiblePercentage,
        paymentMethod = paymentMethod,
        isRecurring = isRecurring,
        notes = notes,
        createdAt = now,
        updatedAt = now
    )

    fun createTestPayment(
        id: PaymentId = payment1Id,
        tenantId: TenantId = tenant1Id,
        invoiceId: InvoiceId = invoice1Id,
        amount: Money = Money("1210.00"),
        paymentDate: LocalDate = today,
        paymentMethod: PaymentMethod = PaymentMethod.BankTransfer,
        transactionId: TransactionId? = TransactionId("TXN-123456"),
        notes: String? = "Full payment received"
    ): Payment = Payment(
        id = id,
        tenantId = tenantId,
        invoiceId = invoiceId,
        amount = amount,
        paymentDate = paymentDate,
        paymentMethod = paymentMethod,
        transactionId = transactionId,
        notes = notes,
        createdAt = now
    )
}
