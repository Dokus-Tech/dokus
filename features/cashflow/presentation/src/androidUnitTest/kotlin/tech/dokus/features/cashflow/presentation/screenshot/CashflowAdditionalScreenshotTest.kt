package tech.dokus.features.cashflow.presentation.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.cash.paparazzi.Paparazzi
import kotlinx.datetime.LocalDateTime
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.model.ai.ChatCitation
import tech.dokus.domain.model.ai.ChatMessageDto
import tech.dokus.domain.model.ai.ChatMessageId
import tech.dokus.domain.model.ai.ChatScope
import tech.dokus.domain.model.ai.ChatSessionId
import tech.dokus.domain.model.ai.MessageRole
import tech.dokus.features.cashflow.mvi.AddDocumentState
import tech.dokus.features.cashflow.presentation.cashflow.model.manager.DocumentUploadManager
import tech.dokus.features.cashflow.presentation.cashflow.screen.AddDocumentScreen
import tech.dokus.features.cashflow.presentation.chat.ChatState
import tech.dokus.features.cashflow.presentation.chat.screen.ChatScreen
import tech.dokus.features.cashflow.usecases.DeleteDocumentUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar

@RunWith(Parameterized::class)
class CashflowAdditionalScreenshotTest(private val viewport: ScreenshotViewport) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun viewports() = ScreenshotViewport.entries.toList()
    }

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = viewport.deviceConfig,
        showSystemUi = false,
        maxPercentDifference = 0.1
    )

    @Test
    fun addDocumentScreen() {
        val uploadManager = createUploadManager()
        paparazzi.snapshotAllViewports("AddDocumentScreen", viewport) {
            AddDocumentScreen(
                state = AddDocumentState.Idle(),
                uploadTasks = emptyList(),
                uploadedDocuments = emptyMap(),
                deletionHandles = emptyMap(),
                uploadManager = uploadManager,
                onIntent = {}
            )
        }
    }

    @Test
    fun chatScreen() {
        val state = ChatState.Content(
            scope = ChatScope.AllDocs,
            messages = sampleChatMessages(),
            inputText = "Summarize the latest invoices",
            isSending = false
        )
        paparazzi.snapshotAllViewports("ChatScreen", viewport) {
            val snackbarHostState = remember { SnackbarHostState() }
            ChatScreen(
                state = state,
                listState = rememberLazyListState(),
                isLargeScreen = false,
                snackbarHostState = snackbarHostState,
                onIntent = {},
                onBackClick = {}
            )
        }
    }

    @Test
    fun documentReviewScreen() {
        paparazzi.snapshotAllViewports("DocumentReviewScreen", viewport) {
            DocumentReviewContent()
        }
    }
}

@Composable
private fun DocumentReviewContent() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(0.dp)
    ) {
        PTopAppBar(
            title = "Document Review",
            navController = null,
            showBackButton = true
        )

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Document preview placeholder",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            DokusCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Extracted Data",
                        style = MaterialTheme.typography.titleMedium
                    )
                    ReviewRow(label = "Vendor", value = "Acme Corporation")
                    ReviewRow(label = "Amount", value = "EUR 1,230.00")
                    ReviewRow(label = "Date", value = "2024-01-12")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                POutlinedButton(
                    text = "Reject",
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
                PPrimaryButton(
                    text = "Confirm",
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }
        }
    }
}

@Composable
private fun ReviewRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun sampleChatMessages(): List<ChatMessageDto> {
    val tenantId = TenantId("00000000-0000-0000-0000-000000000201")
    val userId = UserId("00000000-0000-0000-0000-000000000202")
    val sessionId = ChatSessionId.parse("00000000-0000-0000-0000-000000000203")
    val documentId = DocumentId.parse("00000000-0000-0000-0000-000000000204")

    return listOf(
        ChatMessageDto(
            id = ChatMessageId.parse("00000000-0000-0000-0000-000000000211"),
            tenantId = tenantId,
            userId = userId,
            sessionId = sessionId,
            role = MessageRole.User,
            content = "Summarize the latest invoices",
            scope = ChatScope.AllDocs,
            documentId = null,
            sequenceNumber = 1,
            createdAt = LocalDateTime(2024, 1, 12, 10, 15)
        ),
        ChatMessageDto(
            id = ChatMessageId.parse("00000000-0000-0000-0000-000000000212"),
            tenantId = tenantId,
            userId = userId,
            sessionId = sessionId,
            role = MessageRole.Assistant,
            content = "The latest invoices total EUR 1,230.00 across 3 documents.",
            scope = ChatScope.AllDocs,
            documentId = documentId,
            citations = listOf(
                ChatCitation(
                    chunkId = "chunk-1",
                    documentId = "doc-001",
                    documentName = "Invoice_001.pdf",
                    pageNumber = 2,
                    excerpt = "Total: EUR 430.00"
                ),
                ChatCitation(
                    chunkId = "chunk-2",
                    documentId = "doc-002",
                    documentName = "Invoice_002.pdf",
                    pageNumber = 1,
                    excerpt = "Total: EUR 800.00"
                )
            ),
            sequenceNumber = 2,
            createdAt = LocalDateTime(2024, 1, 12, 10, 16)
        )
    )
}

private fun createUploadManager(): DocumentUploadManager {
    val uploadUseCase = object : UploadDocumentUseCase {
        override suspend fun invoke(
            fileContent: ByteArray,
            filename: String,
            contentType: String?,
            prefix: String,
            onProgress: (Float) -> Unit
        ): Result<tech.dokus.domain.model.DocumentIntakeResult> {
            return Result.failure(IllegalStateException("Not implemented"))
        }
    }
    val deleteUseCase = object : DeleteDocumentUseCase {
        override suspend fun invoke(
            documentId: DocumentId,
            sourceId: tech.dokus.domain.ids.DocumentSourceId?
        ): Result<Unit> {
            return Result.success(Unit)
        }
    }
    return DocumentUploadManager(
        uploadDocumentUseCase = uploadUseCase,
        deleteDocumentUseCase = deleteUseCase
    )
}

private fun Paparazzi.snapshotAllViewports(
    baseName: String,
    viewport: ScreenshotViewport,
    content: @Composable () -> Unit
) {
    snapshot("${baseName}_${viewport.displayName}_light") {
        ScreenshotTestWrapper(isDarkMode = false, screenSize = viewport.screenSize) {
            content()
        }
    }
    snapshot("${baseName}_${viewport.displayName}_dark") {
        ScreenshotTestWrapper(isDarkMode = true, screenSize = viewport.screenSize) {
            content()
        }
    }
}
