package tech.dokus.app.share

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShareImportContainerTest {

    @BeforeTest
    fun setUp() {
        clearPendingSharedFiles()
    }

    @AfterTest
    fun tearDown() {
        clearPendingSharedFiles()
    }

    @Test
    fun `logged out fail-fast`() = runTest {
        val testScope = this
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(testDocument()))
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = false),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(testTenant()))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertTrue(error.canNavigateToLogin)
            assertEquals(DokusException.NotAuthenticated(), error.exception)
            assertEquals(null, error.retryHandler)
            assertEquals(0, uploadUseCase.invocations)
        }
    }

    @Test
    fun `single workspace auto-upload`() = runTest {
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000011"),
            displayName = "Solo Workspace"
        )
        val document = testDocument(
            id = DocumentId("10000000-0000-0000-0000-000000000001"),
            tenantId = workspace.id
        )
        val selectTenantUseCase = FakeSelectTenantUseCase()
        val uploadUseCase = FakeUploadDocumentUseCase(
            result = Result.success(document),
            progressPoints = listOf(0.25f, 0.75f, 1f)
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn ShareImportAction.NavigateToDocumentReview(document.id.toString())

            assertEquals(
                ShareImportState.Success(
                    primaryFileName = "invoice.pdf",
                    additionalFileCount = 0,
                    uploadedCount = 1,
                    documentId = document.id.toString()
                ),
                states.value
            )
            assertEquals(listOf(workspace.id), selectTenantUseCase.invocations)
            assertEquals(1, uploadUseCase.invocations)
        }
    }

    @Test
    fun `multi-workspace requires selection`() = runTest {
        val testScope = this
        val workspaceA = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000021"),
            displayName = "Workspace A"
        )
        val workspaceB = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000022"),
            displayName = "Workspace B"
        )
        val document = testDocument(
            id = DocumentId("20000000-0000-0000-0000-000000000001"),
            tenantId = workspaceB.id
        )
        val selectTenantUseCase = FakeSelectTenantUseCase()
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(document))
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspaceA, workspaceB))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val selectState = assertIs<ShareImportState.SelectWorkspace>(states.value)
            assertEquals(2, selectState.workspaces.size)
            assertEquals("invoice.pdf", selectState.primaryFileName)
            assertEquals(0, selectState.additionalFileCount)
            assertEquals(0, uploadUseCase.invocations)

            ShareImportIntent.SelectWorkspace(workspaceB.id) resultsIn
                ShareImportAction.NavigateToDocumentReview(document.id.toString())
            assertEquals(listOf(workspaceB.id), selectTenantUseCase.invocations)
            assertEquals(1, uploadUseCase.invocations)
        }
    }

    @Test
    fun `multiple shared files upload sequentially`() = runTest {
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000026"),
            displayName = "Workspace"
        )
        val documents = listOf(
            testDocument(id = DocumentId("26000000-0000-0000-0000-000000000001"), tenantId = workspace.id),
            testDocument(id = DocumentId("26000000-0000-0000-0000-000000000002"), tenantId = workspace.id),
            testDocument(id = DocumentId("26000000-0000-0000-0000-000000000003"), tenantId = workspace.id)
        )
        val uploadUseCase = FakeUploadDocumentUseCase(
            results = documents.map { Result.success(it) }
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = uploadUseCase
        )

        ExternalShareImportHandler.onNewSharedFiles(
            listOf(
                testSharedFile(name = "one.pdf"),
                testSharedFile(name = "two.pdf"),
                testSharedFile(name = "three.pdf")
            )
        )

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn
                ShareImportAction.NavigateToDocumentReview(documents.first().id.toString())

            assertEquals(3, uploadUseCase.invocations)
            assertEquals(listOf("one.pdf", "two.pdf", "three.pdf"), uploadUseCase.uploadedFilenames)

            assertEquals(
                ShareImportState.Success(
                    primaryFileName = "one.pdf",
                    additionalFileCount = 2,
                    uploadedCount = 3,
                    documentId = documents.first().id.toString()
                ),
                states.value
            )
        }
    }

    @Test
    fun `select tenant failure`() = runTest {
        val testScope = this
        val workspaceA = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000031"),
            displayName = "Workspace A"
        )
        val workspaceB = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000032"),
            displayName = "Workspace B"
        )
        val selectTenantUseCase = FakeSelectTenantUseCase(
            resultByTenantId = mapOf(
                workspaceB.id to Result.failure(IllegalStateException("switch failed"))
            )
        )
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(testDocument()))
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspaceA, workspaceB))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()
            assertIs<ShareImportState.SelectWorkspace>(states.value)

            emit(ShareImportIntent.SelectWorkspace(workspaceB.id))
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertEquals(DokusException.WorkspaceSelectFailed, error.exception)
            assertTrue(error.retryHandler != null)
            assertEquals(0, uploadUseCase.invocations)
        }
    }

    @Test
    fun `upload failure`() = runTest {
        val testScope = this
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000041"),
            displayName = "Workspace"
        )
        val uploadUseCase = FakeUploadDocumentUseCase(
            Result.failure(IllegalStateException("upload failed"))
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertEquals(DokusException.DocumentUploadFailed, error.exception)
            assertTrue(error.retryHandler != null)
            assertEquals(1, uploadUseCase.invocations)
        }
    }

    @Test
    fun `upload success emits navigation to document review`() = runTest {
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000051"),
            displayName = "Workspace"
        )
        val document = testDocument(
            id = DocumentId("50000000-0000-0000-0000-000000000001"),
            tenantId = workspace.id
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = FakeUploadDocumentUseCase(Result.success(document))
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn
                ShareImportAction.NavigateToDocumentReview(document.id.toString())
        }
    }
}

private class FakeTokenManager(
    isAuthenticated: Boolean
) : TokenManager {
    override val isAuthenticated = MutableStateFlow(isAuthenticated)

    override suspend fun getValidAccessToken(): String? = null

    override suspend fun refreshToken(force: Boolean): String? = null

    override suspend fun onAuthenticationFailed() = Unit

    override suspend fun getCurrentClaims(): JwtClaims? = null
}

private class FakeListMyTenantsUseCase(
    private val result: Result<List<Tenant>>
) : ListMyTenantsUseCase {
    override suspend fun invoke(): Result<List<Tenant>> = result
}

private class FakeSelectTenantUseCase(
    private val resultByTenantId: Map<TenantId, Result<Unit>> = emptyMap(),
    private val defaultResult: Result<Unit> = Result.success(Unit)
) : SelectTenantUseCase {
    val invocations = mutableListOf<TenantId>()

    override suspend fun invoke(tenantId: TenantId): Result<Unit> {
        invocations += tenantId
        return resultByTenantId[tenantId] ?: defaultResult
    }
}

private class FakeUploadDocumentUseCase(
    private val results: List<Result<DocumentDto>>,
    private val defaultProgressPoints: List<Float> = emptyList(),
    private val progressPointsByInvocation: Map<Int, List<Float>> = emptyMap()
) : UploadDocumentUseCase {
    constructor(
        result: Result<DocumentDto>,
        progressPoints: List<Float> = emptyList()
    ) : this(
        results = listOf(result),
        defaultProgressPoints = progressPoints,
        progressPointsByInvocation = emptyMap()
    )

    var invocations: Int = 0
    val uploadedFilenames = mutableListOf<String>()

    override suspend fun invoke(
        fileContent: ByteArray,
        filename: String,
        contentType: String?,
        prefix: String,
        onProgress: (Float) -> Unit
    ): Result<DocumentDto> {
        val invocationIndex = invocations
        invocations += 1
        uploadedFilenames += filename

        val progressPoints = progressPointsByInvocation[invocationIndex] ?: defaultProgressPoints
        progressPoints.forEach(onProgress)

        return results.getOrNull(invocationIndex)
            ?: results.lastOrNull()
            ?: Result.failure(IllegalStateException("No upload result configured"))
    }
}

private fun testSharedFile(name: String = "invoice.pdf"): SharedImportFile = SharedImportFile(
    name = name,
    bytes = byteArrayOf(1, 2, 3),
    mimeType = "application/pdf"
)

private fun testTenant(
    id: TenantId = TenantId("00000000-0000-0000-0000-000000000001"),
    displayName: String = "Workspace"
): Tenant = Tenant(
    id = id,
    type = TenantType.Company,
    legalName = LegalName("Workspace LLC"),
    displayName = DisplayName(displayName),
    subscription = SubscriptionTier.Core,
    status = TenantStatus.Active,
    language = Language.En,
    vatNumber = VatNumber("BE0123456789"),
    trialEndsAt = null,
    subscriptionStartedAt = null,
    createdAt = LocalDateTime(2024, 1, 1, 0, 0),
    updatedAt = LocalDateTime(2024, 1, 1, 0, 0),
    avatar = null
)

private fun testDocument(
    id: DocumentId = DocumentId("00000000-0000-0000-0000-000000000101"),
    tenantId: TenantId = TenantId("00000000-0000-0000-0000-000000000001")
): DocumentDto = DocumentDto(
    id = id,
    tenantId = tenantId,
    filename = "invoice.pdf",
    contentType = "application/pdf",
    sizeBytes = 1234,
    storageKey = "documents/invoice.pdf",
    source = DocumentSource.Upload,
    uploadedAt = LocalDateTime(2024, 1, 1, 0, 0),
    downloadUrl = null
)

private fun clearPendingSharedFiles() {
    while (!ExternalShareImportHandler.consumePendingFiles().isNullOrEmpty()) {
        // Clear all pending payloads left by previous tests.
    }
}
