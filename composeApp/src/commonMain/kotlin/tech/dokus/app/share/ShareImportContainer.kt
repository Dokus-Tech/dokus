package tech.dokus.app.share

import kotlinx.coroutines.delay
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.updateStateImmediate
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.exceptions.DokusException
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
    private var activeFiles: List<SharedImportFile> = emptyList()

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

        val sharedFiles = if (activeFiles.isNotEmpty()) {
            activeFiles
        } else {
            ExternalShareImportHandler.consumePendingFiles().orEmpty()
        }
        if (sharedFiles.isEmpty()) {
            logger.w { "Share import opened without pending shared files" }
            updateState { ShareImportState.Error(DokusException.NotFound(), null, false) }
            return
        }
        activeFiles = sharedFiles

        if (!tokenManager.isAuthenticated.value) {
            logger.w { "Share import received while user is not authenticated" }
            updateState {
                ShareImportState.Error(
                    exception = DokusException.NotAuthenticated(),
                    retryHandler = null,
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
                                exception = DokusException.WorkspaceSelectFailed,
                                retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                                canNavigateToLogin = false
                            )
                        }
                    }

                    workspaces.size == 1 -> {
                        uploadToWorkspace(sharedFiles, workspaces.first())
                    }

                    else -> {
                        updateState {
                            ShareImportState.SelectWorkspace(
                                primaryFileName = sharedFiles.first().name,
                                additionalFileCount = (sharedFiles.size - 1).coerceAtLeast(0),
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
                        exception = DokusException.WorkspaceSelectFailed,
                        retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
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
                    exception = DokusException.WorkspaceSelectFailed,
                    retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                    canNavigateToLogin = false
                )
            }
            return
        }

        if (activeFiles.isEmpty()) {
            updateState {
                ShareImportState.Error(
                    exception = DokusException.NotFound(),
                    retryHandler = null,
                    canNavigateToLogin = false
                )
            }
            return
        }

        updateState { selectState.copy(isSwitchingWorkspace = true) }
        uploadToWorkspace(activeFiles, workspace)
    }

    private suspend fun ShareImportCtx.uploadToWorkspace(
        sharedFiles: List<SharedImportFile>,
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
                            exception = DokusException.WorkspaceSelectFailed,
                            retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                            canNavigateToLogin = false
                        )
                    }
                    return
                }
        }

        val totalFiles = sharedFiles.size
        var firstUploadedDocumentId: String? = null

        for ((index, sharedFile) in sharedFiles.withIndex()) {
            val currentFileIndex = index + 1
            updateState {
                ShareImportState.Uploading(
                    currentFileName = sharedFile.name,
                    currentFileIndex = currentFileIndex,
                    totalFiles = totalFiles,
                    workspaceName = workspace.displayName.value,
                    currentFileProgress = 0f,
                    overallProgress = index.toFloat() / totalFiles.toFloat()
                )
            }

            val result = uploadDocumentUseCase(
                fileContent = sharedFile.bytes,
                filename = sharedFile.name,
                contentType = sharedFile.mimeType,
                prefix = "documents",
                onProgress = { progress ->
                    val clampedProgress = progress.coerceIn(0f, 1f)
                    val overallProgress = (index.toFloat() + clampedProgress) / totalFiles.toFloat()

                    // Frequent progress updates can bypass suspended state transactions for responsiveness.
                    updateStateImmediate<ShareImportState.Uploading, _> {
                        copy(
                            currentFileName = sharedFile.name,
                            currentFileIndex = currentFileIndex,
                            totalFiles = totalFiles,
                            currentFileProgress = clampedProgress,
                            overallProgress = overallProgress.coerceIn(0f, 1f)
                        )
                    }
                }
            )

            result.onSuccess { document ->
                if (firstUploadedDocumentId == null) {
                    firstUploadedDocumentId = document.id.toString()
                }
            }.onFailure { error ->
                logger.e(error) { "Share import upload failed for file: ${sharedFile.name}" }
                val exception = (error as? DokusException) ?: DokusException.DocumentUploadFailed
                updateState {
                    ShareImportState.Error(
                        exception = exception,
                        retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                        canNavigateToLogin = false
                    )
                }
                return
            }
        }

        val documentId = firstUploadedDocumentId
        if (documentId == null) {
            logger.w { "Share import completed with no uploaded document id" }
            updateState {
                ShareImportState.Error(
                    exception = DokusException.DocumentUploadFailed,
                    retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                    canNavigateToLogin = false
                )
            }
            return
        }

        updateState {
            ShareImportState.Success(
                primaryFileName = sharedFiles.first().name,
                additionalFileCount = (sharedFiles.size - 1).coerceAtLeast(0),
                uploadedCount = sharedFiles.size,
                documentId = documentId
            )
        }
        delay(SuccessAnimationDelayMs)
        action(ShareImportAction.NavigateToDocumentReview(documentId))
    }
}
