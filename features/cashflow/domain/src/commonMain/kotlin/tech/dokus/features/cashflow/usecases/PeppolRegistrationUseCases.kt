package tech.dokus.features.cashflow.usecases

import tech.dokus.domain.ids.VatNumber
import tech.dokus.domain.model.PeppolIdVerificationResult
import tech.dokus.domain.model.PeppolRegistrationDto
import tech.dokus.domain.model.PeppolRegistrationResponse

/**
 * Use case to get the current PEPPOL registration status.
 */
interface GetPeppolRegistrationUseCase {
    suspend operator fun invoke(): Result<PeppolRegistrationDto?>
}

/**
 * Use case to verify if a PEPPOL ID is available for registration.
 */
interface VerifyPeppolIdUseCase {
    suspend operator fun invoke(peppolId: String): Result<PeppolIdVerificationResult>
}

/**
 * Use case to enable PEPPOL for the tenant.
 */
interface EnablePeppolUseCase {
    suspend operator fun invoke(vatNumber: VatNumber): Result<PeppolRegistrationResponse>
}

/**
 * Use case to opt for waiting for PEPPOL ID transfer.
 */
interface WaitForPeppolTransferUseCase {
    suspend operator fun invoke(): Result<PeppolRegistrationResponse>
}

/**
 * Use case to opt out of PEPPOL via Dokus.
 */
interface OptOutPeppolUseCase {
    suspend operator fun invoke(): Result<Unit>
}

/**
 * Use case to manually poll for PEPPOL transfer status.
 */
interface PollPeppolTransferUseCase {
    suspend operator fun invoke(): Result<PeppolRegistrationResponse>
}
