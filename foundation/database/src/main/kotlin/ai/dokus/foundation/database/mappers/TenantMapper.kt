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
object TenantMapper {

    fun ResultRow.toTenant(): Tenant = Tenant(
        id = TenantId(this[TenantsTable.id].value.toKotlinUuid()),
        name = this[TenantsTable.name],
        email = this[TenantsTable.email],
        plan = this[TenantsTable.plan],
        status = this[TenantsTable.status],
        trialEndsAt = this[TenantsTable.trialEndsAt]?.toKotlinLocalDateTime(),
        subscriptionStartedAt = this[TenantsTable.subscriptionStartedAt]?.toKotlinLocalDateTime(),
        country = this[TenantsTable.country],
        language = this[TenantsTable.language],
        vatNumber = this[TenantsTable.vatNumber]?.let { VatNumber(it) },
        createdAt = this[TenantsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[TenantsTable.updatedAt].toKotlinLocalDateTime()
    )

    fun ResultRow.toTenantSettings(): TenantSettings = TenantSettings(
        tenantId = TenantId(this[TenantSettingsTable.tenantId].value.toKotlinUuid()),
        invoicePrefix = this[TenantSettingsTable.invoicePrefix],
        nextInvoiceNumber = this[TenantSettingsTable.nextInvoiceNumber],
        defaultPaymentTerms = this[TenantSettingsTable.defaultPaymentTerms],
        defaultVatRate = VatRate(this[TenantSettingsTable.defaultVatRate].toString()),
        companyName = this[TenantSettingsTable.companyName],
        companyAddress = this[TenantSettingsTable.companyAddress],
        companyVatNumber = this[TenantSettingsTable.companyVatNumber]?.let { VatNumber(it) },
        companyIban = this[TenantSettingsTable.companyIban]?.let { Iban(it) },
        companyBic = this[TenantSettingsTable.companyBic]?.let { Bic(it) },
        companyLogoUrl = this[TenantSettingsTable.companyLogoUrl],
        emailInvoiceReminders = this[TenantSettingsTable.emailInvoiceReminders],
        emailPaymentConfirmations = this[TenantSettingsTable.emailPaymentConfirmations],
        emailWeeklyReports = this[TenantSettingsTable.emailWeeklyReports],
        enableBankSync = this[TenantSettingsTable.enableBankSync],
        enablePeppol = this[TenantSettingsTable.enablePeppol],
        createdAt = this[TenantSettingsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[TenantSettingsTable.updatedAt].toKotlinLocalDateTime()
    )
}