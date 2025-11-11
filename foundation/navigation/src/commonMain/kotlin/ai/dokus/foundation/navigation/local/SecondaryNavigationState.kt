package ai.dokus.foundation.navigation.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the visibility state and type of the secondary navigation panel.
 * Navigation itself is handled by the NavController.
 */
class SecondaryNavigationState {
    private val _isPanelVisible = MutableStateFlow(false)
    val isPanelVisible: StateFlow<Boolean> = _isPanelVisible.asStateFlow()

    private val _panelType = MutableStateFlow(SecondaryPanelType.Complimentary)
    val panelType: StateFlow<SecondaryPanelType> = _panelType.asStateFlow()

    /**
     * Shows the secondary panel with the specified type.
     *
     * Thread-safe: Updates panel type before visibility to ensure consistent state.
     *
     * @param panelType The type of panel to display (Inline, Complimentary, or Info)
     */
    fun showPanel(panelType: SecondaryPanelType = SecondaryPanelType.Complimentary) {
        // Update panel type first to ensure it's set before visibility triggers observers
        _panelType.value = panelType
        _isPanelVisible.value = true
    }

    /**
     * Hides the secondary panel.
     */
    fun hidePanel() {
        _isPanelVisible.value = false
    }

    /**
     * Toggles the panel visibility.
     */
    fun togglePanel() {
        _isPanelVisible.value = !_isPanelVisible.value
    }

    /**
     * Sets the panel visibility to a specific state.
     */
    fun setPanelVisibility(visible: Boolean) {
        _isPanelVisible.value = visible
    }

    /**
     * Updates the panel type without changing visibility.
     */
    fun setPanelType(type: SecondaryPanelType) {
        _panelType.value = type
    }
}