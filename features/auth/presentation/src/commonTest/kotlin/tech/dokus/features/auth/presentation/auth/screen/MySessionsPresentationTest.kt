package tech.dokus.features.auth.presentation.auth.screen

import tech.dokus.domain.DeviceType
import tech.dokus.domain.ids.SessionId
import tech.dokus.domain.model.auth.IpLocationInfo
import tech.dokus.domain.model.auth.SessionDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MySessionsPresentationTest {

    @Test
    fun `raw desktop user agent is hidden from user facing labels`() {
        val session = session(
            deviceType = DeviceType.Desktop,
            userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_6_1) AppleWebKit/537.36 Chrome/127.0.0.0 Safari/537.36"
        )

        assertEquals("Desktop", session.userFacingPrimaryLabel())
        assertNull(session.userFacingClientLabel())
    }

    @Test
    fun `web sessions do not expose browser labels`() {
        val session = session(
            deviceType = DeviceType.Web,
            userAgent = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 Chrome/127.0.0.0 Safari/537.36"
        )

        assertEquals("Web session", session.userFacingPrimaryLabel())
        assertNull(session.userFacingClientLabel())
    }

    @Test
    fun `telegram agent tokens are never exposed as the session label`() {
        val session = session(
            deviceType = DeviceType.Desktop,
            userAgent = "TelegramDesktop/5.11 CFNetwork/1568.200.51 Darwin/24.1.0"
        )

        assertEquals("Desktop", session.userFacingPrimaryLabel())
        assertNull(session.userFacingClientLabel())
    }

    @Test
    fun `context label prefers location and relative activity`() {
        val session = session(
            location = IpLocationInfo(city = "Oostkamp", country = "Belgium"),
            lastActivityAt = SessionsPreviewNowEpochSeconds - 172_800L,
        )

        assertEquals(
            "Oostkamp, Belgium • 2d ago",
            session.userFacingContextLabel(nowEpochSeconds = SessionsPreviewNowEpochSeconds)
        )
    }

    @Test
    fun `context label falls back to ip when location is missing`() {
        val session = session(
            ipAddress = "203.0.113.42",
            lastActivityAt = SessionsPreviewNowEpochSeconds - 7_200L,
        )

        assertEquals(
            "203.0.113.42 • 2h ago",
            session.userFacingContextLabel(nowEpochSeconds = SessionsPreviewNowEpochSeconds)
        )
    }

    @Test
    fun `sections keep current session separate from other sessions`() {
        val current = session(id = "00000000-0000-0000-0000-000000000010", isCurrent = true)
        val other = session(id = "00000000-0000-0000-0000-000000000011")

        val sections = listOf(other, current).toSessionSections()

        assertEquals(current.id, sections.currentSession?.id)
        assertEquals(listOf(other.id), sections.otherSessions.map(SessionDto::id))
    }
}

private fun session(
    id: String = "00000000-0000-0000-0000-000000000001",
    deviceType: DeviceType = DeviceType.Desktop,
    userAgent: String = "Chrome on macOS",
    ipAddress: String? = "192.168.1.10",
    location: IpLocationInfo? = null,
    lastActivityAt: Long = SessionsPreviewNowEpochSeconds - 3_600L,
    isCurrent: Boolean = false,
): SessionDto {
    return SessionDto(
        id = SessionId(id),
        deviceType = deviceType,
        userAgent = userAgent,
        ipAddress = ipAddress,
        location = location,
        createdAt = SessionsPreviewNowEpochSeconds - 86_400L,
        lastActivityAt = lastActivityAt,
        isCurrent = isCurrent,
    )
}
