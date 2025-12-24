package ai.dokus.foundation.design.components

import ai.dokus.foundation.design.constrains.Constrains
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Banner that displays offline status warning.
 * Shows when the device is offline with an option to retry.
 *
 * @param isOffline Whether the device is currently offline
 * @param onRetryClick Callback when user clicks retry
 * @param modifier Optional modifier
 */
@Composable
fun OfflineBanner(
    isOffline: Boolean,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isOffline,
        enter = expandVertically(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(
                    horizontal = Constrains.Spacing.medium,
                    vertical = Constrains.Spacing.small
                ),
            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = "Offline",
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "You're offline",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (onRetryClick != null) {
                IconButton(
                    onClick = onRetryClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Retry",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Text component that displays when data was last synced.
 *
 * @param lastSyncTimeMillis Epoch milliseconds of last sync (null if never synced)
 * @param modifier Optional modifier
 */
@Composable
fun LastUpdatedText(
    lastSyncTimeMillis: Long?,
    modifier: Modifier = Modifier
) {
    if (lastSyncTimeMillis != null) {
        val timeAgo = formatTimeAgo(lastSyncTimeMillis)
        Text(
            text = "Updated $timeAgo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

/**
 * Combined sync status component with offline banner and last updated text.
 *
 * @param isOffline Whether the device is currently offline
 * @param lastSyncTimeMillis Epoch milliseconds of last sync (null if never synced)
 * @param onRetryClick Callback when user clicks retry
 * @param modifier Optional modifier
 */
@Composable
fun SyncStatusBanner(
    isOffline: Boolean,
    lastSyncTimeMillis: Long?,
    onRetryClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        OfflineBanner(
            isOffline = isOffline,
            onRetryClick = onRetryClick
        )

        // Show "last updated" only when we have cached data and are offline
        if (isOffline && lastSyncTimeMillis != null) {
            LastUpdatedText(
                lastSyncTimeMillis = lastSyncTimeMillis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = Constrains.Spacing.medium,
                        vertical = Constrains.Spacing.xSmall
                    )
            )
        }
    }
}

/**
 * Format epoch milliseconds to a human-readable "time ago" string.
 */
private fun formatTimeAgo(epochMillis: Long): String {
    val now = currentTimeMillis()
    val diffMillis = now - epochMillis

    val seconds = diffMillis / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hour${if (hours > 1) "s" else ""} ago"
        days < 7 -> "$days day${if (days > 1) "s" else ""} ago"
        else -> "over a week ago"
    }
}

/**
 * Get current time in epoch milliseconds.
 * Uses kotlin.time.Clock which is available in Kotlin 2.0+
 */
@OptIn(kotlin.time.ExperimentalTime::class)
private fun currentTimeMillis(): Long {
    return kotlin.time.Clock.System.now().toEpochMilliseconds()
}
