package tech.dokus.app.navigation

import kotlin.concurrent.atomics.AtomicLong
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@OptIn(ExperimentalAtomicApi::class)
internal object SearchFocusRequestBus {
    private val nextId = AtomicLong(0L)
    private val pending = MutableStateFlow(0L)

    val focusRequestId = pending.asStateFlow()

    fun requestFocus() {
        pending.value = nextId.addAndFetch(1L)
    }
}
