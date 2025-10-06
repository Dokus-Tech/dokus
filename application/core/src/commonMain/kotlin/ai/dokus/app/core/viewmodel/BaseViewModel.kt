package ai.dokus.app.app.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Base ViewModel class that provides state management similar to Voyager's StateScreenModel
 */
abstract class BaseViewModel<S>(initialState: S) : ViewModel() {

    protected val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    /**
     * Provides access to viewModelScope for launching coroutines
     */
    protected val scope: CoroutineScope
        get() = viewModelScope
}