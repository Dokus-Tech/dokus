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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.Clock
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.mvi.MySessionsState
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.PTopAppBar

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
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                DokusLoader()
            }
        }

        is MySessionsState.Error -> {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(state.exception.message ?: "")
                    Spacer(Modifier.height(12.dp))
                    PPrimaryButton(
                        text = stringResource(Res.string.action_continue),
                        onClick = { onIntent(MySessionsIntent.Load) }
                    )
                }
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

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MySessionsScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        MySessionsScreen(
            state = MySessionsState.Loaded(sessions = previewSessions()),
            snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() },
            onIntent = {},
            nowEpochSeconds = SessionsPreviewNowEpochSeconds,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview(name = "Sessions Desktop", widthDp = 1200, heightDp = 760)
@Composable
private fun MySessionsContentDesktopPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            MySessionsContent(
                state = MySessionsState.Loaded(sessions = previewSessions()),
                onIntent = {},
                nowEpochSeconds = SessionsPreviewNowEpochSeconds,
            )
        }
    }
}
