package tech.dokus.features.cashflow.presentation.documents.model

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.document_status_failed
import tech.dokus.aura.resources.documents_local_status_preparing_document
import tech.dokus.aura.resources.documents_local_status_reading_document
import tech.dokus.aura.resources.documents_local_status_uploading

internal val DocumentsLocalUploadRow.Status.localized: String
    @Composable get() = when (this) {
        DocumentsLocalUploadRow.Status.Uploading -> {
            stringResource(Res.string.documents_local_status_uploading)
        }

        DocumentsLocalUploadRow.Status.PreparingDocument -> {
            stringResource(Res.string.documents_local_status_preparing_document)
        }

        DocumentsLocalUploadRow.Status.ReadingDocument -> {
            stringResource(Res.string.documents_local_status_reading_document)
        }

        DocumentsLocalUploadRow.Status.Failed -> {
            stringResource(Res.string.document_status_failed)
        }
    }
