package ai.dokus.foundation.database.mappers

import ai.dokus.foundation.database.tables.TenantsTable
import ai.dokus.foundation.database.tables.TenantSettingsTable
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import kotlinx.datetime.toKotlinLocalDateTime
import org.jetbrains.exposed.sql.ResultRow
import java.util.UUID

object TenantMapper {

    fun ResultRow.toTenant(): Tenant = Tenant(
        id = this[TenantsTable.id].value.toString(),
        name = this[TenantsTable.name],
        email = this[TenantsTable.email],
        plan = this[TenantsTable.plan],
        status = this[TenantsTable.status],
        country = this[TenantsTable.country],
        language = this[TenantsTable.language],
        vatNumber = this[TenantsTable.vatNumber],
        trialEndsAt = this[TenantsTable.trialEndsAt]?.toKotlinLocalDateTime(),
        subscriptionStartedAt = this[TenantsTable.subscriptionStartedAt]?.toKotlinLocalDateTime(),
        createdAt = this[TenantsTable.createdAt].toKotlinLocalDateTime(),
        updatedAt = this[TenantsTable.updatedAt].toKotlinLocalDateTime()
    )

    fun ResultRow.toTenantSettings(): TenantSettings = TenantSettings(
        tenantId = this[TenantSettingsTable.tenantId].value.toString(),
        invoicePrefix = this[TenantSettingsTable.invoicePrefix],
        nextInvoiceNumber = this[TenantSettingsTable.nextInvoiceNumber],
        defaultPaymentTerms = this[TenantSettingsTable.defaultPaymentTerms],
        defaultVatRate = this[TenantSettingsTable.defaultVatRate].toString(),
        companyName = this[TenantSettingsTable.companyName],
        companyAddress = this[TenantSettingsTable.companyAddress],
        companyVatNumber = this[TenantSettingsTable.companyVatNumber],
        companyIban = this[TenantSettingsTable.companyIban],
        companyBic = this[TenantSettingsTable.companyBic],
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