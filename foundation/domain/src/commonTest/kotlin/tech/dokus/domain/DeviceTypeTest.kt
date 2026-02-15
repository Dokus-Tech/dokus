package tech.dokus.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceTypeTest {

    @Test
    fun `fromAgent returns Android for Android mobile user agent`() {
        val userAgent =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

        val deviceType = DeviceType.fromAgent(userAgent)

        assertEquals(DeviceType.Android, deviceType)
    }

    @Test
    fun `fromAgent returns Ios for iPhone mobile user agent`() {
        val userAgent =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

        val deviceType = DeviceType.fromAgent(userAgent)

        assertEquals(DeviceType.Ios, deviceType)
    }

    @Test
    fun `fromAgent returns Tablet for iPad user agent`() {
        val userAgent =
            "Mozilla/5.0 (iPad; CPU OS 17_0 like Mac OS X) AppleWebKit/605.1.15 " +
                "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1"

        val deviceType = DeviceType.fromAgent(userAgent)

        assertEquals(DeviceType.Tablet, deviceType)
    }

    @Test
    fun `fromAgent returns Tablet for generic tablet user agent`() {
        val userAgent =
            "Mozilla/5.0 (Linux; Android 13; SM-T865 Build/TP1A.220624.014; Tablet) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"

        val deviceType = DeviceType.fromAgent(userAgent)

        assertEquals(DeviceType.Tablet, deviceType)
    }

    @Test
    fun `fromAgent returns Desktop for desktop browser user agent`() {
        val userAgent =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_3) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"

        val deviceType = DeviceType.fromAgent(userAgent)

        assertEquals(DeviceType.Desktop, deviceType)
    }

    @Test
    fun `fromAgent returns Desktop when user agent is null`() {
        val deviceType = DeviceType.fromAgent(null)

        assertEquals(DeviceType.Desktop, deviceType)
    }

    @Test
    fun `fromAgent returns Desktop when user agent is empty`() {
        val deviceType = DeviceType.fromAgent("")

        assertEquals(DeviceType.Desktop, deviceType)
    }

    @Test
    fun `fromAgent is case insensitive`() {
        val userAgent = "MOZILLA/5.0 (IPHONE) MOBILE"

        val deviceType = DeviceType.fromAgent(userAgent)

        assertEquals(DeviceType.Ios, deviceType)
    }
}
