package ai.dokus.foundation.sstorage

import ai.dokus.foundation.domain.model.common.Feature
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IOSSecureStorageTest {
    // Use unique feature to avoid conflicts with other test classes
    private val storage: SecureStorage = createSecureStorage(null, Feature.Payment)

    @BeforeTest
    fun setUp() = runBlocking {
        storage.clear()
    }

    @AfterTest
    fun tearDown() = runBlocking {
        storage.clear()
    }

    @Test
    fun stringPutGetAndObserve() = runBlocking {
        storage.set("k", "v1")
        assertEquals("v1", storage.get<String>("k"))
        val flow = storage.subscribe<String>("k")
        storage.set("k", "v2")
        // Wait briefly to allow Keychain write + flow propagation
        assertEquals("v2", flow.first { it == "v2" })
    }

    @Test
    fun numericAndBooleanOps() = runBlocking {
        storage.set("i", 42)
        storage.set("l", 1234567890123L)
        storage.set("f", 3.14f)
        storage.set("b", true)
        assertEquals(42, storage.get<Int>("i"))
        assertEquals(1234567890123L, storage.get<Long>("l"))
        assertEquals(3.14f, storage.get<Float>("f"))
        assertEquals(true, storage.get<Boolean>("b"))
    }

    @Test
    fun containsRemoveAndKeys() = runBlocking {
        storage.set("a", "1")
        storage.set("b", "2")
        assertTrue(storage.contains("a"))
        assertTrue(storage.contains("b"))
        var keys = storage.getAllKeys()
        assertTrue("a" in keys && "b" in keys)
        storage.remove("a")
        assertFalse(storage.contains("a"))
        assertEquals("2", storage.get<String>("b"))
        storage.clear()
        keys = storage.getAllKeys()
        assertTrue(keys.isEmpty())
    }
}