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
fun Tenant.Companion.from(row: ResultRow): Tenant = Tenant(
    id = TenantId(row[TenantTable.id].value.toKotlinUuid()),
    type = row[TenantTable.type],
    legalName = LegalName(row[TenantTable.legalName]),
    displayName = DisplayName(row[TenantTable.displayName]),
    subscription = row[TenantTable.plan],
    status = row[TenantTable.status],
    language = row[TenantTable.language],
    vatNumber = VatNumber(row[TenantTable.vatNumber]),
    trialEndsAt = row[TenantTable.trialEndsAt],
    subscriptionStartedAt = row[TenantTable.subscriptionStartedAt],
    createdAt = row[TenantTable.createdAt],
    updatedAt = row[TenantTable.updatedAt],
    websiteUrl = row[TenantTable.websiteUrl]
)

@OptIn(ExperimentalUuidApi::class)
fun TenantSettings.Companion.from(row: ResultRow): TenantSettings = TenantSettings(
    tenantId = TenantId(row[TenantSettingsTable.tenantId].value.toKotlinUuid()),
    invoicePrefix = row[TenantSettingsTable.invoicePrefix],
    nextInvoiceNumber = row[TenantSettingsTable.nextInvoiceNumber],
    defaultPaymentTerms = row[TenantSettingsTable.defaultPaymentTerms],
    defaultVatRate = VatRate.fromDbDecimal(row[TenantSettingsTable.defaultVatRate]),
    invoiceYearlyReset = row[TenantSettingsTable.invoiceYearlyReset],
    invoicePadding = row[TenantSettingsTable.invoicePadding],
    invoiceIncludeYear = row[TenantSettingsTable.invoiceIncludeYear],
    invoiceTimezone = row[TenantSettingsTable.invoiceTimezone],
    companyName = row[TenantSettingsTable.companyName],
    companyIban = row[TenantSettingsTable.companyIban]?.let { Iban(it) },
    companyBic = row[TenantSettingsTable.companyBic]?.let { Bic(it) },
    companyLogoUrl = row[TenantSettingsTable.companyLogoUrl],
    emailInvoiceReminders = row[TenantSettingsTable.emailInvoiceReminders],
    emailPaymentConfirmations = row[TenantSettingsTable.emailPaymentConfirmations],
    emailWeeklyReports = row[TenantSettingsTable.emailWeeklyReports],
    enableBankSync = row[TenantSettingsTable.enableBankSync],
    enablePeppol = row[TenantSettingsTable.enablePeppol],
    paymentTermsText = row[TenantSettingsTable.paymentTermsText],
    cashflowTrackingStartDate = row[TenantSettingsTable.cashflowTrackingStartDate],
    createdAt = row[TenantSettingsTable.createdAt],
    updatedAt = row[TenantSettingsTable.updatedAt]
)

@OptIn(ExperimentalUuidApi::class)
fun Address.Companion.from(row: ResultRow): Address = Address(
    id = AddressId(row[AddressTable.id].value.toKotlinUuid()),
    tenantId = TenantId(row[AddressTable.tenantId].value.toKotlinUuid()),
    streetLine1 = row[AddressTable.streetLine1],
    streetLine2 = row[AddressTable.streetLine2],
    city = row[AddressTable.city],
    postalCode = row[AddressTable.postalCode],
    country = row[AddressTable.country], // Now stored as ISO 3166-1 alpha-2 string
    createdAt = row[AddressTable.createdAt],
    updatedAt = row[AddressTable.updatedAt]
)
