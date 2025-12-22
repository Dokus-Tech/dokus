package ai.dokus.app.cashflow.components.upload

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Cancel action button for pending/uploading items.
 *
 * Displays a close icon to cancel the current upload operation.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to apply to the button
 */
@Composable
fun CancelUploadAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Cancel",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Retry action button for failed uploads.
 *
 * Displays a refresh icon to retry the upload operation.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to apply to the button
 */
@Composable
fun RetryUploadAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Retry",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Delete action button for uploaded items.
 *
 * Displays a trash icon to initiate deletion of the uploaded document.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to apply to the button
 */
@Composable
fun DeleteDocumentAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Delete",
            tint = MaterialTheme.colorScheme.error
        )
    }
}

/**
 * Undo action button for items being deleted.
 *
 * Displays an undo icon to cancel the pending deletion.
 *
 * @param onClick Callback invoked when the button is clicked
 * @param modifier Modifier to apply to the button
 */
@Composable
fun UndoDeleteAction(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Undo,
            contentDescription = "Undo delete",
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Combined actions for failed upload state.
 *
 * Displays retry (if available) and cancel/remove buttons together.
 *
 * @param canRetry Whether the retry button should be shown
 * @param onRetry Callback invoked when retry is clicked
 * @param onCancel Callback invoked when cancel is clicked
 * @param modifier Modifier to apply to the row
 */
@Composable
fun FailedUploadActions(
    canRetry: Boolean,
    onRetry: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier) {
        if (canRetry) {
            RetryUploadAction(onClick = onRetry)
        }
        CancelUploadAction(onClick = onCancel)
    }
}
