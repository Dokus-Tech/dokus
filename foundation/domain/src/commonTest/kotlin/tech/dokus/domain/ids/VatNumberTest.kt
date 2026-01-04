package tech.dokus.domain.ids

import kotlin.test.Test
import kotlin.test.assertEquals

class VatNumberTest {

    @Test
    fun `normalize strips whitespace punctuation and uppercases`() {
        val normalized = VatNumber.normalize(" be 0123.456.789 ")
        assertEquals("BE0123456789", normalized)
    }

    @Test
    fun `normalized property uses normalize rules`() {
        val vatNumber = VatNumber("be-0123 456 789")
        assertEquals("BE0123456789", vatNumber.normalized)
    }
}
