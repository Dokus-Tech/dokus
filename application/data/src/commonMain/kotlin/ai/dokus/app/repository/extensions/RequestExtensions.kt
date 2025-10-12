package ai.dokus.app.repository.extensions

import ai.dokus.foundation.domain.model.DocumentType
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter

private enum class Headers(val key: String) {
    CompanyId("X-Company-Id"),
}

private enum class Parameters(val key: String) {
    Page("page"),
    Size("size"),
    DocumentType("document_type"),
    SupplierId("supplier_id"),
    DateFrom("date_from"),
    DateTo("date_to"),
    AmountMin("amount_min"),
    AmountMax("amount_max"),
    Ids("ids")
}

private fun HttpRequestBuilder.header(header: Headers, value: Any?) {
    header(header.key, value)
}

private fun HttpRequestBuilder.parameter(parameter: Parameters, value: Any?) {
    parameter(parameter.key, value)
}


internal fun HttpRequestBuilder.withCompanyId(companyId: String) {
    header(Headers.CompanyId, companyId)
}

internal fun HttpRequestBuilder.withPagination(page: Int, size: Int) {
    parameter(Parameters.Page, page)
    parameter(Parameters.Size, size)
}

internal fun HttpRequestBuilder.withDocumentType(documentType: DocumentType?) {
    if (documentType == null) return
    parameter(Parameters.DocumentType, documentType)
}

internal fun HttpRequestBuilder.withSupplierId(supplierId: String?) {
    if (supplierId == null) return
    parameter(Parameters.SupplierId, supplierId)
}

internal fun HttpRequestBuilder.withDateRange(dateFrom: String?, dateTo: String?) {
    dateFrom?.let { parameter(Parameters.DateFrom, it) }
    dateTo?.let { parameter(Parameters.DateTo, it) }
}

internal fun HttpRequestBuilder.withAmountRange(amountMin: Double?, amountMax: Double?) {
    amountMin?.let { parameter(Parameters.AmountMin, it) }
    amountMax?.let { parameter(Parameters.AmountMax, it) }
}

internal fun HttpRequestBuilder.withIds(ids: List<String>?) {
    if (ids.isNullOrEmpty()) return
    parameter(Parameters.Ids, ids.joinToString(","))
}