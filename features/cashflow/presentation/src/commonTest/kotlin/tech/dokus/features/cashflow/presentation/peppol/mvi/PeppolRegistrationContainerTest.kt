package tech.dokus.features.cashflow.presentation.peppol.mvi

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import pro.respawn.flowmvi.test.subscribeAndTest
import tech.dokus.domain.DisplayName
import tech.dokus.domain.LegalName
import tech.dokus.domain.enums.Language
import tech.dokus.domain.enums.SubscriptionTier
import tech.dokus.domain.enums.TenantStatus
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolIdVerificationResult
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.PeppolRegistrationResponse
import tech.dokus.domain.model.Tenant
import tech.dokus.features.auth.usecases.GetCurrentTenantUseCase
import tech.dokus.features.cashflow.usecases.EnablePeppolSendingOnlyUseCase
import tech.dokus.features.cashflow.usecases.EnablePeppolUseCase
import tech.dokus.features.cashflow.usecases.GetPeppolRegistrationUseCase
import tech.dokus.features.cashflow.usecases.PollPeppolTransferUseCase
import tech.dokus.features.cashflow.usecases.VerifyPeppolIdUseCase
import tech.dokus.features.cashflow.usecases.WaitForPeppolTransferUseCase
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isSuccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalUuidApi::class)
class PeppolRegistrationContainerTest {

    @Test
    fun `not now navigates home without calling backend operations`() = runTest {
        val verify = FakeVerifyPeppolIdUseCase(Result.success(availableVerificationResult()))
        val getRegistration = FakeGetPeppolRegistrationUseCase(Result.success(null))

        val container = createContainer(
            verifyPeppolId = verify,
            getRegistration = getRegistration
        )

        container.store.subscribeAndTest {
            PeppolRegistrationIntent.NotNow resultsIn PeppolRegistrationAction.NavigateToHome
            assertEquals(0, getRegistration.invocations)
            assertEquals(0, verify.invocations)
        }
    }

    @Test
    fun `refresh emits nonrecoverable error when peppol directory is unavailable`() = runTest {
        val verify = FakeVerifyPeppolIdUseCase(Result.failure(DokusException.PeppolDirectoryUnavailable()))
        val container = createContainer(verifyPeppolId = verify)

        container.store.subscribeAndTest {
            emit(PeppolRegistrationIntent.Refresh)
            advanceUntilIdle()

            val state = states.value
            assertTrue(state.setupContext.isError())
            assertIs<DokusException.PeppolDirectoryUnavailable>(state.setupContext.exception)
            assertTrue(state.setupContext.exception.recoverable)
            assertEquals(1, verify.invocations)
        }
    }

    @Test
    fun `refresh emits recoverable error for connection failures`() = runTest {
        val verify = FakeVerifyPeppolIdUseCase(Result.failure(DokusException.ConnectionError()))
        val container = createContainer(verifyPeppolId = verify)

        container.store.subscribeAndTest {
            emit(PeppolRegistrationIntent.Refresh)
            advanceUntilIdle()

            val state = states.value
            assertTrue(state.setupContext.isError())
            assertIs<DokusException.ConnectionError>(state.setupContext.exception)
            assertTrue(state.setupContext.exception.recoverable)
            assertEquals(1, verify.invocations)
        }
    }

    private fun createContainer(
        getCurrentTenant: GetCurrentTenantUseCase = FakeGetCurrentTenantUseCase(Result.success(sampleTenant())),
        getRegistration: GetPeppolRegistrationUseCase = FakeGetPeppolRegistrationUseCase(Result.success(null)),
        verifyPeppolId: VerifyPeppolIdUseCase = FakeVerifyPeppolIdUseCase(Result.success(availableVerificationResult())),
        enablePeppol: EnablePeppolUseCase = FakeEnablePeppolUseCase(),
        enableSendingOnly: EnablePeppolSendingOnlyUseCase = FakeEnableSendingOnlyUseCase(),
        waitForTransfer: WaitForPeppolTransferUseCase = FakeWaitForTransferUseCase(),
        pollTransfer: PollPeppolTransferUseCase = FakePollTransferUseCase(),
    ): PeppolRegistrationContainer {
        return PeppolRegistrationContainer(
            getCurrentTenant = getCurrentTenant,
            getRegistration = getRegistration,
            verifyPeppolId = verifyPeppolId,
            enablePeppol = enablePeppol,
            enableSendingOnly = enableSendingOnly,
            waitForTransfer = waitForTransfer,
            pollTransfer = pollTransfer
        )
    }

    private class FakeGetCurrentTenantUseCase(
        private val result: Result<Tenant?>
    ) : GetCurrentTenantUseCase {
        override suspend fun invoke(): Result<Tenant?> = result
    }

    private class FakeGetPeppolRegistrationUseCase(
        private val result: Result<PeppolRegistrationDto?>
    ) : GetPeppolRegistrationUseCase {
        var invocations: Int = 0

        override suspend fun invoke(): Result<PeppolRegistrationDto?> {
            invocations += 1
            return result
        }
    }

    private class FakeVerifyPeppolIdUseCase(
        private val result: Result<PeppolIdVerificationResult>
    ) : VerifyPeppolIdUseCase {
        var invocations: Int = 0

        override suspend fun invoke(vatNumber: VatNumber): Result<PeppolIdVerificationResult> {
            invocations += 1
            return result
        }
    }

    private class FakeEnablePeppolUseCase : EnablePeppolUseCase {
        override suspend fun invoke(): Result<PeppolRegistrationResponse> =
            Result.failure(IllegalStateException("Not used in this test"))
    }

    private class FakeEnableSendingOnlyUseCase : EnablePeppolSendingOnlyUseCase {
        override suspend fun invoke(): Result<PeppolRegistrationResponse> =
            Result.failure(IllegalStateException("Not used in this test"))
    }

    private class FakeWaitForTransferUseCase : WaitForPeppolTransferUseCase {
        override suspend fun invoke(): Result<PeppolRegistrationResponse> =
            Result.failure(IllegalStateException("Not used in this test"))
    }

    private class FakePollTransferUseCase : PollPeppolTransferUseCase {
        override suspend fun invoke(): Result<PeppolRegistrationResponse> =
            Result.failure(IllegalStateException("Not used in this test"))
    }

    private fun sampleTenant(): Tenant = Tenant(
        id = TenantId("00000000-0000-0000-0000-000000000001"),
        type = TenantType.Company,
        legalName = LegalName("Dokus Ltd"),
        displayName = DisplayName("Dokus"),
        subscription = SubscriptionTier.CoreFounder,
        status = TenantStatus.Active,
        language = Language.En,
        vatNumber = VatNumber("BE0777887045"),
        createdAt = LocalDateTime(2024, 1, 1, 0, 0),
        updatedAt = LocalDateTime(2024, 1, 1, 0, 0),
        avatar = null
    )

    private fun availableVerificationResult() = PeppolIdVerificationResult(
        peppolId = "0208:BE0777887045",
        isBlocked = false,
        blockedBy = null,
        canProceed = true
    )
}
