package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.domain.asbtractions.RetryHandler
import tech.dokus.domain.exceptions.DokusException
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.mvi.MySessionsState
import tech.dokus.features.auth.presentation.auth.components.MySessionsSkeleton
import tech.dokus.foundation.aura.components.common.DokusErrorBanner
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
    when (state) {
        MySessionsState.Loading -> {
            MySessionsSkeleton(
                modifier = modifier.padding(contentPadding),
            )
        }

        is MySessionsState.Error -> {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            ) {
                DokusErrorBanner(
                    exception = state.exception,
                    retryHandler = state.retryHandler,
                    modifier = Modifier.padding(16.dp),
                )
                MySessionsSkeleton()
            }
        }

        is MySessionsState.Loaded -> {
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
                    sessions = state.sessions,
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
            state = MySessionsState.Loaded(sessions = previewSessions()),
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
            state = MySessionsState.Loading,
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
            state = MySessionsState.Error(
                exception = DokusException.ConnectionError(),
                retryHandler = RetryHandler { },
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
                state = MySessionsState.Loaded(sessions = previewSessions()),
                onIntent = {},
                nowEpochSeconds = SessionsPreviewNowEpochSeconds,
            )
        }
    }
}
