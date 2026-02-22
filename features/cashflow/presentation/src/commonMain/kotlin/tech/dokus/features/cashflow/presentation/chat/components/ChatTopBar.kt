package tech.dokus.features.cashflow.presentation.chat.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.chat_collapse_all_citations
import tech.dokus.aura.resources.chat_document_title_fallback
import tech.dokus.aura.resources.chat_expand_all_citations
import tech.dokus.aura.resources.chat_history_action
import tech.dokus.aura.resources.chat_more_options
import tech.dokus.aura.resources.chat_new_conversation
import tech.dokus.aura.resources.chat_scope_all_documents
import tech.dokus.aura.resources.chat_scope_single_document
import tech.dokus.aura.resources.chat_switch_to_all_documents
import tech.dokus.aura.resources.chat_title_all_documents
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.foundation.aura.components.PBackButton
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChatTopBar(
    state: ChatState,
    onBackClick: () -> Unit,
    onNewChat: () -> Unit,
    onShowHistory: () -> Unit,
    onSwitchScope: (ChatScope) -> Unit,
) {
    val content = state as? ChatState.Content
    var showMenu by remember { mutableStateOf(false) }

    Column {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = when {
                            content?.isSingleDocMode == true ->
                                content.documentName
                                    ?: stringResource(Res.string.chat_document_title_fallback)
                            else -> stringResource(Res.string.chat_title_all_documents)
                        },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (content != null) {
                        Text(
                            text = if (content.isSingleDocMode) {
                                stringResource(Res.string.chat_scope_single_document)
                            } else {
                                stringResource(Res.string.chat_scope_all_documents)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            navigationIcon = {
                PBackButton(onBackPress = onBackClick)
            },
            actions = {
                if (content != null) {
                    IconButton(onClick = onNewChat) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.chat_new_conversation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onShowHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(Res.string.chat_history_action),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = stringResource(Res.string.chat_more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            if (content.isSingleDocMode) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(Res.string.chat_switch_to_all_documents)) },
                                    onClick = {
                                        showMenu = false
                                        onSwitchScope(ChatScope.AllDocs)
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.chat_expand_all_citations)) },
                                onClick = { showMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.chat_collapse_all_citations)) },
                                onClick = { showMenu = false }
                            )
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface,
                navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                titleContentColor = MaterialTheme.colorScheme.onSurface,
                actionIconContentColor = MaterialTheme.colorScheme.onSurface
            )
        )
        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            thickness = Constraints.Stroke.thin
        )
    }
}

@Preview
@Composable
private fun ChatTopBarPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ChatTopBar(
            state = ChatState.Loading,
            onBackClick = {},
            onNewChat = {},
            onShowHistory = {},
            onSwitchScope = {},
        )
    }
}
