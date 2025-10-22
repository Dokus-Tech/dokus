package ai.dokus.app.core.extensions

import ai.dokus.foundation.domain.Validatable
import ai.dokus.foundation.domain.ValueClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun <ValueClassType, ValueType> ValueClassType.rememberIsValid(): Boolean where ValueClassType : Validatable<*>, ValueClassType : ValueClass<ValueType> {
    return remember(value) { this.isValid }
}