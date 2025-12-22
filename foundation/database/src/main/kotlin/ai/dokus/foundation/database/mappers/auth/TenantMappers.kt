package ai.dokus.foundation.database.mappers.auth

import ai.dokus.foundation.database.tables.auth.AddressTable
import ai.dokus.foundation.database.tables.auth.TenantSettingsTable
import ai.dokus.foundation.database.tables.auth.TenantTable
import ai.dokus.foundation.domain.DisplayName
import ai.dokus.foundation.domain.LegalName
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.ids.AddressId
import ai.dokus.foundation.domain.ids.Bic
import ai.dokus.foundation.domain.ids.Iban
import ai.dokus.foundation.domain.ids.TenantId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.Address
import ai.dokus.foundation.domain.model.Tenant
import ai.dokus.foundation.domain.model.TenantSettings
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object TenantMappers {

    fun ResultRow.toTenant(): Tenant = Tenant(
        id = TenantId(this[TenantTable.id].value.toKotlinUuid()),
        type = this[TenantTable.type],
        legalName = LegalName(this[TenantTable.legalName]),
        displayName = DisplayName(this[TenantTable.displayName]),
        plan = this[TenantTable.plan],
        status = this[TenantTable.status],
        trialEndsAt = this[TenantTable.trialEndsAt],
        subscriptionStartedAt = this[TenantTable.subscriptionStartedAt],
        language = this[TenantTable.language],
        vatNumber = this[TenantTable.vatNumber]?.let { VatNumber(it) },
        createdAt = this[TenantTable.createdAt],
        updatedAt = this[TenantTable.updatedAt]
    )

    fun ResultRow.toTenantSettings(): TenantSettings = TenantSettings(
        tenantId = TenantId(this[TenantSettingsTable.tenantId].value.toKotlinUuid()),
        invoicePrefix = this[TenantSettingsTable.invoicePrefix],
        nextInvoiceNumber = this[TenantSettingsTable.nextInvoiceNumber],
        defaultPaymentTerms = this[TenantSettingsTable.defaultPaymentTerms],
        defaultVatRate = VatRate(this[TenantSettingsTable.defaultVatRate].toString()),
        invoiceYearlyReset = this[TenantSettingsTable.invoiceYearlyReset],
        invoicePadding = this[TenantSettingsTable.invoicePadding],
        invoiceIncludeYear = this[TenantSettingsTable.invoiceIncludeYear],
        invoiceTimezone = this[TenantSettingsTable.invoiceTimezone],
        companyName = this[TenantSettingsTable.companyName],
        companyIban = this[TenantSettingsTable.companyIban]?.let { Iban(it) },
        companyBic = this[TenantSettingsTable.companyBic]?.let { Bic(it) },
        companyLogoUrl = this[TenantSettingsTable.companyLogoUrl],
        emailInvoiceReminders = this[TenantSettingsTable.emailInvoiceReminders],
        emailPaymentConfirmations = this[TenantSettingsTable.emailPaymentConfirmations],
        emailWeeklyReports = this[TenantSettingsTable.emailWeeklyReports],
        enableBankSync = this[TenantSettingsTable.enableBankSync],
        enablePeppol = this[TenantSettingsTable.enablePeppol],
        createdAt = this[TenantSettingsTable.createdAt],
        updatedAt = this[TenantSettingsTable.updatedAt]
    )

    fun ResultRow.toAddress(): Address = Address(
        id = AddressId(this[AddressTable.id].value.toKotlinUuid()),
        tenantId = TenantId(this[AddressTable.tenantId].value.toKotlinUuid()),
        streetLine1 = this[AddressTable.streetLine1],
        streetLine2 = this[AddressTable.streetLine2],
        city = this[AddressTable.city],
        postalCode = this[AddressTable.postalCode],
        country = this[AddressTable.country],
        createdAt = this[AddressTable.createdAt],
        updatedAt = this[AddressTable.updatedAt]
    )
}
