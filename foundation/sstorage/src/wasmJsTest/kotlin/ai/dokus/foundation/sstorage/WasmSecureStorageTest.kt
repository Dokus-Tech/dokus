package ai.dokus.foundation.sstorage

import ai.dokus.foundation.domain.model.common.Feature
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WasmSecureStorageTest {
    companion object {
        // Reuse a single instance to avoid multiple DataStores on the same file
        val storage: SecureStorage = createSecureStorage(null, Feature.Invoicing)
    }

    @BeforeTest
    fun setUp() = runTest {
        storage.clear()
    }

    @AfterTest
    fun tearDown() = runTest {
        storage.clear()
    }

    @Test
    fun stringPutGetAndFlow() = runTest {
        val s = storage
        s.set("k", "v1")
        assertEquals("v1", s.get<String>("k"))
        val flow = s.subscribe<String>("k")
        s.set("k", "v2")
        assertEquals("v2", flow.first { it == "v2" })
    }

    @Test
    fun numericAndBooleanOps() = runTest {
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
    fun removeClearContainsAndKeys() = runTest {
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
    fun persistenceAfterRefresh() = runTest {
        // Create new storage instance to test persistence
        val s1 = createSecureStorage(null, Feature.Invoicing)
        s1.set("persist_test", "persistent_value")
        s1.set("persist_int", 123)
        s1.set("persist_bool", true)

        // Simulate page refresh by creating new instance
        val s2 = createSecureStorage(null, Feature.Invoicing)
        assertEquals("persistent_value", s2.get<String>("persist_test"))
        assertEquals(123, s2.get<Int>("persist_int"))
        assertEquals(true, s2.get<Boolean>("persist_bool"))

        // Cleanup
        s2.clear()
    }

    @Test
    fun specialCharactersAndEmptyValues() = runTest {
        val s = storage

        // Test simple special characters
        s.set("special", "!@#$%^&*()")
        assertEquals("!@#$%^&*()", s.get<String>("special"))

        // Test empty string
        s.set("empty", "")
        assertEquals("", s.get<String>("empty"))

        // Test long string without using repeat
        val longString = "abcdabcdabcdabcd" // 16 chars
        s.set("long", longString)
        assertEquals(longString, s.get<String>("long"))
    }
}
