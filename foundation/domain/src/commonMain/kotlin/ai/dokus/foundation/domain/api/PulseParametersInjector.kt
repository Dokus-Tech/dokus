package ai.dokus.foundation.domain.api

import io.ktor.http.ParametersBuilder
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun ParametersBuilder.withId(id: Uuid) {
    append(DokusParameters.Id.apiName, id.toString())
}

fun ParametersBuilder.withSearchTerm(searchTerm: String?) {
    if (searchTerm.isNullOrEmpty()) return
    append(DokusParameters.SearchTerm.apiName, searchTerm)
}

fun ParametersBuilder.withStatus(status: String?) {
    if (status.isNullOrEmpty()) return
    append(DokusParameters.Status.apiName, status)
}

fun ParametersBuilder.withLimit(limit: Int?) {
    if (limit == null) return
    if (limit <= 0) return
    if (limit > 100) return
    set(DokusParameters.Limit.apiName, limit.toString())
}

fun ParametersBuilder.withOffset(offset: Int?) {
    if (offset == null) return
    if (offset < 0) return
    set(DokusParameters.Offset.apiName, offset.toString())
}

fun ParametersBuilder.withEmail(email: String?) {
    if (email.isNullOrEmpty()) return
    append(DokusParameters.Email.apiName, email)
}

fun ParametersBuilder.withFirstName(firstName: String?) {
    if (firstName.isNullOrEmpty()) return
    append(DokusParameters.FirstName.apiName, firstName)
}

fun ParametersBuilder.withLastName(lastName: String?) {
    if (lastName.isNullOrEmpty()) return
    append(DokusParameters.LastName.apiName, lastName)
}

@OptIn(ExperimentalUuidApi::class)
fun ParametersBuilder.withSessionId(sessionId: Uuid?) {
    if (sessionId == null) return
    append(DokusParameters.SessionId.apiName, sessionId.toString())
}