package tech.dokus.app.share

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException

@Immutable
sealed interface ShareImportState : MVIState {
    data object LoadingContext : ShareImportState

    data class Uploading(
        val currentFileName: String,
        val currentFileIndex: Int,
        val totalFiles: Int,
        val workspaceName: String,
        val currentFileProgress: Float,
        val overallProgress: Float,
    ) : ShareImportState

    data class SuccessPulse(
        val primaryFileName: String,
        val additionalFileCount: Int,
        val uploadedCount: Int,
        val uploadedDocumentIds: List<String>
    ) : ShareImportState

    data class Error(
        val exception: DokusException,
        val retryHandler: RetryHandler?,
        val canNavigateToLogin: Boolean,
        val canOpenApp: Boolean = false,
    ) : ShareImportState
}

@Immutable
sealed interface ShareImportIntent : MVIIntent {
    data object Load : ShareImportIntent
    data object Retry : ShareImportIntent
    data object NavigateToLogin : ShareImportIntent
    data object OpenApp : ShareImportIntent
}

@Immutable
sealed interface ShareImportAction : MVIAction {
    data class Finish(
        val successCount: Int,
        val failureCount: Int,
        val uploadedDocumentIds: List<String>
    ) : ShareImportAction

    data object NavigateToLogin : ShareImportAction
    data object OpenApp : ShareImportAction
}
