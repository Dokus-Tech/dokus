package tech.dokus.features.cashflow.gateway

import tech.dokus.domain.enums.PeppolStatus
import tech.dokus.domain.enums.PeppolTransmissionDirection
import tech.dokus.domain.model.PeppolTransmissionDto

/**
 * Gateway for listing Peppol transmissions.
 */
interface PeppolTransmissionsGateway {
    suspend fun listPeppolTransmissions(
        direction: PeppolTransmissionDirection? = null,
        status: PeppolStatus? = null,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<PeppolTransmissionDto>>
}
