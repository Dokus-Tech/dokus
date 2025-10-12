package ai.dokus.foundation.database.mappers

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.toJavaUuid
import kotlin.uuid.toKotlinUuid

/**
 * Extension functions for converting between Java UUID and Kotlin Uuid
 * Used for database mappings
 *
 * These are re-exports of the built-in Kotlin UUID conversion functions
 * to make them easily accessible in the mappers package.
 */

// The actual conversion functions are provided by the kotlin.uuid package:
// - java.util.UUID.toKotlinUuid() -> Uuid
// - Uuid.toJavaUuid() -> java.util.UUID