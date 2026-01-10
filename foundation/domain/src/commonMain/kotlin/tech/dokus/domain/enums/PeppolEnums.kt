package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Status of a PEPPOL directory lookup.
 * Stored in peppol_directory_cache table.
 */
@Serializable
enum class PeppolLookupStatus(override val dbValue: String) : DbEnum {
    @SerialName("found")
    Found("found"),

    @SerialName("not_found")
    NotFound("not_found"),

    @SerialName("error")
    Error("error")
}

/**
 * Source of a PEPPOL directory cache entry.
 */
@Serializable
enum class PeppolLookupSource(override val dbValue: String) : DbEnum {
    @SerialName("directory")
    Directory("directory"),

    @SerialName("manual")
    Manual("manual")
}
