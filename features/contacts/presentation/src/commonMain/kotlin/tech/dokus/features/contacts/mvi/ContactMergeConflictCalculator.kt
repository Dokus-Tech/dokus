package tech.dokus.features.contacts.mvi

import org.jetbrains.compose.resources.StringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.contacts_address_line1
import tech.dokus.aura.resources.contacts_address_line2
import tech.dokus.aura.resources.contacts_city
import tech.dokus.aura.resources.contacts_company_number
import tech.dokus.aura.resources.contacts_contact_person
import tech.dokus.aura.resources.contacts_country
import tech.dokus.aura.resources.contacts_default_vat_rate
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_peppol_id
import tech.dokus.aura.resources.contacts_phone
import tech.dokus.aura.resources.contacts_postal_code
import tech.dokus.aura.resources.contacts_tags
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.domain.model.contact.ContactDto
import tech.dokus.features.contacts.presentation.contacts.model.MergeFieldConflict

internal object ContactMergeConflictCalculator {
    fun compute(source: ContactDto, target: ContactDto): List<MergeFieldConflict> {
        val conflicts = mutableListOf<MergeFieldConflict>()

        fun addConflictIfDifferent(
            fieldName: String,
            fieldLabelRes: StringResource,
            sourceValue: String?,
            targetValue: String?
        ) {
            if (sourceValue != null && targetValue != null && sourceValue != targetValue) {
                conflicts.add(
                    MergeFieldConflict(
                        fieldName = fieldName,
                        fieldLabelRes = fieldLabelRes,
                        sourceValue = sourceValue,
                        targetValue = targetValue,
                        keepSource = false
                    )
                )
            }
        }

        addConflictIfDifferent("email", Res.string.contacts_email, source.email?.value, target.email?.value)
        addConflictIfDifferent("phone", Res.string.contacts_phone, source.phone?.value, target.phone?.value)
        addConflictIfDifferent(
            "vatNumber",
            Res.string.contacts_vat_number,
            source.vatNumber?.value,
            target.vatNumber?.value
        )
        addConflictIfDifferent(
            "companyNumber",
            Res.string.contacts_company_number,
            source.companyNumber,
            target.companyNumber
        )
        addConflictIfDifferent(
            "contactPerson",
            Res.string.contacts_contact_person,
            source.contactPerson,
            target.contactPerson
        )
        addConflictIfDifferent(
            "addressLine1",
            Res.string.contacts_address_line1,
            source.addressLine1,
            target.addressLine1
        )
        addConflictIfDifferent(
            "addressLine2",
            Res.string.contacts_address_line2,
            source.addressLine2,
            target.addressLine2
        )
        addConflictIfDifferent("city", Res.string.contacts_city, source.city?.value, target.city?.value)
        addConflictIfDifferent("postalCode", Res.string.contacts_postal_code, source.postalCode, target.postalCode)
        addConflictIfDifferent("country", Res.string.contacts_country, source.country, target.country)
        addConflictIfDifferent("peppolId", Res.string.contacts_peppol_id, source.peppolId, target.peppolId)
        addConflictIfDifferent("tags", Res.string.contacts_tags, source.tags, target.tags)

        if (source.defaultPaymentTerms != target.defaultPaymentTerms) {
            conflicts.add(
                MergeFieldConflict(
                    fieldName = "defaultPaymentTerms",
                    fieldLabelRes = Res.string.contacts_payment_terms,
                    sourceValue = source.defaultPaymentTerms.toString(),
                    targetValue = target.defaultPaymentTerms.toString(),
                    keepSource = false
                )
            )
        }

        val sourceVatRate = source.defaultVatRate?.toString()
        val targetVatRate = target.defaultVatRate?.toString()
        if (sourceVatRate != null && targetVatRate != null && sourceVatRate != targetVatRate) {
            conflicts.add(
                MergeFieldConflict(
                    fieldName = "defaultVatRate",
                    fieldLabelRes = Res.string.contacts_default_vat_rate,
                    sourceValue = sourceVatRate,
                    targetValue = targetVatRate,
                    keepSource = false
                )
            )
        }

        return conflicts
    }
}
