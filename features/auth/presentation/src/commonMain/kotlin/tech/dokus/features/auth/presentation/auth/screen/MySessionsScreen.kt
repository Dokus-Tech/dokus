package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.mvi.MySessionsState
import tech.dokus.features.auth.presentation.auth.components.MySessionsSkeleton
import tech.dokus.foundation.app.state.DokusState
import tech.dokus.foundation.app.state.isError
import tech.dokus.foundation.app.state.isLoading
import tech.dokus.foundation.app.state.isSuccess
import tech.dokus.foundation.aura.components.common.DokusErrorContent
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.time.Clock

private const val PaneAnimationDurationMs = 220

@Composable
internal fun MySessionsScreen(
    state: MySessionsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (MySessionsIntent) -> Unit,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
) {
    Scaffold(
        topBar = { PTopAppBar(Res.string.profile_sessions_title) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        MySessionsContent(
            state = state,
            onIntent = onIntent,
            contentPadding = padding,
            nowEpochSeconds = nowEpochSeconds,
        )
    }
}

@Composable
internal fun MySessionsContent(
    state: MySessionsState,
    onIntent: (MySessionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
) {
    when {
        state.sessions.isLoading() -> {
            MySessionsSkeleton(
                modifier = modifier.padding(contentPadding),
            )
        }

        state.sessions.isError() -> {
            DokusErrorContent(
                exception = state.sessions.exception,
                retryHandler = state.sessions.retryHandler,
                modifier = Modifier.fillMaxSize().padding(contentPadding),
            )
        }

        state.sessions.isSuccess() -> {
            val sessions = (state.sessions as DokusState.Success).data
            AnimatedVisibility(
                visible = true,
                modifier = modifier.fillMaxSize(),
                enter = fadeIn(animationSpec = tween(PaneAnimationDurationMs)) +
                        slideInVertically(
                            animationSpec = tween(PaneAnimationDurationMs),
                            initialOffsetY = { it / 10 }
                        ),
                exit = fadeOut(animationSpec = tween(PaneAnimationDurationMs / 2))
            ) {
                MySessionsLoadedContent(
                    sessions = sessions,
                    isRevokingOthers = state.isRevokingOthers,
                    nowEpochSeconds = nowEpochSeconds,
                    onRevokeSession = { onIntent(MySessionsIntent.RevokeSession(it)) },
                    onRevokeOthers = { onIntent(MySessionsIntent.RevokeOthers) },
                    contentPadding = contentPadding,
                )
            }
        }
    }
}

@Preview
@Composable
private fun MySessionsScreenPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        MySessionsScreen(
            state = MySessionsState(sessions = DokusState.success(previewSessions())),
            snackbarHostState = remember { SnackbarHostState() },
            onIntent = {},
            nowEpochSeconds = SessionsPreviewNowEpochSeconds,
        )
    }
}

@Preview
@Composable
private fun MySessionsLoadingPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MySessionsContent(
            state = MySessionsState.initial,
            onIntent = {},
        )
    }
}

@Preview
@Composable
private fun MySessionsErrorPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters,
) {
    TestWrapper(parameters) {
        MySessionsContent(
            state = MySessionsState(
                sessions = DokusState.error(
                    exception = tech.dokus.domain.exceptions.DokusException.ConnectionError(),
                    retryHandler = tech.dokus.domain.asbtractions.RetryHandler { },
                ),
            ),
            onIntent = {},
        )
    }
}

@Preview(name = "Sessions Desktop", widthDp = 1200, heightDp = 760)
@Composable
private fun MySessionsContentDesktopPreview(
    @PreviewParameter(PreviewParametersProvider::class) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            MySessionsContent(
                state = MySessionsState(sessions = DokusState.success(previewSessions())),
                onIntent = {},
                nowEpochSeconds = SessionsPreviewNowEpochSeconds,
            )
        }
    }
}
