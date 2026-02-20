package tech.dokus.app.navigation

import kotlin.concurrent.AtomicLong
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class HomeNavigationEnvelope(
    val id: Long,
    val command: HomeNavigationCommand,
)

/**
 * Singleton bus for dispatching navigation commands from root scope into the home NavHost.
 *
 * Uses [MutableStateFlow] so that only the latest unhandled command is retained.
 * Each command is wrapped in an [HomeNavigationEnvelope] with a unique ID to ensure
 * StateFlow conflation doesn't silently drop re-dispatches of the same command type.
 */
internal object HomeNavigationCommandBus {
    private val nextId = AtomicLong(0L)
    private val pending = MutableStateFlow<HomeNavigationEnvelope?>(null)

    val pendingCommand = pending.asStateFlow()

    fun dispatch(command: HomeNavigationCommand) {
        val id = nextId.incrementAndGet()
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
