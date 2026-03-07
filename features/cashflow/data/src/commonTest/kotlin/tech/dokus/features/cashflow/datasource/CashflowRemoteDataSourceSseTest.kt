package tech.dokus.features.cashflow.datasource

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.plugins.resources.Resources
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import tech.dokus.domain.config.DynamicDokusEndpointProvider
import tech.dokus.domain.config.ServerConfig
import tech.dokus.domain.config.ServerConfigManager
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.DocumentDeletedEventDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentRecordDto
import tech.dokus.domain.model.DocumentRecordStreamEvent
import tech.dokus.domain.model.DocumentStreamEventNames
import tech.dokus.domain.utils.json
import tech.dokus.foundation.app.network.SseEventCollector
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.datetime.LocalDateTime

class CashflowRemoteDataSourceSseTest {

    @Test
    fun `observeDocumentRecordEvents maps snapshot and deleted events`() = runTest {
        val record = documentRecord("00000000-0000-0000-0000-000000000101")
        val httpClient = HttpClient(MockEngine { error("Unexpected HTTP request in document record SSE test") }) {
            install(Resources)
        }
        val dataSource = CashflowRemoteDataSourceImpl(
            httpClient = httpClient,
            endpointProvider = DynamicDokusEndpointProvider(FakeServerConfigManager()),
            sseEventCollector = FakeSseEventCollector(
                events = listOf(
                    ServerSentEvent(
                        event = DocumentStreamEventNames.Snapshot,
                        data = json.encodeToString(DocumentRecordDto.serializer(), record),
                    ),
                    ServerSentEvent(
                        event = DocumentStreamEventNames.Deleted,
                        data = json.encodeToString(
                            DocumentDeletedEventDto.serializer(),
                            DocumentDeletedEventDto(record.document.id),
                        ),
                    ),
                )
            ),
        )

        val events = dataSource.observeDocumentRecordEvents(record.document.id)
            .take(2)
            .toList()

        val snapshot = assertIs<DocumentRecordStreamEvent.Snapshot>(events.first())
        assertEquals(record, snapshot.record)
        assertEquals(DocumentRecordStreamEvent.Deleted, events.last())
    }

    @Test
    fun `observeDocumentCollectionChanges maps collection invalidation events`() = runTest {
        val httpClient = HttpClient(MockEngine { error("Unexpected HTTP request in document collection SSE test") }) {
            install(Resources)
        }
        val dataSource = CashflowRemoteDataSourceImpl(
            httpClient = httpClient,
            endpointProvider = DynamicDokusEndpointProvider(FakeServerConfigManager()),
            sseEventCollector = FakeSseEventCollector(
                events = listOf(
                    ServerSentEvent(
                        event = DocumentStreamEventNames.CollectionChanged,
                        data = "{}",
                    ),
                )
            ),
        )

        val events = dataSource.observeDocumentCollectionChanges()
            .take(1)
            .toList()

        assertEquals(1, events.size)
    }
}

private class FakeSseEventCollector(
    private val events: List<ServerSentEvent>,
) : SseEventCollector {
    override suspend fun collect(
        httpClient: HttpClient,
        request: io.ktor.client.request.HttpRequestBuilder.() -> Unit,
        onConnected: () -> Unit,
        onEvent: suspend (ServerSentEvent) -> Unit,
    ) {
        onConnected()
        events.forEach { onEvent(it) }
        awaitCancellation()
    }
}

private class FakeServerConfigManager(
    initialServer: ServerConfig = ServerConfig.Cloud,
) : ServerConfigManager {
    override val currentServer = MutableStateFlow(initialServer)
    override val isCloudServer = MutableStateFlow(initialServer.isCloud)

    override suspend fun setServer(config: ServerConfig) {
        currentServer.value = config
        isCloudServer.value = config.isCloud
    }

    override suspend fun resetToCloud() {
        setServer(ServerConfig.Cloud)
    }

    override suspend fun initialize() = Unit
}

private fun documentRecord(documentId: String): DocumentRecordDto {
    return DocumentRecordDto(
        document = DocumentDto(
            id = DocumentId.parse(documentId),
            tenantId = TenantId.parse("00000000-0000-0000-0000-000000000001"),
            filename = "doc-$documentId.pdf",
            contentType = "application/pdf",
            sizeBytes = 1024,
            storageKey = "documents/$documentId.pdf",
            uploadedAt = LocalDateTime(2026, 1, 1, 10, 0),
            downloadUrl = null,
        ),
        draft = null,
        latestIngestion = null,
        confirmedEntity = null,
        cashflowEntryId = null,
        pendingMatchReview = null,
        sources = emptyList(),
    )
}
