package tech.dokus.features.ai.ensemble

import tech.dokus.features.ai.models.ExtractedBillData
import tech.dokus.features.ai.models.ExtractedInvoiceData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConsensusEngineTest {

    private val engine = ConsensusEngine()

    // =========================================================================
    // Invoice Consensus Tests
    // =========================================================================

    @Test
    fun `mergeInvoices returns NoData when both candidates are null`() {
        val result = engine.mergeInvoices(null, null)

        assertIs<ConsensusResult.NoData>(result)
        assertFalse(result.hasData)
    }

    @Test
    fun `mergeInvoices returns SingleSource for fast when expert is null`() {
        val fast = ExtractedInvoiceData(vendorName = "Fast Corp", totalAmount = "100.00")

        val result = engine.mergeInvoices(fast, null)

        assertIs<ConsensusResult.SingleSource<ExtractedInvoiceData>>(result)
        assertEquals("fast", result.source)
        assertEquals("Fast Corp", result.data.vendorName)
    }

    @Test
    fun `mergeInvoices returns SingleSource for expert when fast is null`() {
        val expert = ExtractedInvoiceData(vendorName = "Expert Corp", totalAmount = "200.00")

        val result = engine.mergeInvoices(null, expert)

        assertIs<ConsensusResult.SingleSource<ExtractedInvoiceData>>(result)
        assertEquals("expert", result.source)
        assertEquals("Expert Corp", result.data.vendorName)
    }

    @Test
    fun `mergeInvoices returns Unanimous when both models agree`() {
        val fast = ExtractedInvoiceData(
            vendorName = "Acme Inc",
            totalAmount = "121.00",
            subtotal = "100.00",
            totalVatAmount = "21.00"
        )
        val expert = ExtractedInvoiceData(
            vendorName = "Acme Inc",
            totalAmount = "121.00",
            subtotal = "100.00",
            totalVatAmount = "21.00"
        )

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.Unanimous<ExtractedInvoiceData>>(result)
        assertEquals("Acme Inc", result.data.vendorName)
        assertEquals("121.00", result.data.totalAmount)
        assertTrue(result.hasBothSources)
    }

    @Test
    fun `mergeInvoices detects conflicts when models disagree`() {
        val fast = ExtractedInvoiceData(
            vendorName = "Acme Inc",
            totalAmount = "100.00" // Different
        )
        val expert = ExtractedInvoiceData(
            vendorName = "Acme Inc",
            totalAmount = "150.00" // Different
        )

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.WithConflicts<ExtractedInvoiceData>>(result)
        assertTrue(result.report.hasConflicts)
        assertEquals(1, result.report.conflicts.size)

        val conflict = result.report.conflicts.first()
        assertEquals("totalAmount", conflict.field)
        assertEquals("100.00", conflict.fastValue)
        assertEquals("150.00", conflict.expertValue)
        assertEquals(ConflictSeverity.CRITICAL, conflict.severity) // totalAmount is critical
    }

    @Test
    fun `mergeInvoices prefers expert value when models disagree`() {
        val fast = ExtractedInvoiceData(vendorName = "Fast Corp")
        val expert = ExtractedInvoiceData(vendorName = "Expert Corp")

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.WithConflicts<ExtractedInvoiceData>>(result)
        assertEquals("Expert Corp", result.data.vendorName)
        assertEquals("expert", result.report.conflicts.first().chosenSource)
    }

    @Test
    fun `mergeInvoices uses available value when one is missing`() {
        val fast = ExtractedInvoiceData(vendorName = "Fast Corp", iban = null)
        val expert = ExtractedInvoiceData(vendorName = "Fast Corp", iban = "BE68539007547034")

        val result = engine.mergeInvoices(fast, expert)

        // No conflict because only one has the value
        assertIs<ConsensusResult.Unanimous<ExtractedInvoiceData>>(result)
        assertEquals("BE68539007547034", result.data.iban)
    }

    @Test
    fun `mergeInvoices treats numerically equal amounts as agreement`() {
        val fast = ExtractedInvoiceData(totalAmount = "100.00")
        val expert = ExtractedInvoiceData(totalAmount = "100") // Same value, different format

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.Unanimous<ExtractedInvoiceData>>(result)
        assertEquals("100", result.data.totalAmount) // Expert's format is used
    }

    @Test
    fun `mergeInvoices treats trimmed equal strings as agreement`() {
        val fast = ExtractedInvoiceData(vendorName = "Acme Inc ")
        val expert = ExtractedInvoiceData(vendorName = "Acme Inc")

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.Unanimous<ExtractedInvoiceData>>(result)
    }

    // =========================================================================
    // Bill Consensus Tests
    // =========================================================================

    @Test
    fun `mergeBills returns NoData when both candidates are null`() {
        val result = engine.mergeBills(null, null)

        assertIs<ConsensusResult.NoData>(result)
    }

    @Test
    fun `mergeBills returns Unanimous when both models agree`() {
        val fast = ExtractedBillData(
            supplierName = "Supplier Co",
            totalAmount = "242.00"
        )
        val expert = ExtractedBillData(
            supplierName = "Supplier Co",
            totalAmount = "242.00"
        )

        val result = engine.mergeBills(fast, expert)

        assertIs<ConsensusResult.Unanimous<ExtractedBillData>>(result)
        assertEquals("Supplier Co", result.data.supplierName)
    }

    @Test
    fun `mergeBills detects conflicts on critical fields`() {
        val fast = ExtractedBillData(bankAccount = "BE68539007547034")
        val expert = ExtractedBillData(bankAccount = "BE68539007547035") // Different

        val result = engine.mergeBills(fast, expert)

        assertIs<ConsensusResult.WithConflicts<ExtractedBillData>>(result)
        // Note: bankAccount maps to iban resolution which is critical
    }

    // =========================================================================
    // Conflict Report Tests
    // =========================================================================

    @Test
    fun `conflict report separates critical and warning conflicts`() {
        val fast = ExtractedInvoiceData(
            vendorName = "Fast Corp",      // Non-critical
            totalAmount = "100.00",        // Critical
            paymentReference = "REF-001"   // Critical
        )
        val expert = ExtractedInvoiceData(
            vendorName = "Expert Corp",
            totalAmount = "150.00",
            paymentReference = "REF-002"
        )

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.WithConflicts<ExtractedInvoiceData>>(result)

        val report = result.report
        assertEquals(3, report.conflicts.size)
        assertEquals(2, report.criticalConflicts.size) // totalAmount, paymentReference
        assertEquals(1, report.warningConflicts.size) // vendorName
    }

    @Test
    fun `conflictsByField provides lookup by field name`() {
        val fast = ExtractedInvoiceData(vendorName = "Fast", totalAmount = "100")
        val expert = ExtractedInvoiceData(vendorName = "Expert", totalAmount = "200")

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.WithConflicts<ExtractedInvoiceData>>(result)

        val vendorConflict = result.report.conflictsByField["vendorName"]
        assertEquals("Fast", vendorConflict?.fastValue)
        assertEquals("Expert", vendorConflict?.expertValue)
    }

    // =========================================================================
    // Confidence Calculation Tests
    // =========================================================================

    @Test
    fun `merged confidence is weighted average of inputs`() {
        val fast = ExtractedInvoiceData(vendorName = "Same", confidence = 0.8)
        val expert = ExtractedInvoiceData(vendorName = "Same", confidence = 0.9)

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.Unanimous<ExtractedInvoiceData>>(result)
        // Formula: (0.8 + 0.9*2) / 3 = 2.6/3 = 0.867
        assertTrue(result.data.confidence > 0.85)
        assertTrue(result.data.confidence < 0.9)
    }

    @Test
    fun `conflicts reduce merged confidence`() {
        val fast = ExtractedInvoiceData(
            vendorName = "Fast",
            totalAmount = "100",
            confidence = 0.9
        )
        val expert = ExtractedInvoiceData(
            vendorName = "Expert",
            totalAmount = "200",
            confidence = 0.9
        )

        val result = engine.mergeInvoices(fast, expert)

        assertIs<ConsensusResult.WithConflicts<ExtractedInvoiceData>>(result)
        // 2 conflicts should reduce confidence by 10%
        assertTrue(result.data.confidence < 0.85)
    }

    // =========================================================================
    // ConsensusResult API Tests
    // =========================================================================

    @Test
    fun `dataOrNull returns data for all result types with data`() {
        val invoice = ExtractedInvoiceData(vendorName = "Test")

        assertEquals(null, ConsensusResult.NoData.dataOrNull())
        assertEquals(invoice, ConsensusResult.SingleSource(invoice, "fast").dataOrNull())
        assertEquals(invoice, ConsensusResult.Unanimous(invoice).dataOrNull())
        assertEquals(invoice, ConsensusResult.WithConflicts(invoice, ConflictReport.EMPTY).dataOrNull())
    }

    @Test
    fun `reportOrNull returns report only for WithConflicts`() {
        val invoice = ExtractedInvoiceData()
        val report = ConflictReport(listOf(
            FieldConflict("test", "a", "b", "b", "expert", ConflictSeverity.WARNING)
        ))

        assertNull(ConsensusResult.NoData.reportOrNull())
        assertNull(ConsensusResult.SingleSource(invoice, "fast").reportOrNull())
        assertNull(ConsensusResult.Unanimous(invoice).reportOrNull())
        assertEquals(report, ConsensusResult.WithConflicts(invoice, report).reportOrNull())
    }
}
