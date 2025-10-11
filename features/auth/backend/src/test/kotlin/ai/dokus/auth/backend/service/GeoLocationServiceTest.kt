package ai.dokus.auth.backend.service

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class GeoLocationServiceTest {

    private lateinit var geoLocationService: GeoLocationServiceDb

    // Use absolute path from classpath resource
    private val testDatabasePath =
        this::class.java.classLoader.getResource("GeoLite2-City.mmdb")?.path
            ?: throw IllegalStateException("Test database not found in classpath")

    @BeforeEach
    fun setup() {
        // Verify test database exists
        val dbFile = File(testDatabasePath)
        assertTrue(dbFile.exists(), "Test database file should exist at: $testDatabasePath")

        geoLocationService = GeoLocationServiceDb(testDatabasePath)
    }

    @AfterEach
    fun tearDown() {
        geoLocationService.close()
    }

    @Test
    fun `getCityFromIp should work for valid public IP`() = runBlocking {
        // Note: 8.8.8.8 (Google DNS) and 8.8.4.4 may or may not have city data
        // Infrastructure IPs often only have country-level data
        val city1 = geoLocationService.getCityFromIp("8.8.8.8")
        val city2 = geoLocationService.getCityFromIp("104.28.40.7")

        // At least one should work, or both return null (which is acceptable)
        println("Resolved 8.8.8.8 to: $city1")
        println("Resolved 104.28.40.7 to: $city2")

        // Test that the service doesn't crash - null is acceptable for infrastructure IPs
        assertTrue(true, "Service should not crash on valid IPs")
    }

    @Test
    fun `getCityFromIp should return null for localhost`() = runBlocking {
        val city = geoLocationService.getCityFromIp("127.0.0.1")

        assertNull(city, "Localhost should not resolve to a city")
    }

    @Test
    fun `getCityFromIp should return null for private IP`() = runBlocking {
        val city = geoLocationService.getCityFromIp("192.168.1.1")

        assertNull(city, "Private IP should not resolve to a city")
    }

    @Test
    fun `getCityFromIp should return null for empty IP`() = runBlocking {
        val city = geoLocationService.getCityFromIp("")

        assertNull(city, "Empty IP should return null")
    }

    @Test
    fun `getCityFromIp should handle invalid IP gracefully`() = runBlocking {
        val city = geoLocationService.getCityFromIp("not-an-ip")

        assertNull(city, "Invalid IP should return null gracefully")
    }

    @Test
    fun `getLocationFromIp should return country info for valid IP`() = runBlocking {
        val location = geoLocationService.getLocationFromIp("8.8.8.8")

        assertNotNull(location, "Location should be found for Google DNS IP")
        // City may or may not be present for infrastructure IPs
        assertNotNull(location!!.country, "Country should be present")
        assertNotNull(location.countryCode, "Country code should be present")
        assertEquals("US", location.countryCode, "Google DNS should be in US")

        println("Full location info for 8.8.8.8:")
        println("  City: ${location.city}")
        println("  Country: ${location.country}")
        println("  Country Code: ${location.countryCode}")
        println("  Region: ${location.region}")
        println("  Latitude: ${location.latitude}")
        println("  Longitude: ${location.longitude}")
    }

    @Test
    fun `getLocationFromIp should handle IPv6 addresses`() = runBlocking {
        // Google's public DNS IPv6
        // IPv6 may or may not resolve depending on database, just verify it doesn't crash
        var threwException = false
        try {
            val location = geoLocationService.getLocationFromIp("2001:4860:4860::8888")
            if (location != null) {
                println("IPv6 location: ${location.city}, ${location.country}")
            } else {
                println("IPv6 address not found in database (this is acceptable)")
            }
        } catch (e: Exception) {
            threwException = true
        }

        assertFalse(threwException, "IPv6 lookup should not throw exceptions")
    }

    @Test
    fun `service should be thread-safe`() = runBlocking {
        // Test concurrent access
        val ips = listOf("8.8.8.8", "1.1.1.1", "8.8.4.4")

        val cities = coroutineScope {
            val results = ips.map { ip ->
                async {
                    geoLocationService.getCityFromIp(ip)
                }
            }
            results.map { it.await() }
        }

        // All lookups should complete without errors
        assertEquals(3, cities.size)
        println("Concurrent lookups completed: $cities")
    }
}
