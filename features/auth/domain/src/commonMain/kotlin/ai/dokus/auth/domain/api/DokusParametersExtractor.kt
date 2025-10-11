package ai.dokus.auth.domain.api

import ai.dokus.foundation.domain.SessionId
import ai.dokus.foundation.domain.UserId
import io.ktor.http.Parameters
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
val Parameters.userId: UserId? get() = DokusParameters.Id(this)
val Parameters.searchTerm: String? get() = DokusParameters.SearchTerm(this)
val Parameters.department: String? get() = DokusParameters.Department(this)
val Parameters.status: String? get() = DokusParameters.Status(this)
val Parameters.limit: Int? get() = DokusParameters.Limit(this)
val Parameters.offset: Int? get() = DokusParameters.Offset(this)
val Parameters.email: String? get() = DokusParameters.Email(this)
val Parameters.unitCode: String? get() = DokusParameters.UnitCode(this)
val Parameters.role: String? get() = DokusParameters.Role(this)
val Parameters.firstName: String? get() = DokusParameters.FirstName(this)
val Parameters.lastName: String? get() = DokusParameters.LastName(this)

@OptIn(ExperimentalUuidApi::class)
val Parameters.sessionId: SessionId? get() = DokusParameters.SessionId(this)
