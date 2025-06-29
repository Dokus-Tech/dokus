package ai.thepredict.repository.extensions

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.parameter

private enum class Headers(val key: String) {
    CompanyId("X-Company-Id"),
}

private enum class Parameters(val key: String) {
    Page("page"),
    Size("size")
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