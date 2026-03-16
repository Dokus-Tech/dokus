package tech.dokus.foundation.aura.components.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.ArrowUp
import com.composables.icons.lucide.Lucide
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_input_placeholder_v2
import tech.dokus.aura.resources.chat_rag_footer
import tech.dokus.aura.resources.chat_send_shortcut
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.amberSoft
import tech.dokus.foundation.aura.style.borderAmber
import tech.dokus.foundation.aura.style.textMuted
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

private val InputBarShape = RoundedCornerShape(9.dp)
private val ButtonSize = 28.dp
private val ButtonCorner = RoundedCornerShape(6.dp)
private val MinInputHeight = 22.dp
private val MaxInputHeight = 100.dp

/**
 * Chat input bar matching v29 design.
 *
 * Features:
 * - Upload button (arrow up) on the left
 * - Auto-growing textarea in the center
 * - Send button (arrow right) on the right — fills amber when text present
 * - Subtle amber border glow on focus
 * - Footer: "All confirmed documents · RAG-powered" left, "⌘ Enter" right
 *
 * @param text Current input text
 * @param onTextChange Text change callback
 * @param onSend Send action — called on button click or Enter key
 * @param onUpload Upload button click
 * @param isSending Whether a message is being sent (disables input)
 * @param maxLength Maximum character count
 */
@Composable
fun ChatInputBar(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onUpload: () -> Unit,
    modifier: Modifier = Modifier,
    isSending: Boolean = false,
    maxLength: Int = 4000,
) {
    var isFocused by remember { mutableStateOf(false) }
    val canSend = text.isNotBlank() && !isSending
    val borderColor = if (isFocused) {
        MaterialTheme.colorScheme.borderAmber
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Constraints.Spacing.xxLarge, vertical = Constraints.Spacing.medium),
    ) {
        // Input container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (isFocused) 8.dp else 4.dp,
                    shape = InputBarShape,
                    ambientColor = if (isFocused) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.03f)
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                    },
                )
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = InputBarShape,
                )
                .border(
                    width = Constraints.Stroke.thin,
                    color = borderColor,
                    shape = InputBarShape,
                )
                .padding(Constraints.Spacing.small),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
        ) {
            // Upload button
            IconButton(
                onClick = onUpload,
                modifier = Modifier.size(ButtonSize),
            ) {
                Icon(
                    imageVector = Lucide.ArrowUp,
                    contentDescription = "Upload document",
                    modifier = Modifier.size(Constraints.IconSize.small),
                    tint = MaterialTheme.colorScheme.textMuted,
                )
            }

            // Text field
            BasicTextField(
                value = text,
                onValueChange = { newText ->
                    if (newText.length <= maxLength) onTextChange(newText)
                },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinInputHeight, max = MaxInputHeight)
                    .onFocusChanged { isFocused = it.isFocused }
                    .padding(vertical = 3.dp),
                enabled = !isSending,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send,
                ),
                keyboardActions = KeyboardActions(
                    onSend = { if (canSend) onSend() },
                ),
                decorationBox = { innerTextField ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.chat_input_placeholder_v2),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.textMuted,
                            )
                        }
                        innerTextField()
                    }
                },
            )

            // Send button — fills amber when text present
            IconButton(
                onClick = { if (canSend) onSend() },
                enabled = canSend,
                modifier = Modifier
                    .size(ButtonSize)
                    .background(
                        color = if (canSend) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)
                        },
                        shape = ButtonCorner,
                    )
                    .then(
                        if (!canSend) {
                            Modifier.border(
                                width = Constraints.Stroke.thin,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = ButtonCorner,
                            )
                        } else {
                            Modifier
                        }
                    ),
            ) {
                Icon(
                    imageVector = Lucide.ArrowRight,
                    contentDescription = "Send",
                    modifier = Modifier.size(Constraints.IconSize.small),
                    tint = if (canSend) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.textMuted
                    },
                )
            }
        }

        // Footer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Constraints.Spacing.xSmall, start = Constraints.Spacing.xxSmall, end = Constraints.Spacing.xxSmall),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(Res.string.chat_rag_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
            )
            Text(
                text = stringResource(Res.string.chat_send_shortcut),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted.copy(alpha = 0.6f),
            )
        }
    }
}

@Preview
@Composable
private fun ChatInputBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatInputBar(
            text = "",
            onTextChange = {},
            onSend = {},
            onUpload = {},
        )
    }
}

@Preview
@Composable
private fun ChatInputBarWithTextPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatInputBar(
            text = "What were my biggest expenses?",
            onTextChange = {},
            onSend = {},
            onUpload = {},
        )
    }
}
