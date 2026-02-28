package tech.dokus.features.cashflow.presentation.cashflow.components.invoice.create.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import tech.dokus.features.cashflow.mvi.CreateInvoiceIntent
import tech.dokus.features.cashflow.mvi.CreateInvoiceState
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.components.background.AmbientBackground
import tech.dokus.foundation.aura.components.chat.ChatMessageRole
import tech.dokus.foundation.aura.components.chat.PAssistantMessageBubble
import tech.dokus.foundation.aura.components.chat.PChatInputField
import tech.dokus.foundation.aura.components.chat.PUserMessageBubble
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.style.dokusEffects
import tech.dokus.foundation.aura.style.dokusSpacing
import tech.dokus.foundation.aura.style.glass
import tech.dokus.foundation.aura.style.glassBorder
import tech.dokus.foundation.aura.style.glassContent
import tech.dokus.foundation.aura.style.glassHeader
import tech.dokus.foundation.aura.style.textMuted

private val PaneTitleBarHeight = Constraints.Height.button + Constraints.Spacing.medium
private val AssistantPaneWidth = 380.dp

private data class AssistantMessage(
    val role: ChatMessageRole,
    val text: String
)

@Composable
internal fun DesktopCreateInvoiceWorkspace(
    state: CreateInvoiceState,
    onIntent: (CreateInvoiceIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
    ) {
        AmbientBackground()

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Constraints.Shell.padding)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(AssistantPaneWidth),
                shape = MaterialTheme.shapes.large,
                color = colorScheme.glass,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    AssistantPaneTopBar(
                        invoiceNumber = state.invoiceNumberPreview,
                        onBackClick = { onIntent(CreateInvoiceIntent.BackClicked) }
                    )
                    AssistantPaneContent()
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = Constraints.Shell.gap),
                shape = MaterialTheme.shapes.large,
                color = colorScheme.glassContent,
                border = BorderStroke(1.dp, colorScheme.glassBorder),
                tonalElevation = 0.dp,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    DesktopPaneTitleBar(
                        title = state.invoiceNumberPreview ?: "New Invoice",
                        subtitle = "Invoice Editor",
                        trailing = "Draft"
                    )
                    Box(modifier = Modifier.fillMaxSize()) {
                        DesktopCreateInvoiceContent(
                            state = state,
                            onIntent = onIntent,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistantPaneTopBar(
    invoiceNumber: String?,
    onBackClick: () -> Unit
) {
    val effects = MaterialTheme.dokusEffects

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = Constraints.Spacing.medium,
                    vertical = Constraints.Spacing.small
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PBackButton(
                label = "Invoices",
                onBackPress = onBackClick
            )
            Text(
                text = invoiceNumber ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted
            )
        }
        HorizontalDivider(color = effects.railTrackLine)
    }
}

@Composable
private fun DesktopPaneTitleBar(
    title: String,
    subtitle: String,
    trailing: String
) {
    val colorScheme = MaterialTheme.colorScheme
    val spacing = MaterialTheme.dokusSpacing
    val effects = MaterialTheme.dokusEffects

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(PaneTitleBarHeight)
                .background(colorScheme.glassHeader)
                .padding(horizontal = spacing.xLarge),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.textMuted
            )
        }
        HorizontalDivider(color = effects.railTrackLine)
    }
}

@Composable
private fun AssistantPaneContent() {
    val messages = remember {
        mutableStateListOf(
            AssistantMessage(
                role = ChatMessageRole.Assistant,
                text = "I can help draft or adjust this invoice while you edit the form."
            ),
            AssistantMessage(
                role = ChatMessageRole.Assistant,
                text = "Try: \"Add a line item for consulting: 2h at EUR 95, VAT 21%.\""
            )
        )
    }
    var input by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Constraints.Spacing.medium, vertical = Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.small)
        ) {
            items(messages) { message ->
                when (message.role) {
                    ChatMessageRole.User -> {
                        PUserMessageBubble(message = message.text)
                    }
                    ChatMessageRole.Assistant -> {
                        PAssistantMessageBubble(message = message.text)
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Constraints.Spacing.medium, vertical = Constraints.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
        ) {
            Text(
                text = "Prompt",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textMuted
            )
            PChatInputField(
                value = input,
                onValueChange = { input = it },
                onSend = {
                    val prompt = input.trim()
                    if (prompt.isBlank()) return@PChatInputField
                    messages += AssistantMessage(role = ChatMessageRole.User, text = prompt)
                    messages += AssistantMessage(
                        role = ChatMessageRole.Assistant,
                        text = "Received. I will map this request to invoice changes."
                    )
                    input = ""
                },
                placeholder = "Ask about this invoice or describe a change..."
            )
        }
    }
}
