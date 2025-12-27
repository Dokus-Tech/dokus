package tech.dokus.domain.model.common

/**
 * Extension properties and functions for PaginationState to simplify pagination logic.
 */

/**
 * Returns true if there is a previous page available.
 */
val <T : Any> PaginationState<T>.hasPreviousPage: Boolean
    get() = currentPage > 0

/**
 * Returns true if there is a next page available.
 * This is an alias for hasMorePages for semantic clarity in UI contexts.
 */
val <T : Any> PaginationState<T>.hasNextPage: Boolean
    get() = hasMorePages

/**
 * Calculates total number of pages based on total item count.
 *
 * @param totalItems Total number of items across all pages
 * @return Number of pages needed to display all items
 */
fun <T : Any> PaginationState<T>.totalPages(totalItems: Int): Int {
    if (totalItems <= 0 || pageSize <= 0) return 1
    return ((totalItems - 1) / pageSize) + 1
}

/**
 * Gets the current page slice from a complete list of data.
 * Useful for client-side pagination where all data is loaded but displayed in pages.
 *
 * @param allData Complete list of all items
 * @return Sublist containing only items for the current page
 */
fun <T : Any> PaginationState<T>.getCurrentPageData(allData: List<T>): List<T> {
    if (allData.isEmpty()) return emptyList()
    val start = currentPage * pageSize
    val end = minOf(start + pageSize, allData.size)
    return if (start < allData.size) {
        allData.subList(start, end)
    } else {
        emptyList()
    }
}

/**
 * Object providing factory methods for PaginationState.
 */
object PaginationStateCompanion {
    /**
     * Creates a PaginationState for client-side pagination from a complete data set.
     *
     * @param allData Complete list of all items
     * @param currentPage The page to display (0-indexed)
     * @param pageSize Number of items per page
     * @return New PaginationState configured for the given page
     */
    fun <T : Any> fromLocalData(
        allData: List<T>,
        currentPage: Int,
        pageSize: Int
    ): PaginationState<T> {
        val totalPages = if (allData.isEmpty() || pageSize <= 0) 1 else ((allData.size - 1) / pageSize) + 1
        val start = currentPage * pageSize
        val end = minOf(start + pageSize, allData.size)
        val pageData = if (start < allData.size) allData.subList(start, end) else emptyList()

        return PaginationState(
            currentPage = currentPage,
            pageSize = pageSize,
            isLoadingMore = false,
            hasMorePages = currentPage < totalPages - 1,
            data = pageData
        )
    }
}
