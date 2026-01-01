package tech.dokus.features.cashflow.presentation.cashflow.model

import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.utils.currentTimeMillis
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Handle for a pending document deletion with undo capability.
 *
 * Tracks the deletion countdown and allows cancellation (undo) before
 * the deletion is actually executed.
 *
 * @property documentId ID of the document being deleted
 * @property fileName Name of the file for display
 * @property fileSize Size of the file in bytes
 * @property job Coroutine job that will execute the deletion
 * @property startedAt Epoch milliseconds when deletion was initiated
 * @property duration Total duration of the undo window
 * @property resultDeferred Deferred result of the deletion operation
 */
data class DocumentDeletionHandle(
    val documentId: DocumentId,
    val fileName: String,
    val fileSize: Long,
    val job: Job,
    val startedAt: Long = currentTimeMillis(),
    val duration: Duration = 5.seconds,
    val resultDeferred: CompletableDeferred<Result<Unit>> = CompletableDeferred()
) {
    /**
     * Returns the current progress fraction from 1.0 (just started) to 0.0 (expired).
     */
    fun progressFraction(): Float {
        val elapsed = currentTimeMillis() - startedAt
        val remaining = duration.inWholeMilliseconds - elapsed
        return (remaining.toFloat() / duration.inWholeMilliseconds).coerceIn(0f, 1f)
    }

    /**
     * Whether the undo window has expired.
     */
    fun isExpired(): Boolean = progressFraction() <= 0f

    /**
     * Cancels the deletion, preventing it from being executed.
     */
    fun cancel() {
        job.cancel()
    }

    /**
     * Suspends until the deletion completes and returns the result.
     */
    suspend fun awaitResult(): Result<Unit> = resultDeferred.await()
}
