package tech.dokus.foundation.aura.extensions

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_source_einvoice_data
import tech.dokus.aura.resources.document_source_structured_einvoice_data
import tech.dokus.aura.resources.document_source_original_document
import tech.dokus.aura.resources.document_source_uploaded_attachment
import tech.dokus.aura.resources.contacts_email
import tech.dokus.aura.resources.contacts_peppol
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.foundation.aura.style.amberWhisper
import tech.dokus.foundation.aura.style.statusWarning
import tech.dokus.foundation.aura.style.textMuted

val DocumentSource.localized: String
    @Composable get() = when (this) {
        DocumentSource.Peppol -> stringResource(Res.string.contacts_peppol)
        DocumentSource.Email -> stringResource(Res.string.contacts_email)
        DocumentSource.Upload -> "PDF"
        DocumentSource.Manual -> "Manual"
    }

val DocumentSource.localizedUppercase: String
    @Composable get() = localized.uppercase()

val DocumentSource.colorized: Color
    @Composable get() = when (this) {
        DocumentSource.Peppol -> MaterialTheme.colorScheme.statusWarning
        DocumentSource.Email,
        DocumentSource.Upload,
        DocumentSource.Manual -> MaterialTheme.colorScheme.textMuted
    }

val DocumentSource.sourceViewerSubtitleLocalized: String
    @Composable get() = when (this) {
        DocumentSource.Peppol -> stringResource(Res.string.document_source_structured_einvoice_data)
        DocumentSource.Email,
        DocumentSource.Upload,
        DocumentSource.Manual -> stringResource(Res.string.document_source_uploaded_attachment)
    }

val DocumentSource.sourceListLabelLocalized: String
    @Composable get() = when (this) {
        DocumentSource.Peppol -> stringResource(Res.string.document_source_einvoice_data)
        DocumentSource.Email,
        DocumentSource.Upload,
        DocumentSource.Manual -> stringResource(Res.string.document_source_original_document)
    }

val DocumentSource.viewerHeaderBackgroundColorized: Color
    @Composable get() = when (this) {
        DocumentSource.Peppol -> MaterialTheme.colorScheme.amberWhisper
        DocumentSource.Email,
        DocumentSource.Upload,
        DocumentSource.Manual -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    }
