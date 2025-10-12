package ai.dokus.app.repository.extensions

import ai.dokus.foundation.domain.exceptions.DokusException
import ai.dokus.foundation.domain.exceptions.fromRestStatus
import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText

suspend inline fun <reified T> HttpResponse.bodyIfOk(): T {
    if (status.value in 200..299 || status.value == 302) {
        return body<T>()
    } else {
        throw DokusException.fromRestStatus(status.value)
            ?: Exception("Request failed with status ${status.value}")
    }
}

suspend inline fun HttpResponse.bodyAsTextIfOk(): String {
    if (status.value in 200..299 || status.value == 302) {
        return bodyAsText()
    } else {
        throw DokusException.fromRestStatus(status.value)
            ?: Exception("Request failed with status ${status.value}")
    }
}