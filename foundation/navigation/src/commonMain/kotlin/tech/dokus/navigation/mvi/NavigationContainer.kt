package tech.dokus.navigation.mvi

import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.PipelineContext
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.dsl.withState
import pro.respawn.flowmvi.plugins.reduce
import tech.dokus.navigation.data.NavigationPrefsRepository

internal typealias NavigationCtx = PipelineContext<NavigationState, NavigationIntent, NavigationAction>

/**
 * Configuration for a navigation section.
 * Used to initialize the NavigationContainer with default expanded states.
 */
data class SectionConfig(
    val id: String,
    val defaultExpanded: Boolean
)

/**
 * Container for Navigation state management.
 *
 * Manages section expanded states for desktop rail navigation.
 * Uses accordion behavior: only one section can be expanded at a time.
 *
 * @param prefsRepository Repository for persisting navigation preferences
 * @param sectionConfigs List of section configurations with default expanded states
 */
class NavigationContainer(
    private val prefsRepository: NavigationPrefsRepository,
    private val sectionConfigs: List<SectionConfig>
) : Container<NavigationState, NavigationIntent, NavigationAction> {

    override val store: Store<NavigationState, NavigationIntent, NavigationAction> =
        store(NavigationState.Loading) {
            reduce { intent ->
                when (intent) {
                    is NavigationIntent.Initialize -> handleInitialize()
                    is NavigationIntent.ToggleSection -> handleToggleSection(intent.sectionId)
                }
            }
        }

    private suspend fun NavigationCtx.handleInitialize() {
        // Load expanded states from preferences, using defaults
        val expandedStates = sectionConfigs.associate { section ->
            section.id to prefsRepository.isSectionExpanded(section.id, section.defaultExpanded)
        }

        updateState {
            NavigationState.Ready(expandedSections = expandedStates)
        }
    }

    private suspend fun NavigationCtx.handleToggleSection(sectionId: String) {
        withState<NavigationState.Ready, _> {
            val currentlyExpanded = expandedSections[sectionId] ?: false
            val newExpanded = !currentlyExpanded

            // Accordion behavior: collapse all others when expanding
            val newStates = if (newExpanded) {
                // Collapse all sections, expand the clicked one
                expandedSections.keys.associateWith { id ->
                    id == sectionId
                }
            } else {
                // Just collapse the clicked section
                expandedSections + (sectionId to false)
            }

            // Persist preference
            prefsRepository.setSectionExpanded(sectionId, newExpanded)

            updateState {
                NavigationState.Ready(expandedSections = newStates)
            }
        }
    }
}
