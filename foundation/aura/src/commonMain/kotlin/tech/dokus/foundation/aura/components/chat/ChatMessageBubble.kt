package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import tech.dokus.foundation.aura.constrains.Constrains

/**
 * Represents the role of a message sender in a chat conversation.
 */
enum class ChatMessageRole {
    User,
    Assistant
}

/**
 * Default values for ChatMessageBubble components.
 */
object ChatMessageBubbleDefaults {
    val maxBubbleWidth = 280.dp
    val minBubbleWidth = 48.dp
}

/**
 * A chat message bubble component that displays a message with appropriate styling
 * based on the sender's role (user or assistant).
 *
 * User messages appear on the right with primary color styling.
 * Assistant messages appear on the left with surface variant styling.
 *
 * @param message The text content of the message
 * @param role The role of the message sender (User or Assistant)
 * @param modifier Optional modifier for the bubble row
 * @param timestamp Optional timestamp text to display below the message
 */
@Composable
fun ChatMessageBubble(
    message: String,
    role: ChatMessageRole,
    modifier: Modifier = Modifier,
    timestamp: String? = null
) {
    val isUser = role == ChatMessageRole.User
    val bubbleShape = when (role) {
        ChatMessageRole.User -> RoundedCornerShape(
            topStart = Constrains.Spacing.large,
            topEnd = Constrains.Spacing.large,
            bottomStart = Constrains.Spacing.large,
            bottomEnd = Constrains.Spacing.xSmall
        )
        ChatMessageRole.Assistant -> RoundedCornerShape(
            topStart = Constrains.Spacing.xSmall,
            topEnd = Constrains.Spacing.large,
            bottomStart = Constrains.Spacing.large,
            bottomEnd = Constrains.Spacing.large
        )
    }
    val backgroundColor = when (role) {
        ChatMessageRole.User -> MaterialTheme.colorScheme.primary
        ChatMessageRole.Assistant -> MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = when (role) {
        ChatMessageRole.User -> MaterialTheme.colorScheme.onPrimary
        ChatMessageRole.Assistant -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (isUser) {
            Spacer(modifier = Modifier.width(Constrains.Spacing.xxLarge))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(
                min = ChatMessageBubbleDefaults.minBubbleWidth,
                max = ChatMessageBubbleDefaults.maxBubbleWidth
            )
        ) {
            Box(
                modifier = Modifier
                    .clip(bubbleShape)
                    .background(backgroundColor)
                    .padding(
                        horizontal = Constrains.Spacing.medium,
                        vertical = Constrains.Spacing.small
                    )
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor
                )
            }

            if (timestamp != null) {
                Text(
                    text = timestamp,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(
                        horizontal = Constrains.Spacing.xSmall,
                        vertical = Constrains.Spacing.xxSmall
                    )
                )
            }
        }

        if (!isUser) {
            Spacer(modifier = Modifier.width(Constrains.Spacing.xxLarge))
        }
    }
}

/**
 * A user chat message bubble with primary color styling, aligned to the right.
 *
 * @param message The text content of the message
 * @param modifier Optional modifier for the bubble row
 * @param timestamp Optional timestamp text to display below the message
 */
@Composable
fun PUserMessageBubble(
    message: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null
) {
    ChatMessageBubble(
        message = message,
        role = ChatMessageRole.User,
        modifier = modifier,
        timestamp = timestamp
    )
}

/**
 * An assistant chat message bubble with surface variant styling, aligned to the left.
 *
 * @param message The text content of the message
 * @param modifier Optional modifier for the bubble row
 * @param timestamp Optional timestamp text to display below the message
 */
@Composable
fun PAssistantMessageBubble(
    message: String,
    modifier: Modifier = Modifier,
    timestamp: String? = null
) {
    ChatMessageBubble(
        message = message,
        role = ChatMessageRole.Assistant,
        modifier = modifier,
        timestamp = timestamp
    )
}
