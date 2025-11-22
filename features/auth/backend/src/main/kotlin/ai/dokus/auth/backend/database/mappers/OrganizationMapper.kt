package ai.dokus.auth.backend.database.mappers

import ai.dokus.auth.backend.database.tables.*
import ai.dokus.foundation.domain.VatRate
import ai.dokus.foundation.domain.ids.Bic
import ai.dokus.foundation.domain.ids.Iban
import ai.dokus.foundation.domain.ids.OrganizationId
import ai.dokus.foundation.domain.ids.VatNumber
import ai.dokus.foundation.domain.model.*
import org.jetbrains.exposed.v1.core.ResultRow
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.toKotlinUuid

@OptIn(ExperimentalUuidApi::class)
object TenantMapper {

    fun ResultRow.toTenant(): Organization = Organization(
        id = OrganizationId(this[OrganizationTable.id].value.toKotlinUuid()),
        name = this[OrganizationTable.name],
        email = this[OrganizationTable.email],
        plan = this[OrganizationTable.plan],
        status = this[OrganizationTable.status],
        trialEndsAt = this[OrganizationTable.trialEndsAt],
        subscriptionStartedAt = this[OrganizationTable.subscriptionStartedAt],
        country = this[OrganizationTable.country],
        language = this[OrganizationTable.language],
        vatNumber = this[OrganizationTable.vatNumber]?.let { VatNumber(it) },
        createdAt = this[OrganizationTable.createdAt],
        updatedAt = this[OrganizationTable.updatedAt]
    )

    fun ResultRow.toOrganizationSettings(): OrganizationSettings = OrganizationSettings(
        organizationId = OrganizationId(this[OrganizationSettingsTable.organizationId].value.toKotlinUuid()),
        invoicePrefix = this[OrganizationSettingsTable.invoicePrefix],
        nextInvoiceNumber = this[OrganizationSettingsTable.nextInvoiceNumber],
        defaultPaymentTerms = this[OrganizationSettingsTable.defaultPaymentTerms],
        defaultVatRate = VatRate(this[OrganizationSettingsTable.defaultVatRate].toString()),
        companyName = this[OrganizationSettingsTable.companyName],
        companyAddress = this[OrganizationSettingsTable.companyAddress],
        companyVatNumber = this[OrganizationSettingsTable.companyVatNumber]?.let { VatNumber(it) },
        companyIban = this[OrganizationSettingsTable.companyIban]?.let { Iban(it) },
        companyBic = this[OrganizationSettingsTable.companyBic]?.let { Bic(it) },
        companyLogoUrl = this[OrganizationSettingsTable.companyLogoUrl],
        emailInvoiceReminders = this[OrganizationSettingsTable.emailInvoiceReminders],
        emailPaymentConfirmations = this[OrganizationSettingsTable.emailPaymentConfirmations],
        emailWeeklyReports = this[OrganizationSettingsTable.emailWeeklyReports],
        enableBankSync = this[OrganizationSettingsTable.enableBankSync],
        enablePeppol = this[OrganizationSettingsTable.enablePeppol],
        createdAt = this[OrganizationSettingsTable.createdAt],
        updatedAt = this[OrganizationSettingsTable.updatedAt]
    )
}