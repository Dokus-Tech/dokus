package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.*
import ai.dokus.foundation.database.utils.toKotlinLocalDate
import ai.dokus.foundation.database.utils.toKotlinLocalDateTime
import ai.dokus.foundation.domain.*
import ai.dokus.foundation.domain.model.*
import org.jetbrains.exposed.sql.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object ClientMapper {

    fun ResultRow.toClient(): Client = Client(
        id = ClientId(this[ClientsTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[ClientsTable.tenantId].value.toKotlinUuid()),
        name = this[ClientsTable.name],
        email = this[ClientsTable.email]?.let { Email(it) },
        vatNumber = this[ClientsTable.vatNumber]?.let { VatNumber(it) },
        addressLine1 = this[ClientsTable.addressLine1],
        addressLine2 = this[ClientsTable.addressLine2],
        city = this[ClientsTable.city],
        postalCode = this[ClientsTable.postalCode],
        country = this[ClientsTable.country],
        contactPerson = this[ClientsTable.contactPerson],
        phone = this[ClientsTable.phone],
        companyNumber = this[ClientsTable.companyNumber],
        defaultPaymentTerms = this[ClientsTable.defaultPaymentTerms],
        defaultVatRate = this[ClientsTable.defaultVatRate]?.let { VatRate(it.toString()) },
        peppolId = this[ClientsTable.peppolId],
        peppolEnabled = this[ClientsTable.peppolEnabled],
        tags = this[ClientsTable.tags],
        notes = this[ClientsTable.notes],
        isActive = this[ClientsTable.isActive],
        createdAt = this[ClientsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[ClientsTable.updatedAt].toKotlinLocalDateTime()
    )
}