package tech.dokus.features.cashflow.presentation.review.components.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_bill_details_section
import tech.dokus.aura.resources.cashflow_contact_information
import tech.dokus.aura.resources.cashflow_contact_name
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_section_additional_information
import tech.dokus.aura.resources.cashflow_section_amounts
import tech.dokus.aura.resources.cashflow_vat_amount
import tech.dokus.aura.resources.common_bank_account
import tech.dokus.aura.resources.common_currency
import tech.dokus.aura.resources.common_notes
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_total_amount
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.cashflow.presentation.review.BillField
import tech.dokus.features.cashflow.presentation.review.ContactSuggestion
import tech.dokus.features.cashflow.presentation.review.EditableBillFields
import tech.dokus.foundation.aura.components.fields.PDateField
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun BillForm(
    fields: EditableBillFields,
    onFieldUpdate: (BillField, Any?) -> Unit,
    contactSuggestions: List<ContactSuggestion>,
    onContactSelect: (ContactId) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        SectionHeader(stringResource(Res.string.cashflow_contact_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_contact_name),
            value = fields.supplierName,
            onValueChange = { onFieldUpdate(BillField.SUPPLIER_NAME, it) },
            modifier = Modifier.fillMaxWidth()
        )

        ContactSuggestionsChips(
            suggestions = contactSuggestions,
            selectedContactId = fields.selectedContactId,
            onSelect = onContactSelect
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_vat_number),
            value = fields.supplierVatNumber,
            onValueChange = { onFieldUpdate(BillField.SUPPLIER_VAT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_bill_details_section))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_invoice_number),
            value = fields.invoiceNumber,
            onValueChange = { onFieldUpdate(BillField.INVOICE_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PDateField(
            label = stringResource(Res.string.invoice_issue_date),
            value = fields.issueDate,
            onValueChange = { onFieldUpdate(BillField.ISSUE_DATE, it) }
        )

        PDateField(
            label = stringResource(Res.string.invoice_due_date),
            value = fields.dueDate,
            onValueChange = { onFieldUpdate(BillField.DUE_DATE, it) }
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_section_amounts))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_total_amount),
            value = fields.totalAmount,
            onValueChange = { onFieldUpdate(BillField.TOTAL_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_vat_amount),
            value = fields.vatAmount,
            onValueChange = { onFieldUpdate(BillField.VAT_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_currency),
            value = fields.currency,
            onValueChange = { onFieldUpdate(BillField.CURRENCY, it) },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_section_additional_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_payment_terms),
            value = fields.paymentReference,
            onValueChange = { onFieldUpdate(BillField.PAYMENT_REFERENCE, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_bank_account),
            value = fields.iban,
            onValueChange = { onFieldUpdate(BillField.IBAN, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_notes),
            value = fields.notes,
            onValueChange = { onFieldUpdate(BillField.NOTES, it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
