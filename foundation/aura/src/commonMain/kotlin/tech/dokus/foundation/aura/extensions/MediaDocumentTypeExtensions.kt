package tech.dokus.foundation.aura.extensions

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_type_document
import tech.dokus.aura.resources.document_type_invoice
import tech.dokus.aura.resources.document_type_unknown
import tech.dokus.domain.enums.DocumentType
import tech.dokus.domain.enums.MediaDocumentType

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

// ============================================================================
// DocumentType Extensions (for document processing)
// ============================================================================

/**
 * Extension property to get a localized display name for a DocumentType.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentTypeBadge(type: DocumentType) {
 *     Text(text = type.localized)
 * }
 * ```
 */
val DocumentType.localized: String
    @Composable get() = when (this) {
        DocumentType.Invoice -> stringResource(Res.string.document_type_invoice)
        DocumentType.CreditNote -> stringResource(Res.string.document_type_document) // TODO: Add specific string resource
        DocumentType.Receipt -> stringResource(Res.string.document_type_document) // TODO: Add specific string resource
        DocumentType.ProForma -> stringResource(Res.string.document_type_document) // TODO: Add specific string resource
        else -> stringResource(Res.string.document_type_unknown)
    }

/**
 * Extension property to get the localized document type name in uppercase for display.
 *
 * Usage:
 * ```kotlin
 * @Composable
 * fun DocumentHeader(type: DocumentType) {
 *     Text(text = type.localizedUppercase) // "INVOICE", "EXPENSE", etc.
 * }
 * ```
 */
val DocumentType.localizedUppercase: String
    @Composable get() = localized.uppercase()
