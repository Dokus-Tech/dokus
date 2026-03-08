package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.profile_current_session
import tech.dokus.aura.resources.profile_revoke_other_sessions
import tech.dokus.aura.resources.profile_revoke_session
import tech.dokus.aura.resources.profile_sessions_description
import tech.dokus.aura.resources.profile_sessions_empty
import tech.dokus.aura.resources.profile_sessions_this_device
import tech.dokus.aura.resources.profile_sessions_title
import tech.dokus.domain.DeviceType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.foundation.aura.components.DokusCardSurface
import tech.dokus.foundation.aura.components.DokusCardVariant
import tech.dokus.foundation.aura.constrains.Constraints
import tech.dokus.foundation.aura.constrains.withContentPaddingForScrollable
import tech.dokus.foundation.aura.extensions.iconized
import tech.dokus.foundation.aura.style.textMuted

private val SessionsListSpacing = 18.dp
private val SessionRowPaddingH = 18.dp
private val SessionRowPaddingV = Constraints.Spacing.large
private val SessionIconSize = 44.dp
private val SessionHeroIconSize = 56.dp
private val SessionIconContentSize = 22.dp
private val SessionHeroIconContentSize = 28.dp

@Composable
internal fun MySessionsLoadedContent(
    sessions: List<SessionDto>,
    isRevokingOthers: Boolean,
    nowEpochSeconds: Long,
    onRevokeSession: (SessionId) -> Unit,
    onRevokeOthers: () -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val sections = sessions.toSessionSections()

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(contentPadding)
            .withContentPaddingForScrollable(),
        verticalArrangement = Arrangement.spacedBy(SessionsListSpacing)
    ) {
        item(key = "overview") {
            SessionsOverviewCard()
        }

        sections.currentSession?.let { currentSession ->
            item(key = "current_session") {
                Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)) {
                    SessionSectionTitle(text = stringResource(Res.string.profile_sessions_this_device))
                    CurrentDeviceCard(
                        session = currentSession,
                        hasOtherSessions = sections.otherSessions.isNotEmpty(),
                        isRevokingOthers = isRevokingOthers,
                        nowEpochSeconds = nowEpochSeconds,
                        onRevokeOthers = onRevokeOthers,
                    )
                }
            }
        }

        item(key = "other_sessions_group") {
            AnimatedVisibility(
                visible = sections.otherSessions.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(180)) + expandVertically(animationSpec = tween(220)),
                exit = fadeOut(animationSpec = tween(140)) + shrinkVertically(animationSpec = tween(180)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.medium)) {
                    SessionSectionTitle(text = stringResource(Res.string.profile_sessions_title))
                    OtherSessionsGroup(
                        sessions = sections.otherSessions,
                        nowEpochSeconds = nowEpochSeconds,
                        onRevokeSession = onRevokeSession,
                    )
                }
            }
        }

        if (sections.currentSession == null && sections.otherSessions.isEmpty()) {
            item(key = "empty_state") {
                DokusCardSurface(
                    modifier = Modifier.fillMaxWidth(),
                    variant = DokusCardVariant.Soft,
                ) {
                    Text(
                        text = stringResource(Res.string.profile_sessions_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted,
                        modifier = Modifier.padding(SessionRowPaddingH)
                    )
                }
            }
        }
    }
}

@Composable
private fun SessionsOverviewCard(
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    DokusCardSurface(
        modifier = modifier.fillMaxWidth(),
        variant = DokusCardVariant.Soft,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.large)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SessionIcon(
                    deviceType = DeviceType.Desktop,
                    containerSize = SessionHeroIconSize,
                    iconSize = SessionHeroIconContentSize,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.profile_sessions_title),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = stringResource(Res.string.profile_sessions_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.textMuted,
                    )
                }
            }
            action?.invoke()
        }
    }
}

@Composable
private fun SessionSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.textMuted
    )
}

@Composable
private fun CurrentDeviceCard(
    session: SessionDto,
    hasOtherSessions: Boolean,
    isRevokingOthers: Boolean,
    nowEpochSeconds: Long,
    onRevokeOthers: () -> Unit,
) {
    DokusCardSurface(
        modifier = Modifier.fillMaxWidth(),
        accent = true,
    ) {
        Column(modifier = Modifier.animateContentSize()) {
            SessionRow(
                title = session.userFacingPrimaryLabel(),
                secondary = session.userFacingClientLabel(),
                context = session.userFacingContextLabel(nowEpochSeconds = nowEpochSeconds),
                deviceType = session.deviceType,
                badgeLabel = stringResource(Res.string.profile_current_session),
                onAction = null,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SessionDestructiveActionRow(
                enabled = hasOtherSessions,
                isLoading = isRevokingOthers,
                onClick = onRevokeOthers,
            )
        }
    }
}

@Composable
private fun OtherSessionsGroup(
    sessions: List<SessionDto>,
    nowEpochSeconds: Long,
    onRevokeSession: (SessionId) -> Unit,
) {
    AnimatedContent(
        targetState = sessions,
        transitionSpec = {
            (
                fadeIn(animationSpec = tween(180)) +
                    slideInVertically(animationSpec = tween(180), initialOffsetY = { it / 8 })
                ) togetherWith (
                fadeOut(animationSpec = tween(140)) +
                    slideOutVertically(animationSpec = tween(140), targetOffsetY = { -it / 8 })
                )
        },
        label = "OtherSessionsGroup"
    ) { visibleSessions ->
        DokusCardSurface(
            modifier = Modifier.fillMaxWidth(),
            variant = DokusCardVariant.Soft,
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                visibleSessions.forEachIndexed { index, session ->
                    SessionRow(
                        title = session.userFacingPrimaryLabel(),
                        secondary = session.userFacingClientLabel(),
                        context = session.userFacingContextLabel(nowEpochSeconds = nowEpochSeconds),
                        deviceType = session.deviceType,
                        badgeLabel = null,
                        onAction = { onRevokeSession(session.id) },
                    )
                    if (index < visibleSessions.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    title: String,
    secondary: String?,
    context: String?,
    deviceType: DeviceType,
    badgeLabel: String?,
    onAction: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SessionRowPaddingH, vertical = SessionRowPaddingV),
        horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.large),
        verticalAlignment = Alignment.Top
    ) {
        SessionIcon(deviceType = deviceType)

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Constraints.Spacing.xSmall)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constraints.Spacing.small),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                badgeLabel?.let {
                    SessionBadge(label = it)
                }
            }

            secondary?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            context?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.textMuted
                )
            }
        }

        onAction?.let { action ->
            TextButton(onClick = action) {
                Text(text = stringResource(Res.string.profile_revoke_session))
            }
        }
    }
}

@Composable
private fun SessionBadge(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun SessionDestructiveActionRow(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.01f),
        onClick = onClick,
        enabled = enabled && !isLoading,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SessionRowPaddingH, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(Res.string.profile_revoke_other_sessions),
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.textMuted
            )
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(Constraints.IconSize.small),
                    strokeWidth = Constraints.Stroke.thin,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SessionIcon(
    deviceType: DeviceType,
    containerSize: Dp = SessionIconSize,
    iconSize: Dp = SessionIconContentSize,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.size(containerSize),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
        shape = MaterialTheme.shapes.medium
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = deviceType.iconized,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
