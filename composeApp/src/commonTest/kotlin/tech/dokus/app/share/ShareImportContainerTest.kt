package tech.dokus.app.share

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.TenantId
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
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
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(testTenant()))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertEquals(DokusException.NotAuthenticated(), error.exception)
            assertEquals(null, error.retryHandler)
            assertTrue(error.canNavigateToLogin)
            assertEquals(0, uploadUseCase.invocations)
        }
    }

    @Test
    fun `single tenant auto-upload emits finish`() = runTest {
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000011"),
            displayName = "Solo Workspace"
        )
        val document = testDocument(
            id = DocumentId("10000000-0000-0000-0000-000000000001"),
            tenantId = workspace.id
        )
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(document))
        val selectTenantUseCase = FakeSelectTenantUseCase()
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn ShareImportAction.Finish(
                successCount = 1,
                failureCount = 0,
                uploadedDocumentIds = listOf(document.id.toString())
            )

            assertEquals(1, uploadUseCase.invocations)
            assertEquals(listOf(workspace.id), selectTenantUseCase.invocations)
            val success = assertIs<ShareImportState.SuccessPulse>(states.value)
            assertEquals(1, success.uploadedCount)
            assertEquals(listOf(document.id.toString()), success.uploadedDocumentIds)
        }
    }

    @Test
    fun `multi-tenant with claims tenant uses claims workspace`() = runTest {
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
            tokenManager = FakeTokenManager(
                isAuthenticated = true,
                claims = testClaims(workspaceB.id)
            ),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspaceA, workspaceB))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn ShareImportAction.Finish(
                successCount = 1,
                failureCount = 0,
                uploadedDocumentIds = listOf(document.id.toString())
            )
            assertEquals(0, selectTenantUseCase.invocations.size)
            assertEquals(1, uploadUseCase.invocations)
        }
    }

    @Test
    fun `multi-tenant with missing claims uses last selected tenant`() = runTest {
        val workspaceA = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000031"),
            displayName = "Workspace A"
        )
        val workspaceB = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000032"),
            displayName = "Workspace B"
        )
        val document = testDocument(
            id = DocumentId("30000000-0000-0000-0000-000000000001"),
            tenantId = workspaceB.id
        )
        val selectTenantUseCase = FakeSelectTenantUseCase()
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(document))
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true, claims = null),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(workspaceB.id),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspaceA, workspaceB))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn ShareImportAction.Finish(
                successCount = 1,
                failureCount = 0,
                uploadedDocumentIds = listOf(document.id.toString())
            )
            assertEquals(listOf(workspaceB.id), selectTenantUseCase.invocations)
            assertEquals(1, uploadUseCase.invocations)
        }
    }

    @Test
    fun `multi-tenant with stale claims falls back to valid last selected`() = runTest {
        val workspaceA = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000041"),
            displayName = "Workspace A"
        )
        val workspaceB = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000042"),
            displayName = "Workspace B"
        )
        val staleTenant = TenantId("00000000-0000-0000-0000-000000000099")
        val document = testDocument(
            id = DocumentId("40000000-0000-0000-0000-000000000001"),
            tenantId = workspaceB.id
        )
        val selectTenantUseCase = FakeSelectTenantUseCase()
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(document))
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(
                isAuthenticated = true,
                claims = testClaims(staleTenant)
            ),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(workspaceB.id),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspaceA, workspaceB))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn ShareImportAction.Finish(
                successCount = 1,
                failureCount = 0,
                uploadedDocumentIds = listOf(document.id.toString())
            )
            assertEquals(listOf(workspaceB.id), selectTenantUseCase.invocations)
            assertEquals(1, uploadUseCase.invocations)
        }
    }

    @Test
    fun `multi-tenant unresolved workspace context shows error with open-app CTA`() = runTest {
        val testScope = this
        val workspaceA = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000051"),
            displayName = "Workspace A"
        )
        val workspaceB = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000052"),
            displayName = "Workspace B"
        )
        val uploadUseCase = FakeUploadDocumentUseCase(Result.success(testDocument()))
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true, claims = null),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(null),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspaceA, workspaceB))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertEquals(DokusException.WorkspaceContextUnavailable, error.exception)
            assertNotNull(error.retryHandler)
            assertTrue(error.canOpenApp)
            assertEquals(0, uploadUseCase.invocations)
        }
    }

    @Test
    fun `upload failure stops immediately and does not attempt remaining files`() = runTest {
        val testScope = this
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000061"),
            displayName = "Workspace"
        )
        val uploadUseCase = FakeUploadDocumentUseCase(
            results = listOf(
                Result.success(testDocument(id = DocumentId("61000000-0000-0000-0000-000000000001"), tenantId = workspace.id)),
                Result.failure(IllegalStateException("failure at second file")),
                Result.success(testDocument(id = DocumentId("61000000-0000-0000-0000-000000000003"), tenantId = workspace.id))
            )
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true, claims = testClaims(workspace.id)),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(),
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
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertEquals(DokusException.DocumentUploadFailed, error.exception)
            assertNotNull(error.retryHandler)
            assertEquals(2, uploadUseCase.invocations)
            assertEquals(listOf("one.pdf", "two.pdf"), uploadUseCase.uploadedFilenames)
        }
    }

    @Test
    fun `retry resumes from failed and pending files without reuploading successful`() = runTest {
        val testScope = this
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000071"),
            displayName = "Workspace"
        )
        val firstDoc = testDocument(
            id = DocumentId("71000000-0000-0000-0000-000000000001"),
            tenantId = workspace.id
        )
        val secondDoc = testDocument(
            id = DocumentId("71000000-0000-0000-0000-000000000002"),
            tenantId = workspace.id
        )
        val thirdDoc = testDocument(
            id = DocumentId("71000000-0000-0000-0000-000000000003"),
            tenantId = workspace.id
        )
        val uploadUseCase = FakeUploadDocumentUseCase(
            results = listOf(
                Result.success(firstDoc),
                Result.failure(IllegalStateException("fail second")),
                Result.success(secondDoc),
                Result.success(thirdDoc)
            )
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true, claims = testClaims(workspace.id)),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(),
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
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()
            assertIs<ShareImportState.Error>(states.value)
            assertEquals(listOf("one.pdf", "two.pdf"), uploadUseCase.uploadedFilenames)

            ShareImportIntent.Retry resultsIn ShareImportAction.Finish(
                successCount = 3,
                failureCount = 0,
                uploadedDocumentIds = listOf(
                    firstDoc.id.toString(),
                    secondDoc.id.toString(),
                    thirdDoc.id.toString()
                )
            )

            assertEquals(
                listOf("one.pdf", "two.pdf", "two.pdf", "three.pdf"),
                uploadUseCase.uploadedFilenames
            )
        }
    }

    @Test
    fun `select tenant failure returns workspace selection error`() = runTest {
        val testScope = this
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000081"),
            displayName = "Workspace"
        )
        val selectTenantUseCase = FakeSelectTenantUseCase(
            resultByTenantId = mapOf(
                workspace.id to Result.failure(IllegalStateException("switch failed"))
            )
        )
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true, claims = null),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(workspace.id),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = selectTenantUseCase,
            uploadDocumentUseCase = FakeUploadDocumentUseCase(Result.success(testDocument()))
        )
        ExternalShareImportHandler.onNewSharedFiles(listOf(testSharedFile()))

        container.store.subscribeAndTest {
            emit(ShareImportIntent.Load)
            testScope.advanceUntilIdle()

            val error = assertIs<ShareImportState.Error>(states.value)
            assertEquals(DokusException.WorkspaceSelectFailed, error.exception)
            assertNotNull(error.retryHandler)
        }
    }

    @Test
    fun `multi-file completion emits finish with accurate ids`() = runTest {
        val workspace = testTenant(
            id = TenantId("00000000-0000-0000-0000-000000000091"),
            displayName = "Workspace"
        )
        val docs = listOf(
            testDocument(id = DocumentId("91000000-0000-0000-0000-000000000001"), tenantId = workspace.id),
            testDocument(id = DocumentId("91000000-0000-0000-0000-000000000002"), tenantId = workspace.id)
        )
        val uploadUseCase = FakeUploadDocumentUseCase(results = docs.map { Result.success(it) })
        val container = ShareImportContainer(
            tokenManager = FakeTokenManager(isAuthenticated = true, claims = testClaims(workspace.id)),
            getLastSelectedTenantIdUseCase = FakeGetLastSelectedTenantIdUseCase(),
            listMyTenantsUseCase = FakeListMyTenantsUseCase(Result.success(listOf(workspace))),
            selectTenantUseCase = FakeSelectTenantUseCase(),
            uploadDocumentUseCase = uploadUseCase
        )
        ExternalShareImportHandler.onNewSharedFiles(
            listOf(testSharedFile(name = "a.pdf"), testSharedFile(name = "b.pdf"))
        )

        container.store.subscribeAndTest {
            ShareImportIntent.Load resultsIn ShareImportAction.Finish(
                successCount = 2,
                failureCount = 0,
                uploadedDocumentIds = docs.map { it.id.toString() }
            )
        }
    }
}
