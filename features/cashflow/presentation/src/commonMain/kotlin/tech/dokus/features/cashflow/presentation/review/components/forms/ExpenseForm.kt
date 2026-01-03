package tech.dokus.features.cashflow.presentation.review.components.forms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.cashflow_deductible_percentage
import tech.dokus.aura.resources.cashflow_expense_details_section
import tech.dokus.aura.resources.cashflow_is_deductible
import tech.dokus.aura.resources.cashflow_merchant
import tech.dokus.aura.resources.cashflow_payment_method
import tech.dokus.aura.resources.cashflow_receipt_number
import tech.dokus.aura.resources.cashflow_section_additional_information
import tech.dokus.aura.resources.cashflow_section_amounts
import tech.dokus.aura.resources.cashflow_select_category
import tech.dokus.aura.resources.cashflow_select_payment_method
import tech.dokus.aura.resources.cashflow_tax_deductibility
import tech.dokus.aura.resources.cashflow_vat_amount
import tech.dokus.aura.resources.common_currency
import tech.dokus.aura.resources.common_date
import tech.dokus.aura.resources.common_notes
import tech.dokus.aura.resources.invoice_amount
import tech.dokus.aura.resources.invoice_category
import tech.dokus.aura.resources.invoice_description
import tech.dokus.aura.resources.invoice_vat_rate
import tech.dokus.domain.enums.ExpenseCategory
import tech.dokus.domain.enums.PaymentMethod
import tech.dokus.features.cashflow.presentation.review.EditableExpenseFields
import tech.dokus.features.cashflow.presentation.review.ExpenseField
import tech.dokus.foundation.aura.components.fields.PDateField
import tech.dokus.foundation.aura.components.fields.PDropdownField
import tech.dokus.foundation.aura.components.fields.PTextFieldStandard
import tech.dokus.foundation.aura.constrains.Constrains
import tech.dokus.foundation.aura.extensions.localized

@Composable
internal fun ExpenseForm(
    fields: EditableExpenseFields,
    onFieldUpdate: (ExpenseField, Any?) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        SectionHeader(stringResource(Res.string.cashflow_expense_details_section))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_merchant),
            value = fields.merchant,
            onValueChange = { onFieldUpdate(ExpenseField.MERCHANT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PDateField(
            label = stringResource(Res.string.common_date),
            value = fields.date,
            onValueChange = { onFieldUpdate(ExpenseField.DATE, it) }
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_receipt_number),
            value = fields.receiptNumber,
            onValueChange = { onFieldUpdate(ExpenseField.RECEIPT_NUMBER, it) },
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_section_amounts))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_amount),
            value = fields.amount,
            onValueChange = { onFieldUpdate(ExpenseField.AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.cashflow_vat_amount),
            value = fields.vatAmount,
            onValueChange = { onFieldUpdate(ExpenseField.VAT_AMOUNT, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_vat_rate),
            value = fields.vatRate,
            onValueChange = { onFieldUpdate(ExpenseField.VAT_RATE, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_currency),
            value = fields.currency,
            onValueChange = { onFieldUpdate(ExpenseField.CURRENCY, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PDropdownField<ExpenseCategory>(
            label = stringResource(Res.string.invoice_category),
            value = fields.category,
            onValueChange = { onFieldUpdate(ExpenseField.CATEGORY, it) },
            options = ExpenseCategory.values().toList(),
            optionLabel = { category -> category.localized },
            placeholder = stringResource(Res.string.cashflow_select_category),
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_tax_deductibility))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(Res.string.cashflow_is_deductible),
                style = MaterialTheme.typography.bodyMedium
            )
            Switch(
                checked = fields.isDeductible,
                onCheckedChange = { onFieldUpdate(ExpenseField.IS_DEDUCTIBLE, it) }
            )
        }

        if (fields.isDeductible) {
            PTextFieldStandard(
                fieldName = stringResource(Res.string.cashflow_deductible_percentage),
                value = fields.deductiblePercentage,
                onValueChange = { onFieldUpdate(ExpenseField.DEDUCTIBLE_PERCENTAGE, it) },
                modifier = Modifier.fillMaxWidth()
            )
        }

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_payment_method))

        PDropdownField<PaymentMethod>(
            label = stringResource(Res.string.cashflow_payment_method),
            value = fields.paymentMethod,
            onValueChange = { onFieldUpdate(ExpenseField.PAYMENT_METHOD, it) },
            options = PaymentMethod.values().toList(),
            optionLabel = { method -> method.localized },
            placeholder = stringResource(Res.string.cashflow_select_payment_method),
            modifier = Modifier.fillMaxWidth()
        )

        androidx.compose.material3.HorizontalDivider(
            modifier = Modifier.padding(vertical = Constrains.Spacing.small)
        )

        SectionHeader(stringResource(Res.string.cashflow_section_additional_information))

        PTextFieldStandard(
            fieldName = stringResource(Res.string.invoice_description),
            value = fields.description,
            onValueChange = { onFieldUpdate(ExpenseField.DESCRIPTION, it) },
            modifier = Modifier.fillMaxWidth()
        )

        PTextFieldStandard(
            fieldName = stringResource(Res.string.common_notes),
            value = fields.notes,
            onValueChange = { onFieldUpdate(ExpenseField.NOTES, it) },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
