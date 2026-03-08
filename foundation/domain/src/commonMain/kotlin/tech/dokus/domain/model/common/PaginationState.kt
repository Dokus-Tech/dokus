package tech.dokus.domain.model.common

/**
 * Generic pagination state holder for infinite scroll flows.
 */
data class PaginationState<T : Any>(
    val currentPage: Int = 0,
    val pageSize: Int = 20,
    val hasMorePages: Boolean = true,
    val data: List<T> = emptyList()
) {
    companion object;
}
