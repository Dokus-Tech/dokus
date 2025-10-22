package ai.dokus.foundation.navigation.local

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages the visibility state of the secondary navigation panel.
 * Navigation itself is handled by the NavController.
 */
class SecondaryNavigationState {
    private val _isPanelVisible = MutableStateFlow(false)
    val isPanelVisible: StateFlow<Boolean> = _isPanelVisible.asStateFlow()

    private val _complimentary = MutableStateFlow(true)
    val complimentary: StateFlow<Boolean> = _complimentary.asStateFlow()

    /**
     * Shows the secondary panel.
     */
    fun showPanel(complimentary: Boolean = true) {
        _isPanelVisible.value = true
        _complimentary.value = complimentary
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
}