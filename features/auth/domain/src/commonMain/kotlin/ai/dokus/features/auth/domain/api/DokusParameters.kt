package ai.dokus.features.auth.domain.api

import ai.dokus.foundation.domain.UserId
import io.ktor.http.Parameters
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import ai.dokus.foundation.domain.SessionId as SessionIdValueClass

enum class DokusParameters(val apiName: String) {
    Id("id"),
    SearchTerm("search"),
    Department("department"),
    Status("status"),
    Limit("limit"),
    Offset("offset"),
    Email("email"),
    UnitCode("unitCode"),
    Role("role"),
    FirstName("firstName"),
    LastName("lastName"),
    SessionId("sessionId");

    @OptIn(ExperimentalUuidApi::class)
    inline operator fun <reified T> invoke(parameters: Parameters): T? {
        val param = parameters[apiName] ?: return null
        return when (T::class) {
            Int::class -> param.toIntOrNull() as T
            String::class -> param as T
            Uuid::class -> Uuid.parse(param) as T
            UserId::class -> UserId(param) as T
            SessionIdValueClass::class -> SessionIdValueClass(param) as T
            else -> param as T
        }
    }
}
