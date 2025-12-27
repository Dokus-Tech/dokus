package tech.dokus.domain.model.common

import kotlinx.serialization.Serializable

/**
 * Generic paginated response wrapper used across backend and frontend.
 */
@Serializable
data class PaginatedResponse<T>(
    val items: List<T>,
    val total: Long,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean = offset + items.size < total
)