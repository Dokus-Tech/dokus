package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.model.BillDraftData
import tech.dokus.domain.model.CreditNoteDraftData
import tech.dokus.domain.model.FinancialLineItem
import tech.dokus.domain.model.InvoiceDraftData
import tech.dokus.domain.model.ReceiptDraftData

internal class DocumentReviewLineItems {
    suspend fun DocumentReviewCtx.handleAddLineItem() {
        val newItem = FinancialLineItem(
            description = "",
            quantity = 1L,
            unitPrice = null,
            vatRate = null,
            netAmount = null
        )
        updateLineItems(newItem, add = true)
    }

    suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: FinancialLineItem) {
        updateLineItems(item, index)
    }

    suspend fun DocumentReviewCtx.handleRemoveLineItem(index: Int) {
        updateLineItems(null, index)
    }

    private suspend fun DocumentReviewCtx.updateLineItems(
        item: FinancialLineItem?,
        index: Int? = null,
        add: Boolean = false,
    ) {
        withState<DocumentReviewState.Content, _> {
            val currentData = draftData ?: return@withState

            val currentItems = when (currentData) {
                is InvoiceDraftData -> currentData.lineItems
                is BillDraftData -> currentData.lineItems
                is ReceiptDraftData -> currentData.lineItems
                is CreditNoteDraftData -> currentData.lineItems
            }

            val updatedItems = currentItems.toMutableList()
            when {
                add -> updatedItems.add(item ?: return@withState)
                item != null && index != null && index in updatedItems.indices -> updatedItems[index] = item
                item == null && index != null && index in updatedItems.indices -> updatedItems.removeAt(index)
                else -> return@withState
            }

            val updatedDraftData = when (currentData) {
                is InvoiceDraftData -> currentData.copy(lineItems = updatedItems)
                is BillDraftData -> currentData.copy(lineItems = updatedItems)
                is ReceiptDraftData -> currentData.copy(lineItems = updatedItems)
                is CreditNoteDraftData -> currentData.copy(lineItems = updatedItems)
            }

            updateState {
                copy(
                    draftData = updatedDraftData,
                    hasUnsavedChanges = true
                )
            }
        }
    }
}
