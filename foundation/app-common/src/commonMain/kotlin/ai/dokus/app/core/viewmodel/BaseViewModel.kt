package ai.dokus.app.core.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

abstract class BaseViewModel<S>(initialState: S) : ViewModel() {

    protected val mutableState = MutableStateFlow(initialState)
    val state: StateFlow<S> = mutableState.asStateFlow()

    /**
     * Provides access to viewModelScope for launching coroutines
     */
    protected val scope: CoroutineScope
        get() = viewModelScope
}