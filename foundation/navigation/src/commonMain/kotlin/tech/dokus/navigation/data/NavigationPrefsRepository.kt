package tech.dokus.navigation.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import tech.dokus.foundation.sstorage.SecureStorage

/**
 * Repository for navigation preferences.
 * Stores and retrieves expanded section states for the desktop navigation rail.
 */
class NavigationPrefsRepository(
    private val storage: SecureStorage
) {
    companion object {
        private const val KEY_PREFIX = "nav_section_expanded_"
    }

    private fun keyFor(sectionId: String) = "$KEY_PREFIX$sectionId"

    /**
     * Get whether a section is expanded.
     * Returns the default value if not set.
     */
    suspend fun isSectionExpanded(sectionId: String, defaultExpanded: Boolean): Boolean {
        return storage.get<Boolean>(keyFor(sectionId)) ?: defaultExpanded
    }

    /**
     * Set whether a section is expanded.
     */
    suspend fun setSectionExpanded(sectionId: String, expanded: Boolean) {
        storage.set(keyFor(sectionId), expanded)
    }

    /**
     * Observe section expanded state.
     */
    fun observeSectionExpanded(sectionId: String, defaultExpanded: Boolean): Flow<Boolean> {
        return storage.subscribe<Boolean>(keyFor(sectionId)).map { it ?: defaultExpanded }
    }
}
