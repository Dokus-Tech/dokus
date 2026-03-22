package tech.dokus.backend.cashflow

import kotlinx.datetime.LocalDate
import org.junit.jupiter.api.Test
import tech.dokus.database.repository.auth.computeCashflowStartDate
import kotlin.test.assertEquals

class CashflowStartDateTest {

    @Test
    fun `March creation yields January 1st`() {
        val creationDate = LocalDate(2026, 3, 15)
        val result = computeCashflowStartDate(creationDate)
        assertEquals(LocalDate(2025, 12, 1), result)
    }

    @Test
    fun `April creation yields January 1st`() {
        val creationDate = LocalDate(2026, 4, 1)
        val result = computeCashflowStartDate(creationDate)
        assertEquals(LocalDate(2026, 1, 1), result)
    }

    @Test
    fun `January creation yields October 1st of previous year`() {
        val creationDate = LocalDate(2026, 1, 20)
        val result = computeCashflowStartDate(creationDate)
        assertEquals(LocalDate(2025, 10, 1), result)
    }

    @Test
    fun `June creation yields March 1st`() {
        val creationDate = LocalDate(2026, 6, 10)
        val result = computeCashflowStartDate(creationDate)
        assertEquals(LocalDate(2026, 3, 1), result)
    }

    @Test
    fun `December creation yields September 1st`() {
        val creationDate = LocalDate(2026, 12, 31)
        val result = computeCashflowStartDate(creationDate)
        assertEquals(LocalDate(2026, 9, 1), result)
    }
}
