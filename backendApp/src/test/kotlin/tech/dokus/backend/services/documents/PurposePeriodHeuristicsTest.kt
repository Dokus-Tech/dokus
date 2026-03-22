package tech.dokus.backend.services.documents

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import tech.dokus.domain.enums.DocumentDirection
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.FinancialLineItemDto
import tech.dokus.domain.model.InvoiceDraftData
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PurposePeriodHeuristicsTest {

    @Test
    fun `parses year month from notes`() {
        val draft = InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            notes = "Service period: 2026-02"
        )

        val result = PurposePeriodHeuristics.detectServicePeriodStart(draft)

        assertEquals(LocalDate(2026, 2, 1), result)
    }

    @Test
    fun `parses month range and chooses start month`() {
        val draft = InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            notes = "Model Y leasing January-February 2026"
        )

        val result = PurposePeriodHeuristics.detectServicePeriodStart(draft)

        assertEquals(LocalDate(2026, 1, 1), result)
    }

    @Test
    fun `parses mm slash yyyy from line item`() {
        val draft = CreditNoteDraftData(
            direction = DocumentDirection.Inbound,
            lineItems = listOf(FinancialLineItemDto(description = "ChatGPT subscription 02/2026"))
        )

        val result = PurposePeriodHeuristics.detectServicePeriodStart(draft)

        assertEquals(LocalDate(2026, 2, 1), result)
    }

    @Test
    fun `returns null when no deterministic pattern exists`() {
        val draft = InvoiceDraftData(
            direction = DocumentDirection.Inbound,
            notes = "Regular services rendered"
        )

        val result = PurposePeriodHeuristics.detectServicePeriodStart(draft)

        assertNull(result)
    }
}
