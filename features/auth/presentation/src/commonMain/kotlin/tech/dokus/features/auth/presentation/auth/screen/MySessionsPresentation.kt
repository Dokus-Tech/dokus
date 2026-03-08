package tech.dokus.features.auth.presentation.auth.screen

import androidx.compose.runtime.Composable
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import tech.dokus.aura.resources.Res
import tech.dokus.aura.resources.session_device_ipad
import tech.dokus.aura.resources.session_device_iphone
import tech.dokus.aura.resources.session_device_web
import tech.dokus.aura.resources.session_last_seen_days_ago
import tech.dokus.aura.resources.session_last_seen_hours_ago
import tech.dokus.aura.resources.session_last_seen_just_now
import tech.dokus.aura.resources.session_last_seen_minutes_ago
import tech.dokus.aura.resources.session_last_seen_online
import tech.dokus.domain.DeviceType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.IpLocationInfo
import tech.dokus.domain.model.auth.SessionDto
import tech.dokus.foundation.aura.extensions.localized

internal const val SessionsPreviewNowEpochSeconds: Long = 1_741_392_000L

internal data class SessionSections(
    val currentSession: SessionDto?,
    val otherSessions: List<SessionDto>,
)

internal fun List<SessionDto>.toSessionSections(): SessionSections {
    val currentSession = firstOrNull { it.isCurrent }
    val currentSessionId = currentSession?.id
    return SessionSections(
        currentSession = currentSession,
        otherSessions = filterNot { it.id == currentSessionId }
    )
}

@Composable
internal fun SessionDto.userFacingPrimaryLabel(): String {
    return when (deviceType) {
        DeviceType.Web -> stringResource(Res.string.session_device_web)
        DeviceType.Ios -> if (userAgent.orEmpty().contains("ipad", ignoreCase = true)) {
            stringResource(Res.string.session_device_ipad)
        } else {
            stringResource(Res.string.session_device_iphone)
        }
        DeviceType.Android -> if (userAgent.orEmpty().contains("tablet", ignoreCase = true)) {
            DeviceType.Tablet.localized
        } else {
            DeviceType.Android.localized
        }
        DeviceType.Tablet -> DeviceType.Tablet.localized
        DeviceType.Desktop -> DeviceType.Desktop.localized
    }
}

internal fun SessionDto.userFacingClientLabel(): String? {
    return null
}

@Composable
internal fun SessionDto.userFacingContextLabel(
    nowEpochSeconds: Long,
): String? {
    return listOfNotNull(
        userFacingLocationLabel(),
        userFacingLastSeenLabel(nowEpochSeconds = nowEpochSeconds)
    ).takeIf { it.isNotEmpty() }?.joinToString(" • ")
}

internal fun SessionDto.userFacingLocationLabel(): String? {
    val info = location
    val city = info?.city
    val country = info?.country
    val region = info?.region
    return when {
        !city.isNullOrBlank() && !country.isNullOrBlank() -> "$city, $country"
        !city.isNullOrBlank() && !region.isNullOrBlank() -> "$city, $region"
        !region.isNullOrBlank() && !country.isNullOrBlank() -> "$region, $country"
        !country.isNullOrBlank() -> country
        !ipAddress.isNullOrBlank() -> ipAddress
        else -> null
    }
}

@Composable
internal fun SessionDto.userFacingLastSeenLabel(
    nowEpochSeconds: Long,
): String? {
    if (isCurrent) return stringResource(Res.string.session_last_seen_online)

    val eventEpochSeconds = lastActivityAt ?: createdAt ?: return null
    val diffSeconds = (nowEpochSeconds - eventEpochSeconds).coerceAtLeast(0L)

    return when {
        diffSeconds < 60L -> stringResource(Res.string.session_last_seen_just_now)
        diffSeconds < 3_600L -> stringResource(Res.string.session_last_seen_minutes_ago, diffSeconds / 60L)
        diffSeconds < 86_400L -> stringResource(Res.string.session_last_seen_hours_ago, diffSeconds / 3_600L)
        diffSeconds < 604_800L -> stringResource(Res.string.session_last_seen_days_ago, diffSeconds / 86_400L)
        else -> Instant
            .fromEpochSeconds(eventEpochSeconds)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .format(SessionDateFormat)
    }
}

internal fun previewSessions(): List<SessionDto> {
    return listOf(
        SessionDto(
            id = SessionId("00000000-0000-0000-0000-000000000001"),
            deviceType = DeviceType.Desktop,
            userAgent = "ktor-client/3.0 CFNetwork/1568.200.51 Darwin/24.1.0",
            ipAddress = "192.168.117.1",
            location = IpLocationInfo(city = "Oostkamp", country = "Belgium"),
            createdAt = SessionsPreviewNowEpochSeconds - 604_800L,
            lastActivityAt = SessionsPreviewNowEpochSeconds - 30L,
            isCurrent = true,
        ),
        SessionDto(
            id = SessionId("00000000-0000-0000-0000-000000000002"),
            deviceType = DeviceType.Ios,
            userAgent = "Mozilla/5.0 (iPad; CPU OS 17_6 like Mac OS X) AppleWebKit/605.1.15 Version/17.6 Mobile/15E148 Safari/604.1",
            location = IpLocationInfo(city = "Oostkamp", country = "Belgium"),
            createdAt = SessionsPreviewNowEpochSeconds - 432_000L,
            lastActivityAt = SessionsPreviewNowEpochSeconds - 172_800L,
        ),
        SessionDto(
            id = SessionId("00000000-0000-0000-0000-000000000003"),
            deviceType = DeviceType.Desktop,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6_1) AppleWebKit/537.36 Chrome/127.0.0.0 Safari/537.36",
            ipAddress = "203.0.113.42",
            createdAt = SessionsPreviewNowEpochSeconds - 1_296_000L,
            lastActivityAt = SessionsPreviewNowEpochSeconds - 864_000L,
        ),
    )
}

private val SessionDateFormat = LocalDateTime.Format {
    monthName(MonthNames.ENGLISH_ABBREVIATED)
    char(' ')
    day(padding = Padding.ZERO)
}
