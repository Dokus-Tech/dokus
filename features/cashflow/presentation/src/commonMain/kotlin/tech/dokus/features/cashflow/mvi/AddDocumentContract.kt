package tech.dokus.features.cashflow.mvi

import androidx.compose.runtime.Immutable
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import tech.dokus.features.cashflow.presentation.cashflow.components.DroppedFile

/**
 * Contract for document upload screen.
 *
 * Note: Individual upload task management (cancel, retry, delete) is handled
 * by [DocumentUploadItemState] at the component level. This contract manages
 * the overall screen state.
 */

// ============================================================================
// STATE
// ============================================================================

@Immutable
data class AddDocumentState(
    val isUploading: Boolean = false,
    val hasCompletedUploads: Boolean = false,
    val hasFailedUploads: Boolean = false,
) : MVIState {
    companion object {
        val initial by lazy { AddDocumentState() }
    }
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
