@file:OptIn(ExperimentalUuidApi::class)

package tech.dokus.database.repository.business

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.upsert
import tech.dokus.database.tables.business.BusinessProfilesTable
import tech.dokus.domain.enums.BusinessProfileSubjectType
import tech.dokus.domain.enums.BusinessProfileVerificationState
import tech.dokus.domain.ids.TenantId
import tech.dokus.foundation.backend.database.dbQuery
import java.util.UUID
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

data class BusinessProfileRecord(
    val tenantId: TenantId,
    val subjectType: BusinessProfileSubjectType,
    val subjectId: Uuid,
    val websiteUrl: String? = null,
    val businessSummary: String? = null,
    val businessActivitiesJson: String? = null,
    val verificationState: BusinessProfileVerificationState = BusinessProfileVerificationState.Unset,
    val evidenceScore: Int = 0,
    val evidenceChecksJson: String? = null,
    val logoStorageKey: String? = null,
    val websitePinned: Boolean = false,
    val summaryPinned: Boolean = false,
    val activitiesPinned: Boolean = false,
    val logoPinned: Boolean = false,
    val lastRunAt: LocalDateTime? = null,
    val lastErrorCode: String? = null,
    val lastErrorMessage: String? = null,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)

class BusinessProfileRepository {

    suspend fun getBySubject(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
    ): BusinessProfileRecord? = dbQuery {
        BusinessProfilesTable
            .selectAll()
            .where {
                (BusinessProfilesTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BusinessProfilesTable.subjectType eq subjectType) and
                    (BusinessProfilesTable.subjectId eq subjectId.toJavaUuid())
            }
            .singleOrNull()
            ?.toRecord(tenantId)
    }

    suspend fun getBySubjects(
        tenantId: TenantId,
        subjectType: BusinessProfileSubjectType,
        subjectIds: List<Uuid>,
    ): Map<Uuid, BusinessProfileRecord> = dbQuery {
        if (subjectIds.isEmpty()) return@dbQuery emptyMap()
        val subjectJavaIds = subjectIds.map { it.toJavaUuid() }
        BusinessProfilesTable
            .selectAll()
            .where {
                (BusinessProfilesTable.tenantId eq tenantId.value.toJavaUuid()) and
                    (BusinessProfilesTable.subjectType eq subjectType) and
                    (BusinessProfilesTable.subjectId inList subjectJavaIds)
            }
            .associate { row ->
                row[BusinessProfilesTable.subjectId].toKotlinUuid() to row.toRecord(tenantId)
            }
    }

    suspend fun getLogoStorageKey(
        subjectType: BusinessProfileSubjectType,
        subjectId: Uuid,
    ): String? = dbQuery {
        BusinessProfilesTable
            .select(BusinessProfilesTable.logoStorageKey)
            .where {
                (BusinessProfilesTable.subjectType eq subjectType) and
                    (BusinessProfilesTable.subjectId eq subjectId.toJavaUuid())
            }
            .singleOrNull()
            ?.get(BusinessProfilesTable.logoStorageKey)
    }

    suspend fun upsert(record: BusinessProfileRecord): Unit = dbQuery {
        val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
        val tenantUuid = record.tenantId.value.toJavaUuid()
        val subjectUuid = record.subjectId.toJavaUuid()

        BusinessProfilesTable.upsert(
            BusinessProfilesTable.tenantId,
            BusinessProfilesTable.subjectType,
            BusinessProfilesTable.subjectId,
            onUpdate = { stmt ->
                stmt[BusinessProfilesTable.websiteUrl] = record.websiteUrl
                stmt[BusinessProfilesTable.businessSummary] = record.businessSummary
                stmt[BusinessProfilesTable.businessActivitiesJson] = record.businessActivitiesJson
                stmt[BusinessProfilesTable.verificationState] = record.verificationState
                stmt[BusinessProfilesTable.evidenceScore] = record.evidenceScore
                stmt[BusinessProfilesTable.evidenceChecksJson] = record.evidenceChecksJson
                stmt[BusinessProfilesTable.logoStorageKey] = record.logoStorageKey
                stmt[BusinessProfilesTable.websitePinned] = record.websitePinned
                stmt[BusinessProfilesTable.summaryPinned] = record.summaryPinned
                stmt[BusinessProfilesTable.activitiesPinned] = record.activitiesPinned
                stmt[BusinessProfilesTable.logoPinned] = record.logoPinned
                stmt[BusinessProfilesTable.lastRunAt] = record.lastRunAt
                stmt[BusinessProfilesTable.lastErrorCode] = record.lastErrorCode
                stmt[BusinessProfilesTable.lastErrorMessage] = record.lastErrorMessage
                stmt[BusinessProfilesTable.updatedAt] = now
            }
        ) {
            it[BusinessProfilesTable.tenantId] = tenantUuid
            it[BusinessProfilesTable.subjectType] = record.subjectType
            it[BusinessProfilesTable.subjectId] = subjectUuid
            it[BusinessProfilesTable.websiteUrl] = record.websiteUrl
            it[BusinessProfilesTable.businessSummary] = record.businessSummary
            it[BusinessProfilesTable.businessActivitiesJson] = record.businessActivitiesJson
            it[BusinessProfilesTable.verificationState] = record.verificationState
            it[BusinessProfilesTable.evidenceScore] = record.evidenceScore
            it[BusinessProfilesTable.evidenceChecksJson] = record.evidenceChecksJson
            it[BusinessProfilesTable.logoStorageKey] = record.logoStorageKey
            it[BusinessProfilesTable.websitePinned] = record.websitePinned
            it[BusinessProfilesTable.summaryPinned] = record.summaryPinned
            it[BusinessProfilesTable.activitiesPinned] = record.activitiesPinned
            it[BusinessProfilesTable.logoPinned] = record.logoPinned
            it[BusinessProfilesTable.lastRunAt] = record.lastRunAt
            it[BusinessProfilesTable.lastErrorCode] = record.lastErrorCode
            it[BusinessProfilesTable.lastErrorMessage] = record.lastErrorMessage
            it[BusinessProfilesTable.createdAt] = record.createdAt ?: now
            it[BusinessProfilesTable.updatedAt] = now
        }
    }

    private fun org.jetbrains.exposed.v1.core.ResultRow.toRecord(tenantId: TenantId): BusinessProfileRecord = BusinessProfileRecord(
        tenantId = tenantId,
        subjectType = this[BusinessProfilesTable.subjectType],
        subjectId = this[BusinessProfilesTable.subjectId].toKotlinUuid(),
        websiteUrl = this[BusinessProfilesTable.websiteUrl],
        businessSummary = this[BusinessProfilesTable.businessSummary],
        businessActivitiesJson = this[BusinessProfilesTable.businessActivitiesJson],
        verificationState = this[BusinessProfilesTable.verificationState],
        evidenceScore = this[BusinessProfilesTable.evidenceScore],
        evidenceChecksJson = this[BusinessProfilesTable.evidenceChecksJson],
        logoStorageKey = this[BusinessProfilesTable.logoStorageKey],
        websitePinned = this[BusinessProfilesTable.websitePinned],
        summaryPinned = this[BusinessProfilesTable.summaryPinned],
        activitiesPinned = this[BusinessProfilesTable.activitiesPinned],
        logoPinned = this[BusinessProfilesTable.logoPinned],
        lastRunAt = this[BusinessProfilesTable.lastRunAt],
        lastErrorCode = this[BusinessProfilesTable.lastErrorCode],
        lastErrorMessage = this[BusinessProfilesTable.lastErrorMessage],
        createdAt = this[BusinessProfilesTable.createdAt],
        updatedAt = this[BusinessProfilesTable.updatedAt],
    )
}
