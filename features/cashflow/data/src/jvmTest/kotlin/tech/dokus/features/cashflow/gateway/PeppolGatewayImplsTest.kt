package tech.dokus.features.cashflow.gateway

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDateTime
import tech.dokus.domain.model.PeppolConnectStatus
import tech.dokus.domain.enums.PeppolDocumentType
import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.ids.InvoiceId
import tech.dokus.domain.ids.PeppolTransmissionId
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.PeppolConnectRequest
import tech.dokus.domain.model.PeppolConnectResponse
import tech.dokus.domain.model.PeppolInboxPollResponse
import tech.dokus.domain.model.PeppolTransmissionDto
import tech.dokus.domain.model.PeppolValidationResult
import tech.dokus.domain.model.PeppolVerifyResponse
import tech.dokus.domain.model.SendInvoiceViaPeppolResponse
import tech.dokus.features.cashflow.datasource.CashflowRemoteDataSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeppolGatewayImplsTest {

    @Test
    fun peppolConnectionGatewayDelegates() = runBlocking {
        val remote = mockk<CashflowRemoteDataSource>()
        val request = PeppolConnectRequest(
            apiKey = "key",
            apiSecret = "secret",
            isEnabled = true,
            testMode = false
        )
        val response = PeppolConnectResponse(status = PeppolConnectStatus.Connected)
        coEvery { remote.connectPeppol(request) } returns Result.success(response)
        coEvery { remote.getPeppolSettings() } returns Result.success(null)
        coEvery { remote.deletePeppolSettings() } returns Result.success(Unit)

        val gateway = PeppolConnectionGatewayImpl(remote)

        assertEquals(response, gateway.connectPeppol(request).getOrNull())
        assertEquals(null, gateway.getPeppolSettings().getOrNull())
        assertTrue(gateway.deletePeppolSettings().isSuccess)

        coVerify { remote.connectPeppol(request) }
        coVerify { remote.getPeppolSettings() }
        coVerify { remote.deletePeppolSettings() }
    }

    @Test
    fun peppolRecipientGatewayDelegates() = runBlocking {
        val remote = mockk<CashflowRemoteDataSource>()
        val response = PeppolVerifyResponse(registered = true, participantId = "0208:BE0123456789")
        coEvery { remote.verifyPeppolRecipient("0208:BE0123456789") } returns Result.success(response)

        val gateway = PeppolRecipientGatewayImpl(remote)

        assertEquals(response, gateway.verifyPeppolRecipient("0208:BE0123456789").getOrNull())
        coVerify { remote.verifyPeppolRecipient("0208:BE0123456789") }
    }

    @Test
    fun peppolInvoiceGatewayDelegates() = runBlocking {
        val remote = mockk<CashflowRemoteDataSource>()
        val invoiceId = InvoiceId.parse("00000000-0000-0000-0000-000000000010")
        val validation = PeppolValidationResult(isValid = true)
        val sendResponse = SendInvoiceViaPeppolResponse(
            transmissionId = PeppolTransmissionId.parse("00000000-0000-0000-0000-000000000020"),
            status = PeppolStatus.Sent
        )
        val transmission = sampleTransmission()

        coEvery { remote.validateInvoiceForPeppol(invoiceId) } returns Result.success(validation)
        coEvery { remote.sendInvoiceViaPeppol(invoiceId) } returns Result.success(sendResponse)
        coEvery { remote.getPeppolTransmissionForInvoice(invoiceId) } returns Result.success(transmission)

        val gateway = PeppolInvoiceGatewayImpl(remote)

        assertEquals(validation, gateway.validateInvoiceForPeppol(invoiceId).getOrNull())
        assertEquals(sendResponse, gateway.sendInvoiceViaPeppol(invoiceId).getOrNull())
        assertEquals(transmission, gateway.getPeppolTransmissionForInvoice(invoiceId).getOrNull())

        coVerify { remote.validateInvoiceForPeppol(invoiceId) }
        coVerify { remote.sendInvoiceViaPeppol(invoiceId) }
        coVerify { remote.getPeppolTransmissionForInvoice(invoiceId) }
    }

    @Test
    fun peppolInboxGatewayDelegates() = runBlocking {
        val remote = mockk<CashflowRemoteDataSource>()
        val response = PeppolInboxPollResponse(newDocuments = 0, processedDocuments = emptyList())
        coEvery { remote.pollPeppolInbox() } returns Result.success(response)

        val gateway = PeppolInboxGatewayImpl(remote)

        assertEquals(response, gateway.pollPeppolInbox().getOrNull())
        coVerify { remote.pollPeppolInbox() }
    }

    @Test
    fun peppolTransmissionsGatewayDelegates() = runBlocking {
        val remote = mockk<CashflowRemoteDataSource>()
        val transmission = sampleTransmission()
        coEvery {
            remote.listPeppolTransmissions(
                direction = PeppolTransmissionDirection.Outbound,
                status = PeppolStatus.Pending,
                limit = 25,
                offset = 50
            )
        } returns Result.success(listOf(transmission))

        val gateway = PeppolTransmissionsGatewayImpl(remote)

        assertEquals(
            listOf(transmission),
            gateway.listPeppolTransmissions(
                direction = PeppolTransmissionDirection.Outbound,
                status = PeppolStatus.Pending,
                limit = 25,
                offset = 50
            ).getOrNull()
        )

        coVerify {
            remote.listPeppolTransmissions(
                direction = PeppolTransmissionDirection.Outbound,
                status = PeppolStatus.Pending,
                limit = 25,
                offset = 50
            )
        }
    }

    private fun sampleTransmission(): PeppolTransmissionDto {
        return PeppolTransmissionDto(
            id = PeppolTransmissionId.parse("00000000-0000-0000-0000-000000000030"),
            tenantId = TenantId("00000000-0000-0000-0000-000000000031"),
            direction = PeppolTransmissionDirection.Outbound,
            documentType = PeppolDocumentType.Invoice,
            status = PeppolStatus.Pending,
            createdAt = LocalDateTime(2024, 1, 2, 0, 0),
            updatedAt = LocalDateTime(2024, 1, 2, 0, 0)
        )
    }
}
