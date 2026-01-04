package tech.dokus.features.cashflow.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.cashflow.presentation.cashflow.components.DroppedFile
import tech.dokus.foundation.app.state.DokusState

/**
 * Contract for document upload screen.
 *
 * Flow:
 * 1. Idle → Ready to accept file uploads
 * 2. Uploading → One or more files are being uploaded
 * 3. Error → Upload failed with recovery option
 *
 * Note: Individual upload task management (cancel, retry, delete) is handled
 * by [DocumentUploadItemState] at the component level. This contract manages
 * the overall screen state.
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
sealed interface AddDocumentState : MVIState, DokusState<Nothing> {

    /**
     * Initial state - ready to accept file uploads.
     */
    data class Idle(
        val hasCompletedUploads: Boolean = false,
        val hasFailedUploads: Boolean = false,
    ) : AddDocumentState

    /**
     * One or more files are being uploaded.
     */
    data class Uploading(
        val hasCompletedUploads: Boolean = false,
        val hasFailedUploads: Boolean = false,
    ) : AddDocumentState

    /**
     * Error state with recovery option.
     */
    data class Error(
        override val exception: DokusException,
        override val retryHandler: RetryHandler,
    ) : AddDocumentState, DokusState.Error<Nothing>
}

// ============================================================================
// INTENTS (User Actions)
// ============================================================================

@Immutable
sealed interface AddDocumentIntent : MVIIntent {
    /** User selected files via file picker */
    data object SelectFile : AddDocumentIntent

    /** User dropped or selected files to upload */
    data class Upload(val files: List<DroppedFile>) : AddDocumentIntent

    /** User clicked cancel/back button */
    data object Cancel : AddDocumentIntent
}

// ============================================================================
// ACTIONS (Side Effects)
// ============================================================================

@Immutable
sealed interface AddDocumentAction : MVIAction {
    /** Launch file picker */
    data object LaunchFilePicker : AddDocumentAction

    /** Navigate back to previous screen */
    data object NavigateBack : AddDocumentAction
}
