package tech.dokus.backend.config

import com.typesafe.config.ConfigFactory
import org.junit.jupiter.api.Test
import tech.dokus.foundation.backend.config.ProcessorConfig
import kotlin.test.assertEquals

class ProcessorConfigParsingTest {

    @Test
    fun `fromConfig parses maxConcurrentRuns`() {
        val config = ConfigFactory.parseString(
            """
            pollingInterval = 5000
            maxAttempts = 3
            batchSize = 10
            maxConcurrentRuns = 4
            """.trimIndent()
        )

        val parsed = ProcessorConfig.fromConfig(config)

        assertEquals(4, parsed.maxConcurrentRuns)
    }

    @Test
    fun `fromConfig coerces maxConcurrentRuns below one`() {
        val config = ConfigFactory.parseString(
            """
            pollingInterval = 5000
            maxAttempts = 3
            batchSize = 10
            maxConcurrentRuns = 0
            """.trimIndent()
        )

        val parsed = ProcessorConfig.fromConfig(config)

        assertEquals(1, parsed.maxConcurrentRuns)
    }
}
