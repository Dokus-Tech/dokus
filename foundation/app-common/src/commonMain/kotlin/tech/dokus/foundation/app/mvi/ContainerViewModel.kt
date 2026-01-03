package tech.dokus.foundation.app.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.koin.compose.currentKoinScope
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.annotation.KoinInternalApi
import org.koin.core.definition.Definition
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.qualifier
import org.koin.core.scope.Scope
import org.koin.viewmodel.defaultExtras
import pro.respawn.flowmvi.api.Container
import pro.respawn.flowmvi.api.FlowMVIDSL
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState
import pro.respawn.flowmvi.api.Store

/**
 * ViewModel wrapper that hosts a FlowMVI Container.
 * This allows using FlowMVI with Koin's viewModel DSL and proper lifecycle management.
 *
 * @param container The FlowMVI Container to wrap
 * @param start Whether to start the store immediately (defaults to true)
 */
open class ContainerViewModel<T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction>(
    val container: T,
    start: Boolean = true,
) : ViewModel(), Store<S, I, A> by container.store, Container<S, I, A> by container {

    init {
        if (start) addCloseable(store.start(viewModelScope))
    }
}

/**
 * Koin DSL for registering FlowMVI Containers as ViewModels.
 *
 * Example:
 * ```kotlin
 * val module = module {
 *     container<MyContainer, MyState, MyIntent, MyAction> { params ->
 *         MyContainer(dependency = get())
 *     }
 * }
 * ```
 */
@FlowMVIDSL
inline fun <reified T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> Module.container(
    crossinline definition: Definition<T>,
) = viewModel(qualifier<T>()) { params ->
    ContainerViewModel<T, S, I, A>(container = definition(params))
}

/**
 * Composable function to obtain a FlowMVI Container from Koin.
 *
 * Example:
 * ```kotlin
 * @Composable
 * fun MyScreen(
 *     container: MyContainer = container { parametersOf(someParam) }
 * ) {
 *     val state by container.store.subscribe { action ->
 *         action.handle()
 *     }
 * }
 * ```
 */
@FlowMVIDSL
@NonRestartableComposable
@Composable
@OptIn(KoinInternalApi::class)
inline fun <reified T : Container<S, I, A>, S : MVIState, I : MVIIntent, A : MVIAction> container(
    key: String? = null,
    scope: Scope = currentKoinScope(),
    viewModelStoreOwner: ViewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    },
    extras: CreationExtras = defaultExtras(viewModelStoreOwner),
    noinline params: ParametersDefinition? = null,
): T = koinViewModel<ContainerViewModel<T, S, I, A>>(
    qualifier = qualifier<T>(),
    parameters = params,
    key = key,
    scope = scope,
    viewModelStoreOwner = viewModelStoreOwner,
    extras = extras
).container
