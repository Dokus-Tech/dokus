package tech.dokus.contacts.components.merge

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.common_empty_value
import tech.dokus.aura.resources.common_percent_value
import tech.dokus.aura.resources.contacts_payment_terms_value

@Composable
internal fun formatConflictValue(fieldName: String, value: String?): String {
    if (value == null) {
        return stringResource(Res.string.common_empty_value)
    }
    return when (fieldName) {
        "defaultPaymentTerms" -> {
            val days = value.toIntOrNull()
            if (days != null) {
                stringResource(Res.string.contacts_payment_terms_value, days)
            } else {
                value
            }
        }
        "defaultVatRate" -> stringResource(Res.string.common_percent_value, value)
        else -> value
    }
}
