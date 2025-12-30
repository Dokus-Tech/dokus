package tech.dokus.foundation.aura.components.chat

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.chat_send_message
import tech.dokus.foundation.aura.constrains.Constrains
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.stringResource

/**
 * Default values for ChatInputField components.
 */
object ChatInputFieldDefaults {
    const val DEFAULT_MAX_LINES = 4
}

/**
 * A chat input field component with a send button for composing messages.
 *
 * Features:
 * - Multi-line text input with configurable max lines
 * - Send button that becomes active when there is text to send
 * - Optional placeholder text
 * - Focus state with visual feedback
 * - Keyboard action support for sending messages
 *
 * @param value The current text value of the input field
 * @param onValueChange Callback invoked when the text value changes
 * @param onSend Callback invoked when the send button is clicked or keyboard action is triggered
 * @param modifier Optional modifier for the input field container
 * @param placeholder Optional placeholder text shown when the field is empty
 * @param enabled Whether the input field is enabled for interaction
 * @param maxLines Maximum number of lines for the text input before scrolling
 */
@Composable
fun PChatInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    enabled: Boolean = true,
    maxLines: Int = ChatInputFieldDefaults.DEFAULT_MAX_LINES
) {
    var isFocused by remember { mutableStateOf(false) }
    val canSend = value.isNotBlank() && enabled

    Row(
        modifier = modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.hasFocus
            }
            .border(
                width = Constrains.Stroke.thin,
                color = when {
                    isFocused -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.outline
                },
                shape = MaterialTheme.shapes.medium
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium
            )
            .padding(
                start = Constrains.Spacing.large,
                end = Constrains.Spacing.small,
                top = Constrains.Spacing.small,
                bottom = Constrains.Spacing.small
            ),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = Constrains.Spacing.small)
        ) {
            if (value.isEmpty() && placeholder.isNotEmpty()) {
                Text(
                    text = placeholder,
                    style = LocalTextStyle.current.copy(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                )
            }

            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 16.sp,
                    color = when {
                        !enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                ),
                singleLine = false,
                maxLines = maxLines,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardActions = KeyboardActions(
                    onSend = {
                        if (canSend) {
                            onSend()
                        }
                    }
                ),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Send
                ),
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }

        IconButton(
            onClick = {
                if (canSend) {
                    onSend()
                }
            },
            enabled = canSend,
            modifier = Modifier.size(Constrains.IconSize.large),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = MaterialTheme.colorScheme.primary,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = stringResource(Res.string.chat_send_message),
                modifier = Modifier.size(Constrains.IconSize.medium)
            )
        }
    }
}
