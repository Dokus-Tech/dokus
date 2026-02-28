package tech.dokus.app.share

import kotlinx.coroutines.delay
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.updateStateImmediate
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.usecases.GetLastSelectedTenantIdUseCase
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase
import tech.dokus.foundation.platform.Logger

private const val SuccessAnimationDelayMs = 900L

internal typealias ShareImportCtx =
    PipelineContext<ShareImportState, ShareImportIntent, ShareImportAction>

internal class ShareImportContainer(
    private val tokenManager: TokenManager,
    private val getLastSelectedTenantIdUseCase: GetLastSelectedTenantIdUseCase,
    private val listMyTenantsUseCase: ListMyTenantsUseCase,
    private val selectTenantUseCase: SelectTenantUseCase,
    private val uploadDocumentUseCase: UploadDocumentUseCase,
) : Container<ShareImportState, ShareImportIntent, ShareImportAction> {
    private val logger = Logger.forClass<ShareImportContainer>()
    private var activeFiles: List<SharedImportFile> = emptyList()
    private var uploadSession: UploadSession? = null

    override val store: Store<ShareImportState, ShareImportIntent, ShareImportAction> =
        store(ShareImportState.LoadingContext) {
            reduce { intent ->
                when (intent) {
                    ShareImportIntent.Load -> handleLoad()
                    ShareImportIntent.Retry -> handleRetry()
                    ShareImportIntent.NavigateToLogin -> action(ShareImportAction.NavigateToLogin)
                    ShareImportIntent.OpenApp -> action(ShareImportAction.OpenApp)
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
            updateState {
                ShareImportState.Error(
                    exception = DokusException.NotFound(),
                    retryHandler = null,
                    canNavigateToLogin = false
                )
            }
            return
        }
        activeFiles = sharedFiles
        ensureUploadSession(sharedFiles)

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

        val workspace = resolveWorkspace() ?: return
        uploadToWorkspace(workspace = workspace)
    }

    private suspend fun ShareImportCtx.handleRetry() {
        handleLoad()
    }

    private fun ensureUploadSession(sharedFiles: List<SharedImportFile>) {
        val signatures = sharedFiles.map {
            UploadFileSignature(
                name = it.name,
                mimeType = it.mimeType,
                sizeBytes = it.bytes.size
            )
        }
        val current = uploadSession
        if (current != null && current.signatures == signatures) return
        uploadSession = UploadSession(
            files = sharedFiles,
            signatures = signatures,
            statuses = MutableList(sharedFiles.size) { UploadFileStatus.Pending }
        )
    }

    private suspend fun ShareImportCtx.resolveWorkspace(): Tenant? {
        val workspaces = listMyTenantsUseCase()
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
            .getOrNull()
            ?: return null

        if (workspaces.isEmpty()) {
            updateState {
                ShareImportState.Error(
                    exception = DokusException.WorkspaceSelectFailed,
                    retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                    canNavigateToLogin = false
                )
            }
            return null
        }
        if (workspaces.size == 1) {
            return workspaces.first()
        }

        val selectedTenantId = runCatching { tokenManager.getSelectedTenantId() }
            .getOrNull()
        val lastSelectedTenantId = runCatching { getLastSelectedTenantIdUseCase() }
            .getOrNull()

        val candidates = listOfNotNull(selectedTenantId, lastSelectedTenantId).distinct()
        val resolved = candidates.firstNotNullOfOrNull { candidate ->
            workspaces.firstOrNull { it.id == candidate }
        }

        if (resolved == null) {
            updateState {
                ShareImportState.Error(
                    exception = DokusException.WorkspaceContextUnavailable,
                    retryHandler = RetryHandler { intent(ShareImportIntent.Retry) },
                    canNavigateToLogin = false,
                    canOpenApp = true
                )
            }
            return null
        }

        return resolved
    }

    private suspend fun ShareImportCtx.uploadToWorkspace(workspace: Tenant) {
        val session = uploadSession
        if (session == null || session.files.isEmpty()) {
            updateState {
                ShareImportState.Error(
                    exception = DokusException.NotFound(),
                    retryHandler = null,
                    canNavigateToLogin = false
                )
            }
            return
        }

        val sharedFiles = session.files
        val currentTenantId = runCatching { tokenManager.getSelectedTenantId() }
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
        for (index in sharedFiles.indices) {
            val status = session.statuses[index]
            if (status is UploadFileStatus.Uploaded) continue

            val sharedFile = sharedFiles[index]
            val uploadedCountBefore = session.statuses.count { it is UploadFileStatus.Uploaded }
            val currentFileIndex = index + 1
            updateState {
                ShareImportState.Uploading(
                    currentFileName = sharedFile.name,
                    currentFileIndex = currentFileIndex,
                    totalFiles = totalFiles,
                    workspaceName = workspace.displayName.value,
                    currentFileProgress = 0f,
                    overallProgress = uploadedCountBefore.toFloat() / totalFiles.toFloat()
                )
            }

            val result = uploadDocumentUseCase(
                fileContent = sharedFile.bytes,
                filename = sharedFile.name,
                contentType = sharedFile.mimeType,
                prefix = "documents",
                onProgress = { progress ->
                    val clampedProgress = progress.coerceIn(0f, 1f)
                    val overallProgress = (uploadedCountBefore.toFloat() + clampedProgress) / totalFiles.toFloat()

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

            result.onSuccess { intakeResult ->
                session.statuses[index] = UploadFileStatus.Uploaded(
                    documentId = intakeResult.document.id.toString(),
                    needsReview = intakeResult.intake.outcome == DocumentIntakeOutcome.PendingMatchReview
                )
            }.onFailure { error ->
                logger.e(error) { "Share import upload failed for file: ${sharedFile.name}" }
                val exception = (error as? DokusException) ?: DokusException.DocumentUploadFailed
                session.statuses[index] = UploadFileStatus.Failed(exception)
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

        val uploadedDocumentIds = session.statuses.mapNotNull { status ->
            (status as? UploadFileStatus.Uploaded)?.documentId
        }
        val needsReviewCount = session.statuses.count { status ->
            (status as? UploadFileStatus.Uploaded)?.needsReview == true
        }
        if (uploadedDocumentIds.isEmpty()) {
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
            ShareImportState.SuccessPulse(
                primaryFileName = sharedFiles.first().name,
                additionalFileCount = (sharedFiles.size - 1).coerceAtLeast(0),
                uploadedCount = uploadedDocumentIds.size,
                needsReviewCount = needsReviewCount,
                uploadedDocumentIds = uploadedDocumentIds
            )
        }
        delay(SuccessAnimationDelayMs)
        action(
            ShareImportAction.Finish(
                successCount = uploadedDocumentIds.size,
                failureCount = 0,
                needsReviewCount = needsReviewCount,
                uploadedDocumentIds = uploadedDocumentIds
            )
        )
    }
}

private data class UploadFileSignature(
    val name: String,
    val mimeType: String,
    val sizeBytes: Int
)

private sealed interface UploadFileStatus {
    data object Pending : UploadFileStatus
    data class Uploaded(
        val documentId: String,
        val needsReview: Boolean = false
    ) : UploadFileStatus
    data class Failed(val exception: DokusException) : UploadFileStatus
}

private data class UploadSession(
    val files: List<SharedImportFile>,
    val signatures: List<UploadFileSignature>,
    val statuses: MutableList<UploadFileStatus>
)
