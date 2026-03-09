package tech.dokus.app.share

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException

enum class ShareImportPhase { LoadingContext, Uploading, Success, Error }

@Immutable
data class ShareImportState(
    val phase: ShareImportPhase = ShareImportPhase.LoadingContext,
    // Uploading fields
    val currentFileName: String = "",
    val currentFileIndex: Int = 0,
    val totalFiles: Int = 0,
    val workspaceName: String = "",
    val currentFileProgress: Float = 0f,
    val overallProgress: Float = 0f,
    // Success fields
    val primaryFileName: String = "",
    val additionalFileCount: Int = 0,
    val uploadedCount: Int = 0,
    val needsReviewCount: Int = 0,
    val uploadedDocumentIds: List<String> = emptyList(),
    // Error fields
    val exception: DokusException? = null,
    val retryHandler: RetryHandler? = null,
    val canNavigateToLogin: Boolean = false,
    val canOpenApp: Boolean = false,
) : MVIState {
    companion object {
        val initial by lazy { ShareImportState() }
    }
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
        val needsReviewCount: Int = 0,
        val uploadedDocumentIds: List<String>
    ) : ShareImportAction

    data object NavigateToLogin : ShareImportAction
    data object OpenApp : ShareImportAction
}
