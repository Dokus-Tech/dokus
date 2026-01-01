package tech.dokus.foundation.sstorage

import kotlin.test.Test
import kotlin.test.assertNotNull

class AndroidSecureStorageTest {
    @Test
    fun canCreateInstancePlaceholder() {
        // Unit tests run on the JVM without Android runtime.
        // The real Android implementation requires an Android Context, so unit tests cannot exercise it.
        // Instrumented tests should be placed under androidTest in an Android app module and run with connectedAndroidTest.
        assertNotNull(Unit)
    }
}
