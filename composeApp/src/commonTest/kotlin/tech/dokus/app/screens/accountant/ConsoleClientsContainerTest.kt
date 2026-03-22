package tech.dokus.app.screens.accountant

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.DisplayName
import tech.dokus.domain.Email
import tech.dokus.domain.Name
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.FirmRole
import tech.dokus.domain.enums.UserRole
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.FirmId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDetailDto
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentListItemDto
import tech.dokus.domain.model.User
import tech.dokus.domain.model.auth.AccountMeResponse
import tech.dokus.domain.model.auth.AppSurface
import tech.dokus.domain.model.auth.ConsoleClientSummary
import tech.dokus.domain.model.auth.FirmWorkspaceSummary
import tech.dokus.domain.model.auth.SurfaceAvailability
import tech.dokus.domain.model.common.PaginatedResponse
import tech.dokus.features.auth.usecases.GetAccountMeUseCase
import tech.dokus.features.auth.usecases.GetConsoleClientDocumentUseCase
import tech.dokus.features.auth.usecases.ListConsoleClientDocumentsUseCase
import tech.dokus.features.auth.usecases.ListConsoleClientsUseCase
import tech.dokus.foundation.app.shell.WorkspaceContextStore
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isSuccess
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConsoleClientsContainerTest {

    @BeforeTest
    fun setUp() {
        WorkspaceContextStore.selectTenantWorkspace()
    }

    @Test
    fun `load sorts clients and query filters by name and vat`() = runTest {
        val firmId = FirmId("00000000-0000-0000-0000-000000000901")
        val clients = listOf(
            client(
                tenantId = "00000000-0000-0000-0000-000000000300",
                companyName = "Zeta BV",
                vat = null
            ),
            client(
                tenantId = "00000000-0000-0000-0000-000000000200",
                companyName = "Acme BV",
                vat = "BE02.000.000.000"
            ),
            client(
                tenantId = "00000000-0000-0000-0000-000000000100",
                companyName = "Acme BV",
                vat = "BE01.000.000.000"
            ),
        )
        val getAccountMeUseCase = FakeGetAccountMeUseCase(
            Result.success(accountMe(firmId = firmId))
        )
        val listClientsUseCase = FakeListConsoleClientsUseCase(Result.success(clients))
        val container = ConsoleClientsContainer(
            getAccountMeUseCase = getAccountMeUseCase,
            listConsoleClientsUseCase = listClientsUseCase,
            listConsoleClientDocumentsUseCase = FakeListConsoleClientDocumentsUseCase(
                Result.success(PaginatedResponse(items = emptyList(), total = 0L, limit = 50, offset = 0))
            ),
            getConsoleClientDocumentUseCase = FakeGetConsoleClientDocumentUseCase(
                Result.failure(DokusException.NotFound())
            ),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            assertEquals(1, getAccountMeUseCase.invocations)
            assertEquals(listOf(firmId), listClientsUseCase.receivedFirmIds)

            val loaded = states.value
            assertTrue(loaded.clients.isSuccess())
            assertEquals(
                listOf(
                    TenantId("00000000-0000-0000-0000-000000000100"),
                    TenantId("00000000-0000-0000-0000-000000000200"),
                    TenantId("00000000-0000-0000-0000-000000000300"),
                ),
                loaded.clients.data.map { it.tenantId }
            )

            emit(ConsoleClientsIntent.UpdateQuery("be01"))
            advanceUntilIdle()

            val filtered = states.value
            assertEquals(
                listOf(TenantId("00000000-0000-0000-0000-000000000100")),
                filtered.filteredClients.map { it.tenantId }
            )
        }
    }

    @Test
    fun `load failure maps to error state`() = runTest {
        val expectedError = DokusException.NotAuthenticated()
        val container = ConsoleClientsContainer(
            getAccountMeUseCase = FakeGetAccountMeUseCase(
                Result.success(accountMe(FirmId("00000000-0000-0000-0000-000000000902")))
            ),
            listConsoleClientsUseCase = FakeListConsoleClientsUseCase(Result.failure(expectedError)),
            listConsoleClientDocumentsUseCase = FakeListConsoleClientDocumentsUseCase(
                Result.success(PaginatedResponse(items = emptyList(), total = 0L, limit = 50, offset = 0))
            ),
            getConsoleClientDocumentUseCase = FakeGetConsoleClientDocumentUseCase(
                Result.failure(DokusException.NotFound())
            ),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            val state = states.value
            assertTrue(state.clients.isError())
            assertEquals(expectedError, state.clients.exception)
        }
    }

    @Test
    fun `select client success loads documents list`() = runTest {
        val firmId = FirmId("00000000-0000-0000-0000-000000000903")
        val tenantId = TenantId("00000000-0000-0000-0000-000000000555")
        val docs = listOf(listItemRecord(tenantId, "doc-1.pdf"), listItemRecord(tenantId, "doc-2.pdf"))
        val documentsUseCase = FakeListConsoleClientDocumentsUseCase(
            Result.success(PaginatedResponse(items = docs, total = docs.size.toLong(), limit = 50, offset = 0))
        )
        val container = ConsoleClientsContainer(
            getAccountMeUseCase = FakeGetAccountMeUseCase(Result.success(accountMe(firmId))),
            listConsoleClientsUseCase = FakeListConsoleClientsUseCase(
                Result.success(listOf(client(tenantId.toString(), "Client One", null)))
            ),
            listConsoleClientDocumentsUseCase = documentsUseCase,
            getConsoleClientDocumentUseCase = FakeGetConsoleClientDocumentUseCase(
                Result.failure(DokusException.NotFound())
            ),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            emit(ConsoleClientsIntent.SelectClient(tenantId))
            advanceUntilIdle()

            assertEquals(listOf(tenantId), documentsUseCase.receivedTenantIds)
            val loaded = states.value
            assertEquals(tenantId, loaded.selectedClientTenantId)
            val docsState = assertIs<DokusState.Success<List<DocumentListItemDto>>>(loaded.documentsState)
            assertEquals(2, docsState.data.size)
        }
    }

    @Test
    fun `select client failure keeps context and sets actionError`() = runTest {
        val firmId = FirmId("00000000-0000-0000-0000-000000000904")
        val tenantId = TenantId("00000000-0000-0000-0000-000000000666")
        val expectedError = DokusException.NotAuthorized("denied")
        val documentsUseCase = FakeListConsoleClientDocumentsUseCase(Result.failure(expectedError))
        val container = ConsoleClientsContainer(
            getAccountMeUseCase = FakeGetAccountMeUseCase(Result.success(accountMe(firmId))),
            listConsoleClientsUseCase = FakeListConsoleClientsUseCase(
                Result.success(listOf(client(tenantId.toString(), "Client One", null)))
            ),
            listConsoleClientDocumentsUseCase = documentsUseCase,
            getConsoleClientDocumentUseCase = FakeGetConsoleClientDocumentUseCase(
                Result.failure(DokusException.NotFound())
            ),
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            emit(ConsoleClientsIntent.SelectClient(tenantId))
            advanceUntilIdle()

            val loaded = states.value
            assertEquals(tenantId, loaded.selectedClientTenantId)
            val docsError = assertIs<DokusState.Error<List<DocumentListItemDto>>>(loaded.documentsState)
            assertEquals(expectedError, docsError.exception)
            assertEquals(expectedError, loaded.actionError)
            assertNull(loaded.selectedDocument)
            assertEquals(listOf(tenantId), documentsUseCase.receivedTenantIds)
        }
    }

    @Test
    fun `open document success stores selected document`() = runTest {
        val firmId = FirmId("00000000-0000-0000-0000-000000000905")
        val tenantId = TenantId("00000000-0000-0000-0000-000000000777")
        val documentId = DocumentId.generate()
        val listItem = listItemRecord(tenantId, "INV-77.pdf", documentId = documentId)
        val detailRecord = documentDetailRecord(tenantId, "INV-77.pdf", documentId = documentId)
        val documentUseCase = FakeGetConsoleClientDocumentUseCase(Result.success(detailRecord))
        val container = ConsoleClientsContainer(
            getAccountMeUseCase = FakeGetAccountMeUseCase(Result.success(accountMe(firmId))),
            listConsoleClientsUseCase = FakeListConsoleClientsUseCase(
                Result.success(listOf(client(tenantId.toString(), "Client One", null)))
            ),
            listConsoleClientDocumentsUseCase = FakeListConsoleClientDocumentsUseCase(
                Result.success(PaginatedResponse(items = listOf(listItem), total = 1L, limit = 50, offset = 0))
            ),
            getConsoleClientDocumentUseCase = documentUseCase,
        )

        container.store.subscribeAndTest {
            advanceUntilIdle()
            emit(ConsoleClientsIntent.SelectClient(tenantId))
            advanceUntilIdle()

            emit(ConsoleClientsIntent.OpenDocument(documentId.toString()))
            advanceUntilIdle()

            val loaded = states.value
            assertEquals(documentId, loaded.selectedDocument?.document?.id)
            assertNull(loaded.loadingDocumentId)
            assertEquals(listOf(documentId.toString()), documentUseCase.receivedDocumentIds)
        }
    }
}

private class FakeGetAccountMeUseCase(
    private val result: Result<AccountMeResponse>
) : GetAccountMeUseCase {
    var invocations: Int = 0

    override suspend fun invoke(): Result<AccountMeResponse> {
        invocations += 1
        return result
    }
}

private class FakeListConsoleClientsUseCase(
    private val result: Result<List<ConsoleClientSummary>>
) : ListConsoleClientsUseCase {
    val receivedFirmIds = mutableListOf<FirmId>()

    override suspend fun invoke(firmId: FirmId): Result<List<ConsoleClientSummary>> {
        receivedFirmIds += firmId
        return result
    }
}

private class FakeListConsoleClientDocumentsUseCase(
    private val result: Result<PaginatedResponse<DocumentListItemDto>>
) : ListConsoleClientDocumentsUseCase {
    val receivedTenantIds = mutableListOf<TenantId>()

    override suspend fun invoke(
        firmId: FirmId,
        tenantId: TenantId,
        page: Int,
        limit: Int
    ): Result<PaginatedResponse<DocumentListItemDto>> {
        receivedTenantIds += tenantId
        return result
    }
}

private class FakeGetConsoleClientDocumentUseCase(
    private val result: Result<DocumentDetailDto>
) : GetConsoleClientDocumentUseCase {
    val receivedDocumentIds = mutableListOf<String>()

    override suspend fun invoke(
        firmId: FirmId,
        tenantId: TenantId,
        documentId: String
    ): Result<DocumentDetailDto> {
        receivedDocumentIds += documentId
        return result
    }
}

private fun client(
    tenantId: String,
    companyName: String,
    vat: String?
): ConsoleClientSummary {
    return ConsoleClientSummary(
        tenantId = TenantId(tenantId),
        companyName = DisplayName(companyName),
        vatNumber = vat?.let(::VatNumber)
    )
}

private fun accountMe(firmId: FirmId): AccountMeResponse {
    return AccountMeResponse(
        user = User(
            id = UserId("00000000-0000-0000-0000-000000000011"),
            email = Email("firm@test.dokus"),
            firstName = Name("Firm"),
            lastName = Name("Owner"),
            emailVerified = true,
            isActive = true,
            createdAt = LocalDateTime(2026, 1, 1, 9, 0),
            updatedAt = LocalDateTime(2026, 1, 1, 9, 0),
        ),
        surface = SurfaceAvailability(
            canCompanyManager = false,
            canBookkeeperConsole = true,
            defaultSurface = AppSurface.BookkeeperConsole,
        ),
        firms = listOf(
            FirmWorkspaceSummary(
                id = firmId,
                name = DisplayName("Kantoor Boonen"),
                vatNumber = VatNumber("BE0123456789"),
                role = FirmRole.Owner,
                clientCount = 2,
            )
        ),
        tenants = emptyList()
    )
}

private fun listItemRecord(
    tenantId: TenantId,
    filename: String,
    documentId: DocumentId = DocumentId.generate(),
): DocumentListItemDto {
    return DocumentListItemDto(
        documentId = documentId,
        tenantId = tenantId,
        filename = filename,
        documentType = null,
        direction = null,
        documentStatus = null,
        ingestionStatus = null,
        effectiveOrigin = DocumentSource.Upload,
        uploadedAt = LocalDateTime(2026, 2, 1, 10, 0),
        sortDate = LocalDate(2026, 2, 1),
        counterpartyDisplayName = null,
        purposeRendered = null,
        totalAmount = null,
        currency = null,
    )
}

private fun documentDetailRecord(
    tenantId: TenantId,
    filename: String,
    documentId: DocumentId = DocumentId.generate(),
): DocumentDetailDto {
    return DocumentDetailDto(
        document = DocumentDto(
            id = documentId,
            tenantId = tenantId,
            filename = filename,
            uploadedAt = LocalDateTime(2026, 2, 1, 10, 0),
            sortDate = LocalDate(2026, 2, 1),
        ),
        draft = null,
        latestIngestion = null,
    )
}
