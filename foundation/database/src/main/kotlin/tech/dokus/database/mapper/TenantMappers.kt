package tech.dokus.database.mapper

import org.jetbrains.exposed.v1.core.ResultRow
import tech.dokus.database.tables.auth.AddressTable
import tech.dokus.database.tables.auth.TenantSettingsTable
import tech.dokus.database.tables.auth.TenantTable
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.VatRate
import tech.dokus.domain.fromDbDecimal
import tech.dokus.domain.ids.AddressId
import tech.dokus.domain.ids.Bic
import tech.dokus.domain.ids.Iban
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.Address
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.TenantSettings
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
        defaultVatRate = VatRate.fromDbDecimal(this[TenantSettingsTable.defaultVatRate]),
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
        paymentTermsText = this[TenantSettingsTable.paymentTermsText],
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
        country = this[AddressTable.country], // Now stored as ISO 3166-1 alpha-2 string
        createdAt = this[AddressTable.createdAt],
        updatedAt = this[AddressTable.updatedAt]
    )
}
