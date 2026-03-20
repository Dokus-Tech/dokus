package tech.dokus.backend.services.documents.resolution

import tech.dokus.domain.ids.TenantId
import tech.dokus.domain.model.contact.ContactResolution
import tech.dokus.domain.model.contact.CounterpartySnapshotDto
import tech.dokus.domain.model.contact.SuggestedContactDto

data class ResolverInput(
    val tenantId: TenantId,
    val snapshot: CounterpartySnapshotDto,
    val strictAutoLink: Boolean
)

sealed interface ResolverOutcome {
    data class Resolved(val resolution: ContactResolution) : ResolverOutcome
    data class Partial(val suggestions: List<SuggestedContactDto> = emptyList()) : ResolverOutcome
}
