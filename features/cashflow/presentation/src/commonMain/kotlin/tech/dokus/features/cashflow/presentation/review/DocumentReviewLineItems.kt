package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.FinancialLineItem

internal class DocumentReviewLineItems {
    suspend fun DocumentReviewCtx.handleAddLineItem() {
        withState<DocumentReviewState.Content, _> {
            val newItem = FinancialLineItem(
                description = "",
                quantity = 1L,
                unitPrice = null,
                vatRate = null,
                netAmount = null
            )
            updateLineItems(newItem, add = true)
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: FinancialLineItem) {
        withState<DocumentReviewState.Content, _> {
            updateLineItems(item, index)
        }
    }

    suspend fun DocumentReviewCtx.handleRemoveLineItem(index: Int) {
        withState<DocumentReviewState.Content, _> {
            updateLineItems(null, index)
        }
    }

    private fun DocumentReviewCtx.updateLineItems(
        item: FinancialLineItem?,
        index: Int? = null,
        add: Boolean = false,
    ) {
        val updatedData = when (editableData.documentType) {
            DocumentType.Invoice -> {
                val current = editableData.invoice ?: return
                val updatedItems = current.lineItems.toMutableList()
                when {
                    add -> updatedItems.add(item ?: return)
                    item != null && index != null && index in updatedItems.indices -> updatedItems[index] = item
                    item == null && index != null && index in updatedItems.indices -> updatedItems.removeAt(index)
                    else -> return
                }
                editableData.copy(invoice = current.copy(lineItems = updatedItems))
            }
            DocumentType.Bill -> {
                val current = editableData.bill ?: return
                val updatedItems = current.lineItems.toMutableList()
                when {
                    add -> updatedItems.add(item ?: return)
                    item != null && index != null && index in updatedItems.indices -> updatedItems[index] = item
                    item == null && index != null && index in updatedItems.indices -> updatedItems.removeAt(index)
                    else -> return
                }
                editableData.copy(bill = current.copy(lineItems = updatedItems))
            }
            DocumentType.Receipt -> {
                val current = editableData.receipt ?: return
                val updatedItems = current.lineItems.toMutableList()
                when {
                    add -> updatedItems.add(item ?: return)
                    item != null && index != null && index in updatedItems.indices -> updatedItems[index] = item
                    item == null && index != null && index in updatedItems.indices -> updatedItems.removeAt(index)
                    else -> return
                }
                editableData.copy(receipt = current.copy(lineItems = updatedItems))
            }
            DocumentType.CreditNote -> {
                val current = editableData.creditNote ?: return
                val updatedItems = current.lineItems.toMutableList()
                when {
                    add -> updatedItems.add(item ?: return)
                    item != null && index != null && index in updatedItems.indices -> updatedItems[index] = item
                    item == null && index != null && index in updatedItems.indices -> updatedItems.removeAt(index)
                    else -> return
                }
                editableData.copy(creditNote = current.copy(lineItems = updatedItems))
            }
            else -> return
        }

        updateState {
            copy(
                editableData = updatedData,
                hasUnsavedChanges = true
            )
        }
    }
}
