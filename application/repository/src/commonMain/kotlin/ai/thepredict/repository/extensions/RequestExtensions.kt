package ai.thepredict.repository.extensions

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header

private enum class Headers(val key: String) {
    CompanyId("X-Company-Id")
}

private fun HttpRequestBuilder.header(header: Headers, value: Any?) {
    header(header.key, value)
}


internal fun HttpRequestBuilder.withCompanyId(companyId: String) {
    header(Headers.CompanyId, companyId)
}