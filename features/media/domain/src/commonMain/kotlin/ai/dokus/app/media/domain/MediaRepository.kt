package ai.dokus.app.media.domain

// Re-export MediaRepository from foundation/domain for backward compatibility
// New code should import from ai.dokus.foundation.domain.repository.MediaRepository directly
@Suppress("unused")
typealias MediaRepository = ai.dokus.foundation.domain.repository.MediaRepository
