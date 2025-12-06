package ai.dokus.foundation.domain.model.common

/**
 * Generic pagination state holder for infinite scroll flows.
 */
data class PaginationState<T : Any>(
    val currentPage: Int = 0,
    val pageSize: Int = 20,
    val isLoadingMore: Boolean = false,
    val hasMorePages: Boolean = true,
    val data: List<T> = emptyList()
) {
    companion object;
}
