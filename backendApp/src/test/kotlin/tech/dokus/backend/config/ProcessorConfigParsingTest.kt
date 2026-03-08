package tech.dokus.backend.config

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test
import tech.dokus.foundation.backend.config.ProcessorConfig
import kotlin.test.assertEquals

class ProcessorConfigParsingTest {

    @Test
    fun `fromConfig parses all fields`() {
        val config = ConfigFactory.parseString(
            """
            pollingInterval = 5000
            maxAttempts = 3
            batchSize = 10
            """.trimIndent()
        )

        val parsed = ProcessorConfig.fromConfig(config)

        assertEquals(5000L, parsed.pollingInterval)
        assertEquals(3, parsed.maxAttempts)
        assertEquals(10, parsed.batchSize)
    }
}
