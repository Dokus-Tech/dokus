package ai.dokus.foundation.sstorage

import ai.dokus.foundation.domain.model.common.Feature
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class JvmSecureStorageTest {
    companion object {
        // Reuse a single instance to avoid multiple DataStores on the same file
        // Use unique feature to avoid conflicts with other test classes
        val storage: SecureStorage = createSecureStorage(null, Feature.Reporting)
    }

    @BeforeTest
    fun setUp() = runBlocking {
        storage.clear()
    }

    @AfterTest
    fun tearDown() = runBlocking {
        storage.clear()
    }

    @Test
    fun stringPutGetAndFlow() = runBlocking {
        val s = storage
        s.set("k", "v1")
        assertEquals("v1", s.get<String>("k"))
        val flow = s.subscribe<String>("k")
        s.set("k", "v2")
        assertEquals("v2", flow.first { it == "v2" })
    }

    @Test
    fun numericAndBooleanOps() = runBlocking {
        val s = storage
        s.set("i", 42)
        s.set("l", 1234567890123L)
        s.set("f", 3.14f)
        s.set("d", 3.14159)
        s.set("b", true)
        assertEquals(42, s.get<Int>("i"))
        assertEquals(1234567890123L, s.get<Long>("l"))
        assertEquals(3.14f, s.get<Float>("f"))
        assertEquals(3.14159, s.get<Double>("d"))
        assertEquals(true, s.get<Boolean>("b"))
        // observe
        val iFlow = s.subscribe<Int>("i")
        val bFlow = s.subscribe<Boolean>("b")
        s.set("i", 43)
        s.set("b", false)
        assertEquals(43, iFlow.first { it == 43 })
        assertEquals(false, bFlow.first { it == false })
    }

    @Test
    fun removeClearContainsAndKeys() = runBlocking {
        val s = storage
        s.set("a", "1")
        s.set("b", "2")
        assertTrue(s.contains("a"))
        assertTrue(s.contains("b"))
        var keys = s.getAllKeys()
        assertTrue(keys.contains("a") && keys.contains("b"))
        s.remove("a")
        assertFalse(s.contains("a"))
        assertEquals("2", s.get<String>("b"))
        s.clear()
        keys = s.getAllKeys()
        assertTrue(keys.isEmpty())
        assertNull(s.get<String>("b"))
    }

    @Test
    fun testNullHandling() = runBlocking {
        val s = storage
        s.set("key", "value")
        assertTrue(s.contains("key"))
        s.set<String>("key", null)
        assertFalse(s.contains("key"))
        assertNull(s.get<String>("key"))
    }

    @Test
    fun testDefaultValues() = runBlocking {
        val s = storage
        assertEquals(42, s.get("nonexistent", 42))
        assertEquals("default", s.get("nonexistent", "default"))
        assertEquals(true, s.get("nonexistent", true))
    }
}
