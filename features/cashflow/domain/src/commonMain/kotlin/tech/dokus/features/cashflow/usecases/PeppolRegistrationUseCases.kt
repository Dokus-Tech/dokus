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
 * Accepts a VAT number - the backend converts it to PEPPOL ID format.
 */
interface VerifyPeppolIdUseCase {
    suspend operator fun invoke(vatNumber: VatNumber): Result<PeppolIdVerificationResult>
}

/**
 * Use case to enable PEPPOL for the tenant.
 */
interface EnablePeppolUseCase {
    /**
     * Backend derives everything from the current workspace (VAT, company details).
     * The client must not ask the user for VAT input here.
     */
    suspend operator fun invoke(): Result<PeppolRegistrationResponse>
}

/**
 * Use case to enable PEPPOL in "sending only" mode.
 *
 * Used when receiving is blocked because the PEPPOL inbox is connected elsewhere.
 */
interface EnablePeppolSendingOnlyUseCase {
    suspend operator fun invoke(): Result<PeppolRegistrationResponse>
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

/**
 * Use case to get PEPPOL activity timestamps.
 *
 * Returns the most recent inbound and outbound transmission timestamps
 * for displaying activity status in workspace settings.
 */
interface GetPeppolActivityUseCase {
    suspend operator fun invoke(): Result<tech.dokus.domain.model.PeppolActivityDto?>
}
