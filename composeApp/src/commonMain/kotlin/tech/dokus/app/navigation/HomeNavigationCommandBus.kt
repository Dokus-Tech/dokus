package tech.dokus.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class HomeNavigationEnvelope(
    val id: Long,
    val command: HomeNavigationCommand,
)

internal object HomeNavigationCommandBus {
    private var nextId: Long = 0L
    private val pending = MutableStateFlow<HomeNavigationEnvelope?>(null)

    val pendingCommand = pending.asStateFlow()

    fun dispatch(command: HomeNavigationCommand) {
        val id = ++nextId
        pending.value = HomeNavigationEnvelope(id = id, command = command)
    }

    fun consume(id: Long) {
        pending.update { current ->
            if (current?.id == id) null else current
        }
    }

    fun clear() {
        pending.value = null
    }
}
