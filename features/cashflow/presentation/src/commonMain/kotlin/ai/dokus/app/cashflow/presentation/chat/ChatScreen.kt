package ai.dokus.app.cashflow.presentation.chat

import ai.dokus.app.resources.generated.Res
import ai.dokus.app.resources.generated.action_cancel
import ai.dokus.app.resources.generated.chat_collapse_all_citations
import ai.dokus.app.resources.generated.chat_document_title_fallback
import ai.dokus.app.resources.generated.chat_empty_description_all
import ai.dokus.app.resources.generated.chat_empty_description_single
import ai.dokus.app.resources.generated.chat_example_due_date
import ai.dokus.app.resources.generated.chat_example_format
import ai.dokus.app.resources.generated.chat_example_invoices_company
import ai.dokus.app.resources.generated.chat_example_spend_last_month
import ai.dokus.app.resources.generated.chat_example_total_amount
import ai.dokus.app.resources.generated.chat_expand_all_citations
import ai.dokus.app.resources.generated.chat_general_chat
import ai.dokus.app.resources.generated.chat_history_action
import ai.dokus.app.resources.generated.chat_history_empty
import ai.dokus.app.resources.generated.chat_history_title
import ai.dokus.app.resources.generated.chat_input_placeholder
import ai.dokus.app.resources.generated.chat_loading
import ai.dokus.app.resources.generated.chat_message_count_plural
import ai.dokus.app.resources.generated.chat_message_count_single
import ai.dokus.app.resources.generated.chat_message_too_long
import ai.dokus.app.resources.generated.chat_more_options
import ai.dokus.app.resources.generated.chat_new_chat
import ai.dokus.app.resources.generated.chat_new_conversation
import ai.dokus.app.resources.generated.chat_prompt_all_documents
import ai.dokus.app.resources.generated.chat_prompt_single_document
import ai.dokus.app.resources.generated.chat_scope_all_documents
import ai.dokus.app.resources.generated.chat_scope_single_document
import ai.dokus.app.resources.generated.chat_switch_to_all_documents
import ai.dokus.app.resources.generated.chat_thinking
import ai.dokus.app.resources.generated.chat_this_document
import ai.dokus.app.resources.generated.chat_title_all_documents
import ai.dokus.app.resources.generated.chat_try_asking
import ai.dokus.foundation.design.components.PBackButton
import ai.dokus.foundation.design.components.chat.ChatMessageBubble
import ai.dokus.foundation.design.components.chat.ChatMessageRole
import ai.dokus.foundation.design.components.chat.ChatSourceCitationList
import ai.dokus.foundation.design.components.chat.CitationDisplayData
import ai.dokus.foundation.design.components.chat.PChatInputField
import ai.dokus.foundation.design.components.common.DokusErrorContent
import ai.dokus.foundation.design.constrains.Constrains
import ai.dokus.foundation.design.extensions.localized
import ai.dokus.foundation.design.local.LocalScreenSize
import ai.dokus.foundation.design.local.isLarge
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionSummary
import tech.dokus.domain.model.ai.MessageRole
import ai.dokus.foundation.navigation.local.LocalNavController
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import compose.icons.FeatherIcons
import compose.icons.feathericons.MessageCircle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.stringResource
import pro.respawn.flowmvi.compose.dsl.DefaultLifecycle
import pro.respawn.flowmvi.compose.dsl.subscribe
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.foundation.app.mvi.container

/**
 * Chat Screen for RAG-powered document Q&A.
 *
 * Features:
 * - Single-document chat: Ask questions about a specific document
 * - Cross-document chat: Ask questions across all confirmed documents
 * - Message bubbles with user/assistant styling
 * - Expandable source citations showing document excerpts
 * - Session management for conversation history
 * - Scope selection (single doc vs all docs)
 *
 * Layout:
 * - Mobile: Full-screen chat with input at bottom
 * - Desktop: Same layout with wider message area
 */
@Composable
internal fun ChatScreen(
    documentId: DocumentId? = null,
    container: ChatContainer = container(),
) {
    val navController = LocalNavController.current
    val isLargeScreen = LocalScreenSize.isLarge
    val snackbarHostState = remember { SnackbarHostState() }
    var pendingError by remember { mutableStateOf<DokusException?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val errorMessage = pendingError?.localized

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage)
            pendingError = null
        }
    }

    // Subscribe to state and handle actions
    val state by container.store.subscribe(DefaultLifecycle) { action ->
        when (action) {
            is ChatAction.NavigateBack -> {
                navController.popBackStack()
            }

            is ChatAction.NavigateToDocumentReview -> {
                // TODO: Navigate to document review screen
            }

            is ChatAction.NavigateToDocumentPreview -> {
                // TODO: Navigate to document preview with page number
            }

            is ChatAction.ShowError -> {
                pendingError = action.error
            }

            is ChatAction.ShowSuccess -> {
                scope.launch {
                    snackbarHostState.showSnackbar(action.message)
                }
            }

            is ChatAction.ShowInfo -> {
                scope.launch {
                    snackbarHostState.showSnackbar(action.message)
                }
            }

            is ChatAction.ScrollToBottom -> {
                // Scroll is handled in a LaunchedEffect based on message count changes
            }

            is ChatAction.FocusInput -> {
                // Focus is handled by the input field
            }

            is ChatAction.DismissKeyboard -> {
                // Keyboard dismissal is platform-specific
            }
        }
    }

    // Initialize chat based on documentId
    LaunchedEffect(documentId) {
        if (documentId != null) {
            container.store.intent(ChatIntent.InitSingleDocChat(documentId))
        } else {
            container.store.intent(ChatIntent.InitCrossDocChat)
        }
    }

    // Auto-scroll to bottom when new messages arrive
    val contentState = state as? ChatState.Content
    val messageCount = contentState?.messages?.size ?: 0
    LaunchedEffect(messageCount) {
        if (messageCount > 0) {
            listState.animateScrollToItem(messageCount - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopBar(
                state = state,
                onBackClick = { navController.popBackStack() },
                onNewChat = { container.store.intent(ChatIntent.StartNewConversation) },
                onShowHistory = { container.store.intent(ChatIntent.ShowSessionPicker) },
                onSwitchScope = { newScope ->
                    when (newScope) {
                        ChatScope.SingleDoc -> {
                            // For single-doc, we need a document ID
                            // This should navigate to document picker or use current doc
                        }

                        ChatScope.AllDocs -> {
                            container.store.intent(ChatIntent.SwitchToCrossDoc)
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { contentPadding ->
        when (state) {
            is ChatState.Loading -> {
                LoadingContent(contentPadding)
            }

            is ChatState.Content -> {
                val content = state as ChatState.Content
                ChatContent(
                    state = content,
                    contentPadding = contentPadding,
                    listState = listState,
                    isLargeScreen = isLargeScreen,
                    onIntent = { container.store.intent(it) }
                )
            }

            is ChatState.Error -> {
                val error = state as ChatState.Error
                ErrorContent(
                    error = error,
                    contentPadding = contentPadding
                )
            }
        }
    }
}

// ============================================================================
// TOP BAR
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopBar(
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
                            content?.isSingleDocMode == true -> content.documentName
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
                    // New chat button
                    IconButton(onClick = onNewChat) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(Res.string.chat_new_conversation),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // History button
                    IconButton(onClick = onShowHistory) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = stringResource(Res.string.chat_history_action),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // More menu
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
                                onClick = {
                                    showMenu = false
                                    // Will be handled by intent
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(Res.string.chat_collapse_all_citations)) },
                                onClick = {
                                    showMenu = false
                                    // Will be handled by intent
                                }
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
            thickness = Constrains.Stroke.thin
        )
    }
}

// ============================================================================
// LOADING STATE
// ============================================================================

@Composable
private fun LoadingContent(contentPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
        ) {
            CircularProgressIndicator()
            Text(
                text = stringResource(Res.string.chat_loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ============================================================================
// ERROR STATE
// ============================================================================

@Composable
private fun ErrorContent(
    error: ChatState.Error,
    contentPadding: PaddingValues,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(Constrains.Spacing.large),
        contentAlignment = Alignment.Center
    ) {
        DokusErrorContent(
            exception = error.exception,
            retryHandler = error.retryHandler
        )
    }
}

// ============================================================================
// CHAT CONTENT
// ============================================================================

@Composable
private fun ChatContent(
    state: ChatState.Content,
    contentPadding: PaddingValues,
    listState: LazyListState,
    isLargeScreen: Boolean,
    onIntent: (ChatIntent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .imePadding()
    ) {
        // Scope selector chips (for cross-doc mode)
        if (state.isCrossDocMode) {
            ScopeSelectorChips(
                currentScope = state.scope,
                onScopeChange = { scope ->
                    when (scope) {
                        ChatScope.AllDocs -> onIntent(ChatIntent.SwitchToCrossDoc)
                        ChatScope.SingleDoc -> {
                            // Would need document picker
                        }
                    }
                },
                modifier = Modifier.padding(
                    horizontal = Constrains.Spacing.medium,
                    vertical = Constrains.Spacing.small
                )
            )
        }

        // Messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (state.messages.isEmpty()) {
                EmptyStateContent(
                    isSingleDocMode = state.isSingleDocMode,
                    documentName = state.documentName,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                MessagesList(
                    messages = state.messages,
                    expandedCitationIds = state.expandedCitationIds,
                    listState = listState,
                    isLargeScreen = isLargeScreen,
                    onToggleCitation = { citationId ->
                        onIntent(ChatIntent.ToggleCitation(citationId))
                    },
                    onDocumentClick = { documentId ->
                        onIntent(ChatIntent.ViewCitationSource(documentId))
                    }
                )
            }

            // Loading indicator overlay when sending
            if (state.isSending) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = Constrains.Spacing.medium)
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(
                                horizontal = Constrains.Spacing.medium,
                                vertical = Constrains.Spacing.small
                            ),
                            horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = stringResource(Res.string.chat_thinking),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Input field
        ChatInputSection(
            inputText = state.inputText,
            canSend = state.canSend,
            isSending = state.isSending,
            isInputTooLong = state.isInputTooLong,
            maxLength = state.maxMessageLength,
            onInputChange = { text -> onIntent(ChatIntent.UpdateInputText(text)) },
            onSend = { onIntent(ChatIntent.SendMessage) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constrains.Spacing.medium)
        )
    }

    // Session picker dialog
    if (state.showSessionPicker) {
        SessionPickerDialog(
            sessions = state.recentSessions,
            onSessionSelect = { sessionId ->
                onIntent(ChatIntent.LoadSession(sessionId))
            },
            onNewSession = { onIntent(ChatIntent.StartNewConversation) },
            onDismiss = { onIntent(ChatIntent.HideSessionPicker) }
        )
    }
}

// ============================================================================
// SCOPE SELECTOR
// ============================================================================

@Composable
private fun ScopeSelectorChips(
    currentScope: ChatScope,
    onScopeChange: (ChatScope) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        FilterChip(
            selected = currentScope == ChatScope.AllDocs,
            onClick = { onScopeChange(ChatScope.AllDocs) },
            label = { Text(stringResource(Res.string.chat_scope_all_documents)) },
            leadingIcon = if (currentScope == ChatScope.AllDocs) {
                {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        modifier = Modifier.size(FilterChipDefaults.IconSize)
                    )
                }
            } else null
        )
    }
}

// ============================================================================
// EMPTY STATE
// ============================================================================

@Composable
private fun EmptyStateContent(
    isSingleDocMode: Boolean,
    documentName: String?,
    modifier: Modifier = Modifier,
) {
    val documentLabel = documentName ?: stringResource(Res.string.chat_this_document)
    val promptText = if (isSingleDocMode) {
        stringResource(Res.string.chat_prompt_single_document, documentLabel)
    } else {
        stringResource(Res.string.chat_prompt_all_documents)
    }
    val descriptionText = if (isSingleDocMode) {
        stringResource(Res.string.chat_empty_description_single)
    } else {
        stringResource(Res.string.chat_empty_description_all)
    }

    Column(
        modifier = modifier.padding(Constrains.Spacing.xLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = FeatherIcons.MessageCircle,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.large))

        Text(
            text = promptText,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.small))

        Text(
            text = descriptionText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(max = 300.dp)
        )

        Spacer(modifier = Modifier.height(Constrains.Spacing.large))

        // Example questions
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
        ) {
            Text(
                text = stringResource(Res.string.chat_try_asking),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ExampleQuestionChip(
                text = if (isSingleDocMode) {
                    stringResource(Res.string.chat_example_total_amount)
                } else {
                    stringResource(Res.string.chat_example_spend_last_month)
                }
            )
            ExampleQuestionChip(
                text = if (isSingleDocMode) {
                    stringResource(Res.string.chat_example_due_date)
                } else {
                    stringResource(Res.string.chat_example_invoices_company)
                }
            )
        }
    }
}

@Composable
private fun ExampleQuestionChip(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = Constrains.Spacing.medium, vertical = Constrains.Spacing.small)
    ) {
        Text(
            text = stringResource(Res.string.chat_example_format, text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ============================================================================
// MESSAGES LIST
// ============================================================================

@Composable
private fun MessagesList(
    messages: List<ChatMessageDto>,
    expandedCitationIds: Set<String>,
    listState: LazyListState,
    isLargeScreen: Boolean,
    onToggleCitation: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = if (isLargeScreen) Constrains.Spacing.xLarge else Constrains.Spacing.medium,
            vertical = Constrains.Spacing.medium
        ),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.medium)
    ) {
        items(
            items = messages,
            key = { it.id.toString() }
        ) { message ->
            MessageItem(
                message = message,
                expandedCitationIds = expandedCitationIds,
                onToggleCitation = onToggleCitation,
                onDocumentClick = onDocumentClick
            )
        }
    }
}

@Composable
private fun MessageItem(
    message: ChatMessageDto,
    expandedCitationIds: Set<String>,
    onToggleCitation: (String) -> Unit,
    onDocumentClick: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
    ) {
        // Message bubble
        ChatMessageBubble(
            message = message.content,
            role = when (message.role) {
                MessageRole.User -> ChatMessageRole.User
                MessageRole.Assistant -> ChatMessageRole.Assistant
                MessageRole.System -> ChatMessageRole.Assistant
            },
            timestamp = formatTimestamp(message.createdAt)
        )

        // Citations (only for assistant messages)
        val citations = message.citations
        if (message.role == MessageRole.Assistant && !citations.isNullOrEmpty()) {
            val citationDisplayData = citations.map { citation ->
                CitationDisplayData(
                    chunkId = citation.chunkId,
                    documentId = citation.documentId,
                    documentName = citation.documentName,
                    pageNumber = citation.pageNumber,
                    excerpt = citation.excerpt,
                    relevanceScore = citation.relevanceScore
                )
            }

            ChatSourceCitationList(
                citations = citationDisplayData,
                onDocumentClick = onDocumentClick,
                modifier = Modifier.padding(start = Constrains.Spacing.large)
            )
        }
    }
}

@Composable
private fun formatTimestamp(dateTime: LocalDateTime): String {
    val hour = dateTime.hour.toString().padStart(2, '0')
    val minute = dateTime.minute.toString().padStart(2, '0')
    return "$hour:$minute"
}

// ============================================================================
// INPUT SECTION
// ============================================================================

@Composable
private fun ChatInputSection(
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
        // Character count warning
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

// ============================================================================
// SESSION PICKER DIALOG
// ============================================================================

@Composable
private fun SessionPickerDialog(
    sessions: List<ChatSessionSummary>,
    onSessionSelect: (ChatSessionId) -> Unit,
    onNewSession: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(Res.string.chat_history_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(Constrains.Spacing.medium))
                    Text(
                        text = stringResource(Res.string.chat_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.small)
                ) {
                    items(sessions) { session ->
                        SessionListItem(
                            session = session,
                            onClick = { onSessionSelect(session.sessionId) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onNewSession) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(Constrains.Spacing.xSmall))
                Text(stringResource(Res.string.chat_new_chat))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    )
}

@Composable
private fun SessionListItem(
    session: ChatSessionSummary,
    onClick: () -> Unit,
) {
    val messageCountText = if (session.messageCount == 1) {
        stringResource(Res.string.chat_message_count_single, session.messageCount)
    } else {
        stringResource(Res.string.chat_message_count_plural, session.messageCount)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier.padding(Constrains.Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Constrains.Spacing.xSmall)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.documentName ?: stringResource(Res.string.chat_general_chat),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = messageCountText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val preview = session.lastMessagePreview
            if (preview != null) {
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
