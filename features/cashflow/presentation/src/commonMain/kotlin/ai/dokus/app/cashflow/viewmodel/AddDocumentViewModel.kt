package ai.dokus.app.cashflow.viewmodel

import ai.dokus.app.cashflow.components.DroppedFile
import ai.dokus.app.cashflow.datasource.CashflowRemoteDataSource
import ai.dokus.app.cashflow.datasource.DocumentUploadResult
import ai.dokus.app.core.viewmodel.BaseViewModel
import ai.dokus.foundation.platform.Logger
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class AddDocumentViewModel : BaseViewModel<AddDocumentViewModel.State>(State.Idle), KoinComponent {

    private val cashflowDataSource: CashflowRemoteDataSource by inject()
    private val logger = Logger.forClass<AddDocumentViewModel>()

    sealed interface State {
        data object Idle : State
        data object Uploading : State
        data class Error(val message: String) : State
        data class Success(val uploadedCount: Int, val uploads: List<DocumentUploadResult> = emptyList()) : State
    }

    fun uploadFiles(files: List<DroppedFile>) = scope.launch {
        if (files.isEmpty()) return@launch
        mutableState.value = State.Uploading

        val results = files.mapNotNull { file ->
            cashflowDataSource.uploadDocument(
                fileContent = file.bytes,
                filename = file.name,
                contentType = file.mimeType ?: mimeTypeFromName(file.name),
                prefix = "documents"
            ).onFailure { logger.e(it) { "Failed to upload ${file.name}" } }.getOrNull()
        }

        mutableState.value = if (results.isNotEmpty()) {
            State.Success(results.size, results)
        } else {
            State.Error("Failed to upload documents. Please try again.")
        }
    }

    fun reset() {
        mutableState.value = State.Idle
    }

    private fun mimeTypeFromName(filename: String): String {
        val lower = filename.lowercase()
        return when {
            lower.endsWith(".pdf") -> "application/pdf"
            lower.endsWith(".png") -> "image/png"
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> "image/jpeg"
            lower.endsWith(".webp") -> "image/webp"
            lower.endsWith(".gif") -> "image/gif"
            lower.endsWith(".csv") -> "text/csv"
            lower.endsWith(".txt") -> "text/plain"
            lower.endsWith(".doc") || lower.endsWith(".docx") ->
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            lower.endsWith(".xls") || lower.endsWith(".xlsx") ->
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            else -> "application/octet-stream"
        }
    }
}
