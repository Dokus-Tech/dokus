package tech.dokus.app.share

import kotlinx.coroutines.delay
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.updateStateImmediate
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase
import tech.dokus.foundation.platform.Logger

private const val SuccessAnimationDelayMs = 1200L

internal typealias ShareImportCtx =
    PipelineContext<ShareImportState, ShareImportIntent, ShareImportAction>

internal class ShareImportContainer(
    private val tokenManager: TokenManager,
    private val listMyTenantsUseCase: ListMyTenantsUseCase,
    private val selectTenantUseCase: SelectTenantUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
) : Container<ShareImportState, ShareImportIntent, ShareImportAction> {
    private val logger = Logger.forClass<ShareImportContainer>()
    private var activeFile: SharedImportFile? = null

    override val store: Store<ShareImportState, ShareImportIntent, ShareImportAction> =
        store(ShareImportState.LoadingContext) {
            reduce { intent ->
                when (intent) {
                    ShareImportIntent.Load -> handleLoad()
                    ShareImportIntent.Retry -> handleLoad()
                    ShareImportIntent.NavigateToLogin -> action(ShareImportAction.NavigateToLogin)
                    is ShareImportIntent.SelectWorkspace -> handleSelectWorkspace(intent.tenantId)
                }
            }
        }

    private suspend fun ShareImportCtx.handleLoad() {
        updateState { ShareImportState.LoadingContext }

        val sharedFile = activeFile ?: ExternalShareImportHandler.consumePendingFile()
        if (sharedFile == null) {
            logger.w { "Share import opened without a pending shared file" }
            updateState {
                ShareImportState.Error(
                    title = "No shared document found",
                    message = "Please share a PDF to Dokus again.",
                    canRetry = false,
                    canNavigateToLogin = false
                )
            }
            return
        }
        activeFile = sharedFile

        if (!tokenManager.isAuthenticated.value) {
            logger.w { "Share import received while user is not authenticated" }
            updateState {
                ShareImportState.Error(
                    title = "Login required",
                    message = "Please sign in to upload this document.",
                    canRetry = false,
                    canNavigateToLogin = true
                )
            }
            return
        }

        listMyTenantsUseCase()
            .onSuccess { workspaces ->
                when {
                    workspaces.isEmpty() -> {
                        updateState {
                            ShareImportState.Error(
                                title = "No workspace available",
                                message = "You need at least one workspace to upload documents.",
                                canRetry = true,
                                canNavigateToLogin = false
                            )
                        }
                    }

                    workspaces.size == 1 -> {
                        uploadToWorkspace(sharedFile, workspaces.first())
                    }

                    else -> {
                        updateState {
                            ShareImportState.SelectWorkspace(
                                fileName = sharedFile.name,
                                workspaces = workspaces
                            )
                        }
                    }
                }
            }
            .onFailure { error ->
                logger.e(error) { "Failed to load workspaces for share import" }
                updateState {
                    ShareImportState.Error(
                        title = "Couldn't load workspaces",
                        message = error.message ?: "Please try again.",
                        canRetry = true,
                        canNavigateToLogin = false
                    )
                }
            }
    }

    private suspend fun ShareImportCtx.handleSelectWorkspace(tenantId: TenantId) {
        var currentState: ShareImportState.SelectWorkspace? = null
        withState<ShareImportState.SelectWorkspace, _> {
            currentState = this
        }
        val selectState = currentState ?: return

        val workspace = selectState.workspaces.firstOrNull { it.id == tenantId }
        if (workspace == null) {
            updateState {
                ShareImportState.Error(
                    title = "Workspace not found",
                    message = "Please select another workspace.",
                    canRetry = true,
                    canNavigateToLogin = false
                )
            }
            return
        }

        val sharedFile = activeFile
        if (sharedFile == null) {
            updateState {
                ShareImportState.Error(
                    title = "No shared document found",
                    message = "Please share a PDF to Dokus again.",
                    canRetry = false,
                    canNavigateToLogin = false
                )
            }
            return
        }

        updateState { selectState.copy(isSwitchingWorkspace = true) }
        uploadToWorkspace(sharedFile, workspace)
    }

    private suspend fun ShareImportCtx.uploadToWorkspace(
        sharedFile: SharedImportFile,
        workspace: Tenant
    ) {
        val currentTenantId = runCatching { tokenManager.getCurrentClaims()?.tenant?.tenantId }
            .getOrNull()

        if (currentTenantId != workspace.id) {
            selectTenantUseCase(workspace.id)
                .onFailure { error ->
                    logger.e(error) { "Failed to switch workspace for share import: ${workspace.id}" }
                    updateState {
                        ShareImportState.Error(
                            title = "Couldn't select workspace",
                            message = error.message ?: "Please try again.",
                            canRetry = true,
                            canNavigateToLogin = false
                        )
                    }
                    return
                }
        }

        updateState {
            ShareImportState.Uploading(
                fileName = sharedFile.name,
                workspaceName = workspace.displayName.value,
                progress = 0f
            )
        }

        uploadDocumentUseCase(
            fileContent = sharedFile.bytes,
            filename = sharedFile.name,
            contentType = sharedFile.mimeType,
            prefix = "documents",
            onProgress = { progress ->
                // Frequent progress updates can bypass suspended state transactions for responsiveness.
                updateStateImmediate<ShareImportState.Uploading, _> {
                    copy(progress = progress.coerceIn(0f, 1f))
                }
            }
        ).onSuccess { document ->
            val documentId = document.id.toString()
            updateState {
                ShareImportState.Success(
                    fileName = sharedFile.name,
                    documentId = documentId
                )
            }
            delay(SuccessAnimationDelayMs)
            action(ShareImportAction.NavigateToDocumentReview(documentId))
        }.onFailure { error ->
            logger.e(error) { "Share import upload failed" }
            updateState {
                ShareImportState.Error(
                    title = "Upload failed",
                    message = error.message ?: "Please try again.",
                    canRetry = true,
                    canNavigateToLogin = false
                )
            }
        }
    }
}
