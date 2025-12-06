package ai.dokus.foundation.design.extensions

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.document_type_bill
import ai.dokus.app.resources.generated.document_type_document
import ai.dokus.app.resources.generated.document_type_expense
import ai.dokus.app.resources.generated.document_type_invoice
import ai.dokus.app.resources.generated.document_type_unknown
import ai.dokus.foundation.domain.enums.MediaDocumentType
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource

/**
 * Extension property to get a localized display name for a MediaDocumentType.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentTypeBadge(type: MediaDocumentType) {
 *     Text(text = type.localized)
 * }
 * ```
 */
val MediaDocumentType.localized: String
    @Composable get() = when (this) {
        MediaDocumentType.Invoice -> stringResource(Res.string.document_type_invoice)
        MediaDocumentType.Expense -> stringResource(Res.string.document_type_expense)
        MediaDocumentType.Bill -> stringResource(Res.string.document_type_bill)
        MediaDocumentType.Unknown -> stringResource(Res.string.document_type_unknown)
    }

/**
 * Extension property to get the localized document type name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentHeader(type: MediaDocumentType) {
 *     Text(text = type.localizedUppercase) // "INVOICE", "EXPENSE", etc.
 * }
 * ```
 */
val MediaDocumentType.localizedUppercase: String
    @Composable get() = localized.uppercase()
