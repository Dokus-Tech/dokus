package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import tech.dokus.foundation.aura.components.common.DokusLoader
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.profile_current_session
import tech.dokus.aura.resources.profile_revoke_other_sessions
import tech.dokus.aura.resources.profile_revoke_session
import tech.dokus.aura.resources.profile_sessions_empty
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.mvi.MySessionsState
import tech.dokus.foundation.aura.components.DokusCard
import tech.dokus.foundation.aura.components.DokusCardPadding
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable

@Composable
internal fun MySessionsScreen(
    state: MySessionsState,
    snackbarHostState: SnackbarHostState,
    onIntent: (MySessionsIntent) -> Unit
) {
    Scaffold(
        topBar = { PTopAppBar(Res.string.profile_sessions_title) },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        when (state) {
            MySessionsState.Loading -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    DokusLoader()
                }
            }

            is MySessionsState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(state.exception.message ?: "")
                    Spacer(Modifier.height(12.dp))
                    PPrimaryButton(
                        text = stringResource(Res.string.action_continue),
                        onClick = { onIntent(MySessionsIntent.Load) }
                    )
                }
            }

            is MySessionsState.Loaded -> {
                MySessionsList(
                    sessions = state.sessions,
                    isRevokingOthers = state.isRevokingOthers,
                    onRevokeSession = { onIntent(MySessionsIntent.RevokeSession(it)) },
                    onRevokeOthers = { onIntent(MySessionsIntent.RevokeOthers) },
                    contentPadding = padding
                )
            }
        }
    }
}

@Composable
private fun MySessionsList(
    sessions: List<SessionDto>,
    isRevokingOthers: Boolean,
    onRevokeSession: (SessionId) -> Unit,
    onRevokeOthers: () -> Unit,
    contentPadding: PaddingValues
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            PPrimaryButton(
                text = stringResource(Res.string.profile_revoke_other_sessions),
                onClick = onRevokeOthers,
                isLoading = isRevokingOthers,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (sessions.isEmpty()) {
            item {
                Text(
                    text = stringResource(Res.string.profile_sessions_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(sessions, key = { it.id.toString() }) { session ->
                SessionCard(
                    session = session,
                    onRevoke = { onRevokeSession(session.id) }
                )
            }
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionDto,
    onRevoke: () -> Unit
) {
    DokusCard(
        modifier = Modifier.fillMaxWidth(),
        padding = DokusCardPadding.Default
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = session.userAgent ?: session.deviceType.name,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = session.ipAddress ?: "-",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (session.isCurrent) {
                Text(
                    text = stringResource(Res.string.profile_current_session),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                POutlinedButton(
                    text = stringResource(Res.string.profile_revoke_session),
                    onClick = onRevoke,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun MySessionsScreenPreview(
    @androidx.compose.ui.tooling.preview.PreviewParameter(
        tech.dokus.foundation.aura.tooling.PreviewParametersProvider::class
    ) parameters: tech.dokus.foundation.aura.tooling.PreviewParameters
) {
    tech.dokus.foundation.aura.tooling.TestWrapper(parameters) {
        MySessionsScreen(
            state = MySessionsState.Loaded(
                sessions = listOf(
                    SessionDto(
                        id = SessionId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000001")),
                        deviceType = tech.dokus.domain.DeviceType.Desktop,
                        userAgent = "Chrome on macOS",
                        ipAddress = "192.168.1.1",
                        isCurrent = true,
                    ),
                    SessionDto(
                        id = SessionId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000002")),
                        deviceType = tech.dokus.domain.DeviceType.Android,
                        userAgent = "Dokus Android App",
                        ipAddress = "10.0.0.5",
                        isCurrent = false,
                    ),
                ),
            ),
            snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}
