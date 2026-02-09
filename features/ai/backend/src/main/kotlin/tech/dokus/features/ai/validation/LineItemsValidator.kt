package tech.dokus.features.ai.validation

import tech.dokus.domain.Money
import tech.dokus.domain.model.FinancialLineItem

object LineItemsValidator {

    fun verify(
        lineItems: List<FinancialLineItem>,
        subtotal: Money?,
        required: Boolean
    ): List<AuditCheck> = buildList {
        if (lineItems.isEmpty()) {
            add(missingLineItems(required, "No line items extracted"))
            return@buildList
        }

        val netAmounts = lineItems.mapNotNull { it.netAmount?.let { amount -> Money(amount) } }
        if (netAmounts.size == lineItems.size) {
            add(MathValidator.verifyLineItems(netAmounts, subtotal))
        } else {
            add(missingLineItems(required, "Some line items are missing net amounts"))
        }

        lineItems.forEachIndexed { index, item ->
            add(
                MathValidator.verifyLineItemCalculation(
                    quantity = item.quantity?.toDouble(),
                    unitPrice = item.unitPrice?.let { Money(it) },
                    lineTotal = item.netAmount?.let { Money(it) },
                    lineIndex = index + 1
                )
            )
        }
    }

    private fun missingLineItems(required: Boolean, message: String): AuditCheck {
        return if (required) {
            AuditCheck.warning(
                type = CheckType.LINE_ITEMS,
                field = "lineItems",
                message = message
            )
        } else {
            AuditCheck.incomplete(
                type = CheckType.LINE_ITEMS,
                field = "lineItems",
                message = message
            )
        }
    }
}
