package tech.dokus.features.cashflow.presentation.review.components.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_contact_information
import tech.dokus.aura.resources.cashflow_contact_name
import tech.dokus.aura.resources.cashflow_invoice_details_section
import tech.dokus.aura.resources.cashflow_invoice_number
import tech.dokus.aura.resources.cashflow_section_additional_information
import tech.dokus.aura.resources.cashflow_section_amounts
import tech.dokus.aura.resources.cashflow_vat_amount
import tech.dokus.aura.resources.common_bank_account
import tech.dokus.aura.resources.common_currency
import tech.dokus.aura.resources.common_notes
import tech.dokus.aura.resources.contacts_address
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_payment_terms
import tech.dokus.aura.resources.contacts_vat_number
import tech.dokus.aura.resources.invoice_due_date
import tech.dokus.aura.resources.invoice_issue_date
import tech.dokus.aura.resources.invoice_subtotal
import tech.dokus.aura.resources.invoice_total_amount
import tech.dokus.domain.ids.ContactId
import tech.dokus.features.cashflow.presentation.review.ContactSuggestion
import tech.dokus.features.cashflow.presentation.review.EditableInvoiceFields
import tech.dokus.features.cashflow.presentation.review.InvoiceField
import tech.dokus.foundation.aura.components.fields.PDateField
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun InvoiceForm(
    fields: EditableInvoiceFields,
    onFieldUpdate: (InvoiceField, Any?) -> Unit,
    contactSuggestions: List<ContactSuggestion>,
    onContactSelect: (ContactId) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        SectionHeader(stringResource(Res.string.cashflow_contact_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_contact_name),
            value = fields.clientName,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_NAME, it) },
            modifier = Modifier.fillMaxWidth()
        )

        ContactSuggestionsChips(
            suggestions = contactSuggestions,
            selectedContactId = fields.selectedContactId,
            onSelect = onContactSelect
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_vat_number),
            value = fields.clientVatNumber,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_VAT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_email),
            value = fields.clientEmail,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_EMAIL, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_address),
            value = fields.clientAddress,
            onValueChange = { onFieldUpdate(InvoiceField.CLIENT_ADDRESS, it) },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_invoice_details_section))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_invoice_number),
            value = fields.invoiceNumber,
            onValueChange = { onFieldUpdate(InvoiceField.INVOICE_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PDateField(
            label = stringResource(Res.string.invoice_issue_date),
            value = fields.issueDate,
            onValueChange = { onFieldUpdate(InvoiceField.ISSUE_DATE, it) }
        )

        PDateField(
            label = stringResource(Res.string.invoice_due_date),
            value = fields.dueDate,
            onValueChange = { onFieldUpdate(InvoiceField.DUE_DATE, it) }
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_section_amounts))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_subtotal),
            value = fields.subtotalAmount,
            onValueChange = { onFieldUpdate(InvoiceField.SUBTOTAL_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_vat_amount),
            value = fields.vatAmount,
            onValueChange = { onFieldUpdate(InvoiceField.VAT_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_total_amount),
            value = fields.totalAmount,
            onValueChange = { onFieldUpdate(InvoiceField.TOTAL_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_currency),
            value = fields.currency,
            onValueChange = { onFieldUpdate(InvoiceField.CURRENCY, it) },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_section_additional_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.contacts_payment_terms),
            value = fields.paymentTerms,
            onValueChange = { onFieldUpdate(InvoiceField.PAYMENT_TERMS, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_bank_account),
            value = fields.bankAccount,
            onValueChange = { onFieldUpdate(InvoiceField.BANK_ACCOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_notes),
            value = fields.notes,
            onValueChange = { onFieldUpdate(InvoiceField.NOTES, it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
