package tech.dokus.backend.worker

import tech.dokus.backend.services.business.LogoSelectionTrace
import tech.dokus.domain.enums.Language

internal data class BusinessSubjectContext(
    val name: String,
    val vatNumber: String? = null,
    val country: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val email: String? = null,
    val phone: String? = null,
    val language: Language,
)

internal data class LogoResolutionResult(
    val storageKey: String?,
    val trace: LogoSelectionTrace,
)
