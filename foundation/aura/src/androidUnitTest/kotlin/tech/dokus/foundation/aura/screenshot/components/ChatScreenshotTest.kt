package tech.dokus.foundation.aura.screenshot.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.junit.Rule
import org.junit.Test
import tech.dokus.foundation.aura.components.chat.ChatMessageBubble
import tech.dokus.foundation.aura.components.chat.ChatMessageRole
import tech.dokus.foundation.aura.components.chat.PAssistantMessageBubble
import tech.dokus.foundation.aura.components.chat.PChatInputField
import tech.dokus.foundation.aura.components.chat.PUserMessageBubble
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper
import tech.dokus.foundation.aura.screenshot.ScreenshotTestHelper.snapshotBothThemes
import tech.dokus.foundation.aura.screenshot.ScreenshotViewport

class ChatScreenshotTest {

    @get:Rule
    val paparazzi = ScreenshotTestHelper.createPaparazzi(ScreenshotViewport.MEDIUM)

    @Test
    fun pChatInputField_empty() {
        paparazzi.snapshotBothThemes("PChatInputField_empty") {
            PChatInputField(
                value = "",
                onValueChange = {},
                onSend = {},
                placeholder = "Type a message...",
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    @Test
    fun pChatInputField_withText() {
        paparazzi.snapshotBothThemes("PChatInputField_withText") {
            PChatInputField(
                value = "Hello, how can I help you?",
                onValueChange = {},
                onSend = {},
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    @Test
    fun pChatInputField_disabled() {
        paparazzi.snapshotBothThemes("PChatInputField_disabled") {
            PChatInputField(
                value = "Sending...",
                onValueChange = {},
                onSend = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
    }

    @Test
    fun chatMessageBubble_user() {
        paparazzi.snapshotBothThemes("ChatMessageBubble_user") {
            ChatMessageBubble(
                message = "Hello, I have a question about my invoice.",
                role = ChatMessageRole.User,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun chatMessageBubble_assistant() {
        paparazzi.snapshotBothThemes("ChatMessageBubble_assistant") {
            ChatMessageBubble(
                message = "Of course! I'd be happy to help. What would you like to know?",
                role = ChatMessageRole.Assistant,
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun chatMessageBubble_withTimestamp() {
        paparazzi.snapshotBothThemes("ChatMessageBubble_withTimestamp") {
            ChatMessageBubble(
                message = "This message has a timestamp",
                role = ChatMessageRole.User,
                timestamp = "10:30 AM",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun pUserMessageBubble() {
        paparazzi.snapshotBothThemes("PUserMessageBubble") {
            PUserMessageBubble(
                message = "User message bubble",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun pAssistantMessageBubble() {
        paparazzi.snapshotBothThemes("PAssistantMessageBubble") {
            PAssistantMessageBubble(
                message = "Assistant message bubble",
                modifier = Modifier.padding(16.dp)
            )
        }
    }

    @Test
    fun chatConversation() {
        paparazzi.snapshotBothThemes("ChatConversation") {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PAssistantMessageBubble(
                    message = "Hello! How can I assist you today?",
                    timestamp = "10:00 AM"
                )
                PUserMessageBubble(
                    message = "I need help with my invoice.",
                    timestamp = "10:01 AM"
                )
                PAssistantMessageBubble(
                    message = "I can help with that. What specific issue are you experiencing?",
                    timestamp = "10:01 AM"
                )
                PUserMessageBubble(
                    message = "The total amount seems incorrect.",
                    timestamp = "10:02 AM"
                )
            }
        }
    }
}
