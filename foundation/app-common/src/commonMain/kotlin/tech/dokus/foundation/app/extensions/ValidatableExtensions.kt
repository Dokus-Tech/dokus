package tech.dokus.foundation.app.extensions

import tech.dokus.domain.Validatable
import tech.dokus.domain.ValueClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun <ValueClassType, ValueType> ValueClassType.rememberIsValid(): Boolean where ValueClassType : Validatable<*>, ValueClassType : ValueClass<ValueType> {
    return remember(value) { this.isValid }
}