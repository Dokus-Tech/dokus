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
    fun `location label formats city and country`() {
        val session = session(
            location = IpLocationInfo(city = "Oostkamp", country = "Belgium"),
        )

        assertEquals("Oostkamp, Belgium", session.userFacingLocationLabel())
    }

    @Test
    fun `location label falls back to ip when location is missing`() {
        val session = session(ipAddress = "203.0.113.42")

        assertEquals("203.0.113.42", session.userFacingLocationLabel())
    }

    @Test
    fun `location label returns null when no location or ip`() {
        val session = session(ipAddress = null, location = null)

        assertNull(session.userFacingLocationLabel())
    }

    @Test
    fun `location label formats city and region without country`() {
        val session = session(
            location = IpLocationInfo(city = "Bruges", region = "West Flanders"),
        )

        assertEquals("Bruges, West Flanders", session.userFacingLocationLabel())
    }

    @Test
    fun `sections keep current session separate from other sessions`() {
        val current = session(id = "00000000-0000-0000-0000-000000000010", isCurrent = true)
        val other = session(id = "00000000-0000-0000-0000-000000000011")

        val sections = listOf(other, current).toSessionSections()

        assertEquals(current.id, sections.currentSession?.id)
        assertEquals(listOf(other.id), sections.otherSessions.map(SessionDto::id))
    }

    @Test
    fun `sections return null current session when none marked current`() {
        val session1 = session(id = "00000000-0000-0000-0000-000000000001")
        val session2 = session(id = "00000000-0000-0000-0000-000000000002")

        val sections = listOf(session1, session2).toSessionSections()

        assertNull(sections.currentSession)
        assertEquals(2, sections.otherSessions.size)
    }

    @Test
    fun `client label returns null`() {
        val session = session()
        assertNull(session.userFacingClientLabel())
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
