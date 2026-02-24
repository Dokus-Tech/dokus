package tech.dokus.domain.routes

import io.ktor.resources.Resource
import kotlinx.serialization.Serializable
import tech.dokus.domain.model.UnifiedSearchScope

/**
 * Type-safe route definitions for unified search.
 * Base path: /api/v1/search
 */
@Serializable
@Resource("/api/v1/search")
class Search(
    val query: String = "",
    val scope: UnifiedSearchScope = UnifiedSearchScope.All,
    val limit: Int = 20,
    val suggestionLimit: Int = 8,
)
