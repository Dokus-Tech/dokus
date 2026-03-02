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
object FirmMappers {

    fun ResultRow.toFirm(): Firm = Firm(
        id = FirmId(this[FirmsTable.id].value.toKotlinUuid()),
        name = DisplayName(this[FirmsTable.name]),
        vatNumber = VatNumber(this[FirmsTable.vatNumber]),
        createdAt = this[FirmsTable.createdAt],
        updatedAt = this[FirmsTable.updatedAt],
    )

    fun ResultRow.toFirmMembership(): FirmMembership = FirmMembership(
        userId = UserId(this[FirmMembersTable.userId].value.toKotlinUuid()),
        firmId = FirmId(this[FirmMembersTable.firmId].value.toKotlinUuid()),
        role = this[FirmMembersTable.role],
        isActive = this[FirmMembersTable.isActive],
        createdAt = this[FirmMembersTable.createdAt],
        updatedAt = this[FirmMembersTable.updatedAt],
    )

    fun ResultRow.toFirmAccess(): FirmAccess = FirmAccess(
        firmId = FirmId(this[FirmAccessTable.firmId].value.toKotlinUuid()),
        tenantId = TenantId(this[FirmAccessTable.tenantId].value.toKotlinUuid()),
        status = this[FirmAccessTable.status],
        grantedByUserId = UserId(this[FirmAccessTable.grantedByUserId].value.toKotlinUuid()),
        createdAt = this[FirmAccessTable.createdAt],
        updatedAt = this[FirmAccessTable.updatedAt],
    )
}
