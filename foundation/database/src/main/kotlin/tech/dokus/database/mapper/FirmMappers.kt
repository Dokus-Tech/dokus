package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.auth.FirmAccessTable
import tech.dokus.database.tables.auth.FirmMembersTable
import tech.dokus.database.tables.auth.FirmsTable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Firm
import tech.dokus.domain.model.FirmAccess
import tech.dokus.domain.model.FirmMembership
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
fun Firm.Companion.from(row: ResultRow): Firm = Firm(
    id = FirmId(row[FirmsTable.id].value.toKotlinUuid()),
    name = DisplayName(row[FirmsTable.name]),
    vatNumber = VatNumber(row[FirmsTable.vatNumber]),
    isActive = row[FirmsTable.isActive],
    createdAt = row[FirmsTable.createdAt],
    updatedAt = row[FirmsTable.updatedAt],
)

@OptIn(ExperimentalUuidApi::class)
fun FirmMembership.Companion.from(row: ResultRow): FirmMembership = FirmMembership(
    userId = UserId(row[FirmMembersTable.userId].value.toKotlinUuid()),
    firmId = FirmId(row[FirmMembersTable.firmId].value.toKotlinUuid()),
    role = row[FirmMembersTable.role],
    isActive = row[FirmMembersTable.isActive],
    createdAt = row[FirmMembersTable.createdAt],
    updatedAt = row[FirmMembersTable.updatedAt],
)

@OptIn(ExperimentalUuidApi::class)
fun FirmAccess.Companion.from(row: ResultRow): FirmAccess = FirmAccess(
    firmId = FirmId(row[FirmAccessTable.firmId].value.toKotlinUuid()),
    tenantId = TenantId(row[FirmAccessTable.tenantId].value.toKotlinUuid()),
    status = row[FirmAccessTable.status],
    grantedByUserId = UserId(row[FirmAccessTable.grantedByUserId].value.toKotlinUuid()),
    createdAt = row[FirmAccessTable.createdAt],
    updatedAt = row[FirmAccessTable.updatedAt],
)
