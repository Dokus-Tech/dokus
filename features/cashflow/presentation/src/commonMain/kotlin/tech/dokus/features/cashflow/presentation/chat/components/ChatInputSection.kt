@file:Suppress("UnusedParameter") // Reserved parameters for future features

package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_input_placeholder
import tech.dokus.aura.resources.chat_message_too_long
import tech.dokus.foundation.aura.components.chat.PChatInputField
import tech.dokus.foundation.aura.constrains.Constrains

@Composable
internal fun ChatInputSection(
    inputText: String,
    canSend: Boolean,
    isSending: Boolean,
    isInputTooLong: Boolean,
    maxLength: Int,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        AnimatedVisibility(
            visible = isInputTooLong,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = stringResource(Res.string.chat_message_too_long, maxLength),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(bottom = Constrains.Spacing.xSmall)
            )
        }

        PChatInputField(
            value = inputText,
            onValueChange = onInputChange,
            onSend = onSend,
            placeholder = stringResource(Res.string.chat_input_placeholder),
            enabled = !isSending,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
