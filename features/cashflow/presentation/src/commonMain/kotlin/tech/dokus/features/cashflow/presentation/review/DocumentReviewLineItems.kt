package tech.dokus.features.cashflow.presentation.review

import pro.respawn.flowmvi.dsl.withState
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.model.ExtractedLineItem

internal class DocumentReviewLineItems {
    suspend fun DocumentReviewCtx.handleAddLineItem() {
        withState<DocumentReviewState.Content, _> {
            if (editableData.documentType != DocumentType.Invoice) return@withState

            val currentInvoice = editableData.invoice ?: return@withState
            val newItem = ExtractedLineItem(
                description = "",
                quantity = 1.0,
                unitPrice = null,
                vatRate = null,
                lineTotal = null,
                vatAmount = null
            )
            val updatedItems = currentInvoice.items + newItem
            val updatedInvoice = currentInvoice.copy(items = updatedItems)

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleUpdateLineItem(index: Int, item: ExtractedLineItem) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState
            if (index < 0 || index >= currentInvoice.items.size) return@withState

            val updatedItems = currentInvoice.items.toMutableList()
            updatedItems[index] = item
            val updatedInvoice = currentInvoice.copy(items = updatedItems)

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }

    suspend fun DocumentReviewCtx.handleRemoveLineItem(index: Int) {
        withState<DocumentReviewState.Content, _> {
            val currentInvoice = editableData.invoice ?: return@withState
            if (index < 0 || index >= currentInvoice.items.size) return@withState

            val updatedItems = currentInvoice.items.toMutableList()
            updatedItems.removeAt(index)
            val updatedInvoice = currentInvoice.copy(items = updatedItems)

            updateState {
                copy(
                    editableData = editableData.copy(invoice = updatedInvoice),
                    hasUnsavedChanges = true
                )
            }
        }
    }
}
