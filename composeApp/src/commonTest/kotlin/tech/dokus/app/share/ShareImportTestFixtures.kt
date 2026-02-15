package tech.dokus.app.share

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.asbtractions.TokenManager
import tech.dokus.domain.enums.DocumentIntakeOutcome
import tech.dokus.domain.enums.DocumentSource
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.Permission
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.ids.DocumentId
import tech.dokus.domain.ids.DocumentSourceId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.UserId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.DocumentDto
import tech.dokus.domain.model.DocumentIntakeOutcomeDto
import tech.dokus.domain.model.DocumentIntakeResult
import tech.dokus.domain.model.Tenant
import tech.dokus.domain.model.auth.JwtClaims
import tech.dokus.domain.model.auth.TenantScope
import tech.dokus.features.auth.usecases.GetLastSelectedTenantIdUseCase
import tech.dokus.features.auth.usecases.ListMyTenantsUseCase
import tech.dokus.features.auth.usecases.SelectTenantUseCase
import tech.dokus.features.cashflow.usecases.UploadDocumentUseCase

internal class FakeTokenManager(
    isAuthenticated: Boolean,
    private val claims: JwtClaims? = null
) : TokenManager {
    override val isAuthenticated = MutableStateFlow(isAuthenticated)

    override suspend fun getValidAccessToken(): String? = null

    override suspend fun getRefreshToken(): String? = null

    override suspend fun refreshToken(force: Boolean): String? = null

    override suspend fun onAuthenticationFailed() = Unit

    override suspend fun getCurrentClaims(): JwtClaims? = claims
}

internal class FakeGetLastSelectedTenantIdUseCase(
    private val tenantId: TenantId? = null
) : GetLastSelectedTenantIdUseCase {
    override suspend fun invoke(): TenantId? = tenantId
}

internal class FakeListMyTenantsUseCase(
    private val result: Result<List<Tenant>>
) : ListMyTenantsUseCase {
    override suspend fun invoke(): Result<List<Tenant>> = result
}

internal class FakeSelectTenantUseCase(
    private val resultByTenantId: Map<TenantId, Result<Unit>> = emptyMap(),
    private val defaultResult: Result<Unit> = Result.success(Unit)
) : SelectTenantUseCase {
    val invocations = mutableListOf<TenantId>()

    override suspend fun invoke(tenantId: TenantId): Result<Unit> {
        invocations += tenantId
        return resultByTenantId[tenantId] ?: defaultResult
    }
}

internal class FakeUploadDocumentUseCase(
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
    ): Result<DocumentIntakeResult> {
        val invocationIndex = invocations
        invocations += 1
        uploadedFilenames += filename

        val progressPoints = progressPointsByInvocation[invocationIndex] ?: defaultProgressPoints
        progressPoints.forEach(onProgress)

        val docResult = results.getOrNull(invocationIndex)
            ?: results.lastOrNull()
            ?: return Result.failure(IllegalStateException("No upload result configured"))

        return docResult.map { doc ->
            DocumentIntakeResult(
                document = doc,
                intake = DocumentIntakeOutcomeDto(
                    outcome = DocumentIntakeOutcome.NewDocument,
                    sourceId = DocumentSourceId.parse(doc.id.toString()),
                    documentId = doc.id
                )
            )
        }
    }
}

internal fun testSharedFile(name: String = "invoice.pdf"): SharedImportFile = SharedImportFile(
    name = name,
    bytes = byteArrayOf(1, 2, 3),
    mimeType = "application/pdf"
)

internal fun testTenant(
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

internal fun testDocument(
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

internal fun testClaims(tenantId: TenantId): JwtClaims = JwtClaims(
    userId = UserId("00000000-0000-0000-0000-000000000777"),
    email = "test@dokus.tech",
    tenant = TenantScope(
        tenantId = tenantId,
        permissions = setOf(Permission.InvoicesRead),
        subscriptionTier = SubscriptionTier.Core,
        role = null
    ),
    iat = 1_700_000_000L,
    exp = 1_800_000_000L,
    jti = "jti-test"
)

internal fun clearPendingSharedFiles() {
    while (!ExternalShareImportHandler.consumePendingFiles().isNullOrEmpty()) {
        // Clear all pending payloads left by previous tests.
    }
}
