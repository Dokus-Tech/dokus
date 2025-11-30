package ai.dokus.cashflow.backend.routes

import ai.dokus.foundation.domain.enums.ExpenseCategory
import ai.dokus.foundation.domain.enums.InvoiceStatus
import ai.dokus.foundation.domain.ids.AttachmentId
import ai.dokus.foundation.domain.ids.ExpenseId
import ai.dokus.foundation.domain.ids.InvoiceId
import io.ktor.http.*
import kotlinx.datetime.LocalDate
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Extension properties for extracting typed parameters from Ktor Parameters.
 */

@OptIn(ExperimentalUuidApi::class)
val Parameters.invoiceId: InvoiceId?
    get() = this["invoiceId"]?.let { InvoiceId(Uuid.parse(it)) }

@OptIn(ExperimentalUuidApi::class)
val Parameters.expenseId: ExpenseId?
    get() = this["expenseId"]?.let { ExpenseId(Uuid.parse(it)) }

@OptIn(ExperimentalUuidApi::class)
val Parameters.attachmentId: AttachmentId?
    get() = this["id"]?.let { AttachmentId(Uuid.parse(it)) }

val Parameters.invoiceStatus: InvoiceStatus?
    get() = this["status"]?.let {
        try {
            InvoiceStatus.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

val Parameters.expenseCategory: ExpenseCategory?
    get() = this["category"]?.let {
        try {
            ExpenseCategory.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

val Parameters.fromDate: LocalDate?
    get() = this["fromDate"]?.let {
        try {
            LocalDate.parse(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

val Parameters.toDate: LocalDate?
    get() = this["toDate"]?.let {
        try {
            LocalDate.parse(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

val Parameters.limit: Int
    get() = this["limit"]?.toIntOrNull() ?: 50

val Parameters.offset: Int
    get() = this["offset"]?.toIntOrNull() ?: 0
