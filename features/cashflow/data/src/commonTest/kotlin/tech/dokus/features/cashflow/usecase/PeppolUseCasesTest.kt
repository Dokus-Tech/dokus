package tech.dokus.features.cashflow.usecase

import kotlinx.datetime.LocalDateTime
import kotlinx.coroutines.test.runTest
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolSettingsId
import tech.dokus.domain.ids.PeppolTransmissionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolSettingsDto
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolVerifyResponse
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import tech.dokus.features.cashflow.gateway.PeppolConnectionGateway
import tech.dokus.features.cashflow.gateway.PeppolInboxGateway
import tech.dokus.features.cashflow.gateway.PeppolInvoiceGateway
import tech.dokus.features.cashflow.gateway.PeppolRecipientGateway
import tech.dokus.features.cashflow.gateway.PeppolTransmissionsGateway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalUuidApi::class)
class PeppolUseCasesTest {

    @Test
    fun connectPeppolDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val response = PeppolConnectResponse(status = PeppolConnectStatus.Connected)
        gateway.connectResult = Result.success(response)

        val useCase = ConnectPeppolUseCaseImpl(gateway)
        val request = PeppolConnectRequest(
            apiKey = "key",
            apiSecret = "secret",
            isEnabled = true,
            testMode = false
        )

        val result = useCase(request)

        assertEquals(request, gateway.lastConnectRequest)
        assertEquals(response, result.getOrNull())
    }

    @Test
    fun getPeppolSettingsDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val settings = sampleSettings()
        gateway.settingsResult = Result.success(settings)

        val useCase = GetPeppolSettingsUseCaseImpl(gateway)
        val result = useCase()

        assertTrue(gateway.getSettingsCalled)
        assertEquals(settings, result.getOrNull())
    }

    @Test
    fun deletePeppolSettingsDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        gateway.deleteSettingsResult = Result.success(Unit)

        val useCase = DeletePeppolSettingsUseCaseImpl(gateway)
        val result = useCase()

        assertTrue(gateway.deleteSettingsCalled)
        assertTrue(result.isSuccess)
    }

    @Test
    fun listPeppolTransmissionsDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val transmission = sampleTransmission()
        gateway.listResult = Result.success(listOf(transmission))

        val useCase = ListPeppolTransmissionsUseCaseImpl(gateway)
        val result = useCase(
            direction = PeppolTransmissionDirection.Outbound,
            status = PeppolStatus.Pending,
            limit = 25,
            offset = 50
        )

        assertEquals(
            ListArgs(
                direction = PeppolTransmissionDirection.Outbound,
                status = PeppolStatus.Pending,
                limit = 25,
                offset = 50
            ),
            gateway.lastListArgs
        )
        assertEquals(listOf(transmission), result.getOrNull())
    }

    @Test
    fun verifyRecipientDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val response = PeppolVerifyResponse(registered = true, participantId = "0208:BE0123456789")
        gateway.verifyResult = Result.success(response)

        val useCase = VerifyPeppolRecipientUseCaseImpl(gateway)
        val result = useCase("0208:BE0123456789")

        assertEquals("0208:BE0123456789", gateway.lastVerifyId)
        assertEquals(response, result.getOrNull())
    }

    @Test
    fun validateInvoiceDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val response = PeppolValidationResult(isValid = true)
        gateway.validateResult = Result.success(response)

        val useCase = ValidateInvoiceForPeppolUseCaseImpl(gateway)
        val invoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000010")
        val result = useCase(invoiceId)

        assertEquals(invoiceId, gateway.lastValidationInvoiceId)
        assertEquals(response, result.getOrNull())
    }

    @Test
    fun sendInvoiceDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val response = SendInvoiceViaPeppolResponse(
            transmissionId = PeppolTransmissionId.parse("00000000-0000-0000-0000-000000000020"),
            status = PeppolStatus.Sent
        )
        gateway.sendResult = Result.success(response)

        val useCase = SendInvoiceViaPeppolUseCaseImpl(gateway)
        val invoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000011")
        val result = useCase(invoiceId)

        assertEquals(invoiceId, gateway.lastSendInvoiceId)
        assertEquals(response, result.getOrNull())
    }

    @Test
    fun pollInboxDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val response = PeppolInboxPollResponse(newDocuments = 0, processedDocuments = emptyList())
        gateway.pollResult = Result.success(response)

        val useCase = PollPeppolInboxUseCaseImpl(gateway)
        val result = useCase()

        assertTrue(gateway.pollCalled)
        assertEquals(response, result.getOrNull())
    }

    @Test
    fun getTransmissionForInvoiceDelegatesToGateway() = runTest {
        val gateway = FakePeppolGateway()
        val transmission = sampleTransmission()
        gateway.getTransmissionResult = Result.success(transmission)

        val useCase = GetPeppolTransmissionForInvoiceUseCaseImpl(gateway)
        val invoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000012")
        val result = useCase(invoiceId)

        assertEquals(invoiceId, gateway.lastTransmissionInvoiceId)
        assertEquals(transmission, result.getOrNull())
    }

    private fun sampleSettings(): PeppolSettingsDto {
        return PeppolSettingsDto(
            id = PeppolSettingsId.parse("00000000-0000-0000-0000-000000000001"),
            tenantId = TenantId("00000000-0000-0000-0000-000000000002"),
            companyId = "company",
            peppolId = tech.dokus.domain.ids.PeppolId("0208:BE0123456789"),
            isEnabled = true,
            testMode = false,
            createdAt = LocalDateTime(2024, 1, 1, 0, 0),
            updatedAt = LocalDateTime(2024, 1, 1, 0, 0)
        )
    }

    private fun sampleTransmission(): PeppolTransmissionDto {
        return PeppolTransmissionDto(
            id = PeppolTransmissionId.parse("00000000-0000-0000-0000-000000000003"),
            tenantId = TenantId("00000000-0000-0000-0000-000000000004"),
            direction = PeppolTransmissionDirection.Outbound,
            documentType = PeppolDocumentType.Invoice,
            status = PeppolStatus.Pending,
            createdAt = LocalDateTime(2024, 1, 2, 0, 0),
            updatedAt = LocalDateTime(2024, 1, 2, 0, 0)
        )
    }

    private data class ListArgs(
        val direction: PeppolTransmissionDirection?,
        val status: PeppolStatus?,
        val limit: Int,
        val offset: Int
    )

    private class FakePeppolGateway :
        PeppolConnectionGateway,
        PeppolTransmissionsGateway,
        PeppolRecipientGateway,
        PeppolInvoiceGateway,
        PeppolInboxGateway {
        var lastConnectRequest: PeppolConnectRequest? = null
        var connectResult: Result<PeppolConnectResponse> = Result.failure(IllegalStateException("missing"))

        var getSettingsCalled: Boolean = false
        var settingsResult: Result<PeppolSettingsDto?> = Result.success(null)

        var deleteSettingsCalled: Boolean = false
        var deleteSettingsResult: Result<Unit> = Result.success(Unit)

        var lastListArgs: ListArgs? = null
        var listResult: Result<List<PeppolTransmissionDto>> = Result.success(emptyList())

        var lastVerifyId: String? = null
        var verifyResult: Result<PeppolVerifyResponse> = Result.failure(IllegalStateException("missing"))

        var lastValidationInvoiceId: InvoiceId? = null
        var validateResult: Result<PeppolValidationResult> = Result.failure(IllegalStateException("missing"))

        var lastSendInvoiceId: InvoiceId? = null
        var sendResult: Result<SendInvoiceViaPeppolResponse> = Result.failure(IllegalStateException("missing"))

        var pollCalled: Boolean = false
        var pollResult: Result<PeppolInboxPollResponse> = Result.failure(IllegalStateException("missing"))

        var lastTransmissionInvoiceId: InvoiceId? = null
        var getTransmissionResult: Result<PeppolTransmissionDto?> = Result.success(null)

        override suspend fun connectPeppol(request: PeppolConnectRequest): Result<PeppolConnectResponse> {
            lastConnectRequest = request
            return connectResult
        }

        override suspend fun getPeppolSettings(): Result<PeppolSettingsDto?> {
            getSettingsCalled = true
            return settingsResult
        }

        override suspend fun deletePeppolSettings(): Result<Unit> {
            deleteSettingsCalled = true
            return deleteSettingsResult
        }

        override suspend fun listPeppolTransmissions(
            direction: PeppolTransmissionDirection?,
            status: PeppolStatus?,
            limit: Int,
            offset: Int
        ): Result<List<PeppolTransmissionDto>> {
            lastListArgs = ListArgs(direction, status, limit, offset)
            return listResult
        }

        override suspend fun verifyPeppolRecipient(peppolId: String): Result<PeppolVerifyResponse> {
            lastVerifyId = peppolId
            return verifyResult
        }

        override suspend fun validateInvoiceForPeppol(invoiceId: InvoiceId): Result<PeppolValidationResult> {
            lastValidationInvoiceId = invoiceId
            return validateResult
        }

        override suspend fun sendInvoiceViaPeppol(invoiceId: InvoiceId): Result<SendInvoiceViaPeppolResponse> {
            lastSendInvoiceId = invoiceId
            return sendResult
        }

        override suspend fun pollPeppolInbox(): Result<PeppolInboxPollResponse> {
            pollCalled = true
            return pollResult
        }

        override suspend fun getPeppolTransmissionForInvoice(
            invoiceId: InvoiceId
        ): Result<PeppolTransmissionDto?> {
            lastTransmissionInvoiceId = invoiceId
            return getTransmissionResult
        }
    }
}
