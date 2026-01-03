package tech.dokus.features.contacts.presentation.contacts.model

import org.jetbrains.compose.resources.StringResource

internal enum class MergeDialogStep {
    SelectTarget,
    CompareFields,
    Confirmation,
    Result
}

internal data class MergeFieldConflict(
    val fieldName: String,
    val fieldLabelRes: StringResource,
    val sourceValue: String?,
    val targetValue: String?,
    val keepSource: Boolean = false
)
