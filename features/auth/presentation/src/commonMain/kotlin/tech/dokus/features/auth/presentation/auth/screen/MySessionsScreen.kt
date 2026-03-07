package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.TabletMac
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.action_continue
import tech.dokus.aura.resources.profile_current_session
import tech.dokus.aura.resources.profile_revoke_other_sessions
import tech.dokus.aura.resources.profile_revoke_session
import tech.dokus.aura.resources.profile_sessions_description
import tech.dokus.aura.resources.profile_sessions_empty
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.domain.DeviceType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.features.auth.mvi.MySessionsIntent
import tech.dokus.features.auth.mvi.MySessionsState
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.POutlinedButton
import tech.dokus.foundation.aura.components.PPrimaryButton
import tech.dokus.foundation.aura.components.common.DokusLoader
import tech.dokus.foundation.aura.components.common.PTopAppBar
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.local.LocalScreenSize
import tech.dokus.foundation.aura.style.textMuted

private val SessionsListSpacing = 12.dp
private val SessionsCardPadding = 18.dp
private val SessionIconSize = 44.dp

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
        MySessionsContent(
            state = state,
            onIntent = onIntent,
            contentPadding = padding
        )
    }
}

@Composable
internal fun MySessionsContent(
    state: MySessionsState,
    onIntent: (MySessionsIntent) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
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
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .padding(contentPadding),
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
                modifier = modifier,
                contentPadding = contentPadding
            )
        }
    }
}

@Composable
private fun MySessionsList(
    sessions: List<SessionDto>,
    isRevokingOthers: Boolean,
    onRevokeSession: (SessionId) -> Unit,
    onRevokeOthers: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(SessionsListSpacing)
    ) {
        item {
            SessionsSummaryCard(
                sessionCount = sessions.size,
                isRevokingOthers = isRevokingOthers,
                onRevokeOthers = onRevokeOthers
            )
        }

        if (sessions.isEmpty()) {
            item {
                DokusCardSurface(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(Res.string.profile_sessions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted,
                        modifier = Modifier.padding(SessionsCardPadding)
                    )
                }
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
private fun SessionsSummaryCard(
    sessionCount: Int,
    isRevokingOthers: Boolean,
    onRevokeOthers: () -> Unit,
) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        accent = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SessionsCardPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.profile_sessions_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.textMuted
            )
            POutlinedButton(
                text = stringResource(Res.string.profile_revoke_other_sessions),
                onClick = onRevokeOthers,
                enabled = sessionCount > 1,
                isLoading = isRevokingOthers,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SessionCard(
    session: SessionDto,
    onRevoke: () -> Unit
) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        accent = session.isCurrent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SessionsCardPadding),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            SessionIcon(deviceType = session.deviceType)

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.primaryLabel(),
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (session.isCurrent) {
                        CurrentSessionBadge()
                    }
                }

                Text(
                    text = session.secondaryLabel(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )

                session.detailLabel()?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.textMuted
                    )
                }

                if (!session.isCurrent) {
                    POutlinedButton(
                        text = stringResource(Res.string.profile_revoke_session),
                        onClick = onRevoke,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .widthIn(min = 132.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionIcon(deviceType: DeviceType) {
    Surface(
        modifier = Modifier.size(SessionIconSize),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = deviceType.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun CurrentSessionBadge() {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        contentColor = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = stringResource(Res.string.profile_current_session),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun DeviceType.icon(): ImageVector {
    return when (this) {
        DeviceType.Android, DeviceType.Ios -> Icons.Default.PhoneAndroid
        DeviceType.Tablet -> Icons.Default.TabletMac
        DeviceType.Web -> Icons.Default.Language
        DeviceType.Desktop -> Icons.Default.LaptopMac
    }
}

private fun DeviceType.displayLabel(): String {
    return when (this) {
        DeviceType.Android -> "Android"
        DeviceType.Ios -> "iPhone"
        DeviceType.Desktop -> "Desktop"
        DeviceType.Web -> "Web"
        DeviceType.Tablet -> "Tablet"
    }
}

private fun SessionDto.primaryLabel(): String {
    val agent = userAgent?.trim().orEmpty()
    if (agent.isBlank()) return deviceType.displayLabel()
    if (!agent.looksLikeRawUserAgent()) return agent

    val browser = agent.browserLabel()
    val platform = agent.platformLabel(deviceType)
    return when {
        browser != null && platform != null -> "$browser on $platform"
        browser != null -> browser
        platform != null -> platform
        else -> deviceType.displayLabel()
    }
}

private fun SessionDto.secondaryLabel(): String {
    val agent = userAgent?.trim().orEmpty()
    val primary = primaryLabel()
    val details = buildList {
        val platform = agent.platformLabel(deviceType)
        if (!platform.isNullOrBlank() && !primary.contains(platform, ignoreCase = true)) {
            add(platform)
        } else if (!primary.contains(deviceType.displayLabel(), ignoreCase = true)) {
            add(deviceType.displayLabel())
        }
        ipAddress?.takeIf { it.isNotBlank() }?.let(::add)
    }

    return details.joinToString(" • ").ifBlank { deviceType.displayLabel() }
}

private fun SessionDto.detailLabel(): String? {
    val agent = userAgent?.trim().orEmpty()
    if (agent.isBlank()) return null
    if (!agent.looksLikeRawUserAgent()) return null

    val primary = primaryLabel()
    val details = listOfNotNull(
        agent.browserLabel()?.takeIf { !primary.contains(it, ignoreCase = true) },
        agent.platformLabel(deviceType)?.takeIf { !primary.contains(it, ignoreCase = true) }
    )

    return details.takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

private fun String.looksLikeRawUserAgent(): Boolean {
    return contains("mozilla/", ignoreCase = true) ||
        contains("applewebkit", ignoreCase = true) ||
        contains("gecko/", ignoreCase = true)
}

private fun String.browserLabel(): String? {
    val value = lowercase()
    return when {
        "edg/" in value -> "Edge"
        "firefox/" in value -> "Firefox"
        "chrome/" in value || "chromium/" in value -> "Chrome"
        "safari/" in value -> "Safari"
        else -> null
    }
}

private fun String.platformLabel(deviceType: DeviceType): String? {
    val value = lowercase()
    return when {
        "iphone" in value || "ios" in value -> "iPhone"
        "ipad" in value -> "iPad"
        "android" in value -> "Android"
        "mac os" in value || "macintosh" in value -> "macOS"
        "windows" in value -> "Windows"
        "linux" in value -> "Linux"
        deviceType == DeviceType.Web -> "Web"
        else -> null
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
                sessions = previewSessions(),
            ),
            snackbarHostState = androidx.compose.runtime.remember { SnackbarHostState() },
            onIntent = {},
        )
    }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
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
            )
        }
    }
}

@OptIn(kotlin.uuid.ExperimentalUuidApi::class)
internal fun previewSessions(): List<SessionDto> {
    return listOf(
        SessionDto(
            id = SessionId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000001")),
            deviceType = DeviceType.Desktop,
            userAgent = "Chrome on macOS",
            ipAddress = "192.168.1.10",
            isCurrent = true,
        ),
        SessionDto(
            id = SessionId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000002")),
            deviceType = DeviceType.Android,
            userAgent = "Dokus Android App",
            ipAddress = "10.0.0.5",
            isCurrent = false,
        ),
        SessionDto(
            id = SessionId(kotlin.uuid.Uuid.parse("00000000-0000-0000-0000-000000000003")),
            deviceType = DeviceType.Desktop,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 13_6_1) AppleWebKit/537.36 Chrome/121.0 Safari/537.36",
            ipAddress = "203.0.113.42",
            isCurrent = false,
        ),
    )
}
