package ai.dokus.foundation.database

import ai.dokus.foundation.domain.*
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Test fixtures providing common test data
 */
@OptIn(ExperimentalUuidApi::class)
object TestFixtures {
    // Tenant IDs
    val tenant1Id = TenantId(Uuid.parse("00000000-0000-0000-0000-000000000001"))
    val tenant2Id = TenantId(Uuid.parse("00000000-0000-0000-0000-000000000002"))

    // Client IDs
    val client1Id = ClientId(Uuid.parse("00000000-0000-0000-0000-000000000010"))
    val client2Id = ClientId(Uuid.parse("00000000-0000-0000-0000-000000000011"))

    // Invoice IDs
    val invoice1Id = InvoiceId(Uuid.parse("00000000-0000-0000-0000-000000000020"))
    val invoice2Id = InvoiceId(Uuid.parse("00000000-0000-0000-0000-000000000021"))

    // Payment IDs
    val payment1Id = PaymentId(Uuid.parse("00000000-0000-0000-0000-000000000030"))

    // Expense IDs
    val expense1Id = ExpenseId(Uuid.parse("00000000-0000-0000-0000-000000000040"))

    // Attachment IDs
    val attachment1Id = AttachmentId(Uuid.parse("00000000-0000-0000-0000-000000000050"))
}
