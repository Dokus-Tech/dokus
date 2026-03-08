package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.mvi.MySessionsState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import tech.dokus.foundation.aura.tooling.PreviewParameters
import tech.dokus.foundation.aura.tooling.PreviewParametersProvider
import tech.dokus.foundation.aura.tooling.TestWrapper
import kotlin.time.Clock

private const val PaneAnimationDurationMs = 220

/**
 * Represents which detail pane is shown in the profile settings split view.
 * Extend this sealed interface when new detail panes are added (e.g., ServerConfig).
 */
internal sealed interface ProfileDetailSelection {
    data object None : ProfileDetailSelection
    data object Sessions : ProfileDetailSelection
}

/**
 * Animated host that switches between detail pane content based on [selection].
 */
@Composable
internal fun ProfileDetailPaneHost(
    selection: ProfileDetailSelection,
    sessionsState: MySessionsState,
    onSessionsIntent: (MySessionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
) {
    AnimatedContent(
        targetState = selection,
        modifier = modifier.fillMaxSize(),
        transitionSpec = {
            val isEntering = targetState !is ProfileDetailSelection.None
            if (isEntering) {
                (
                    fadeIn(animationSpec = tween(PaneAnimationDurationMs)) +
                        slideInHorizontally(
                            animationSpec = tween(PaneAnimationDurationMs),
                            initialOffsetX = { it / 12 }
                        )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(PaneAnimationDurationMs / 2)) +
                        slideOutHorizontally(
                            animationSpec = tween(PaneAnimationDurationMs / 2),
                            targetOffsetX = { -it / 16 }
                        )
                    )
            } else {
                (
                    fadeIn(animationSpec = tween(PaneAnimationDurationMs)) +
                        slideInHorizontally(
                            animationSpec = tween(PaneAnimationDurationMs),
                            initialOffsetX = { -it / 16 }
                        )
                    ) togetherWith (
                    fadeOut(animationSpec = tween(PaneAnimationDurationMs / 2)) +
                        slideOutHorizontally(
                            animationSpec = tween(PaneAnimationDurationMs / 2),
                            targetOffsetX = { it / 12 }
                        )
                    )
            }
        },
        label = "ProfileDetailPaneHost"
    ) { currentSelection ->
        when (currentSelection) {
            ProfileDetailSelection.None -> DetailPaneIdlePlaceholder()
            ProfileDetailSelection.Sessions -> MySessionsContent(
                state = sessionsState,
                onIntent = onSessionsIntent,
                nowEpochSeconds = nowEpochSeconds,
            )
        }
    }
}

@Composable
internal fun DetailPaneIdlePlaceholder(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        IdlePlaceholderBackdrop(modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun IdlePlaceholderBackdrop(
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "IdlePlaceholderBackdrop")
    val slowOffset by transition.animateFloat(
        initialValue = -28f,
        targetValue = 28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "SlowOffset"
    )
    val mediumOffset by transition.animateFloat(
        initialValue = 22f,
        targetValue = -18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "MediumOffset"
    )
    val alphaPulse by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.32f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "AlphaPulse"
    )

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 48.dp, top = 64.dp)
                .size(180.dp)
                .graphicsLayer {
                    translationX = slowOffset
                    translationY = mediumOffset
                    alpha = alphaPulse
                },
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            shape = CircleShape,
        ) {}

        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 44.dp)
                .size(140.dp)
                .graphicsLayer {
                    translationX = -mediumOffset
                    translationY = slowOffset * 0.5f
                    alpha = alphaPulse * 0.9f
                },
            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.07f),
            shape = CircleShape,
        ) {}

        Surface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 88.dp, bottom = 72.dp)
                .size(110.dp)
                .graphicsLayer {
                    translationX = mediumOffset * 0.7f
                    translationY = -slowOffset * 0.6f
                    alpha = alphaPulse * 0.8f
                },
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
            shape = CircleShape,
        ) {}
    }
}

@Preview(name = "Detail Pane Idle", widthDp = 520, heightDp = 760)
@Composable
private fun ProfileDetailPaneIdlePreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileDetailPaneHost(
            selection = ProfileDetailSelection.None,
            sessionsState = MySessionsState.Loading,
            onSessionsIntent = {},
        )
    }
}

@Preview(name = "Detail Pane Sessions", widthDp = 520, heightDp = 760)
@Composable
private fun ProfileDetailPaneSessionsPreview(
    @PreviewParameter(
        PreviewParametersProvider::class
    ) parameters: PreviewParameters
) {
    TestWrapper(parameters) {
        ProfileDetailPaneHost(
            selection = ProfileDetailSelection.Sessions,
            sessionsState = MySessionsState.Loaded(sessions = previewSessions()),
            onSessionsIntent = {},
            nowEpochSeconds = SessionsPreviewNowEpochSeconds,
        )
    }
}
