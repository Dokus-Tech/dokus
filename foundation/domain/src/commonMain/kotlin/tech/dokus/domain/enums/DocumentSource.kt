package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

/**
 * Source of a document upload.
 * Used to determine auto-confirmation policy and tracking origin.
 */
@Serializable
enum class DocumentSource(override val dbValue: String, val trustPriority: Int) : DbEnum {
    /** Document uploaded manually by user */
    @SerialName("UPLOAD")
    Upload("UPLOAD", 4),

    /** Document received via email */
    @SerialName("EMAIL")
    Email("EMAIL", 3),

    /** Document received via PEPPOL network (auto-confirmed) */
    @SerialName("PEPPOL")
    Peppol("PEPPOL", 4),

    /** Document created manually (e.g., user entered data directly) */
    @SerialName("MANUAL")
    Manual("MANUAL", 1);

    companion object {
        fun fromDbValue(value: String): DocumentSource = entries.find { it.dbValue == value }!!
    }
}
