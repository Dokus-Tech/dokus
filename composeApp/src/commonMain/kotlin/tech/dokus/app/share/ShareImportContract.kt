package tech.dokus.app.share

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Tenant

@Immutable
sealed interface ShareImportState : MVIState {
    data object LoadingContext : ShareImportState

    data class SelectWorkspace(
        val primaryFileName: String,
        val additionalFileCount: Int,
        val workspaces: List<Tenant>,
        val isSwitchingWorkspace: Boolean = false
    ) : ShareImportState

    data class Uploading(
        val currentFileName: String,
        val currentFileIndex: Int,
        val totalFiles: Int,
        val workspaceName: String,
        val currentFileProgress: Float,
        val overallProgress: Float,
    ) : ShareImportState

    data class Success(
        val primaryFileName: String,
        val additionalFileCount: Int,
        val uploadedCount: Int,
        val documentId: String
    ) : ShareImportState

    data class Error(
        val exception: DokusException,
        val retryHandler: RetryHandler?,
        val canNavigateToLogin: Boolean
    ) : ShareImportState
}

@Immutable
sealed interface ShareImportIntent : MVIIntent {
    data object Load : ShareImportIntent
    data object Retry : ShareImportIntent
    data object NavigateToLogin : ShareImportIntent
    data class SelectWorkspace(val tenantId: TenantId) : ShareImportIntent
}

@Immutable
sealed interface ShareImportAction : MVIAction {
    data class NavigateToDocumentReview(val documentId: String) : ShareImportAction
    data object NavigateToLogin : ShareImportAction
}
