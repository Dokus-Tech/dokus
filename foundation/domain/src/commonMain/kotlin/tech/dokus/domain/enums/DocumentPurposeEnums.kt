package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class DocumentPurposeSource(override val dbValue: String) : DbEnum {
    @SerialName("AI_TEMPLATE")
    AiTemplate("AI_TEMPLATE"),

    @SerialName("AI_RAG")
    AiRag("AI_RAG"),

    @SerialName("USER")
    User("USER");
}

@Serializable
enum class PurposePeriodMode(override val dbValue: String) : DbEnum {
    @SerialName("ISSUE_MONTH")
    IssueMonth("ISSUE_MONTH"),

    @SerialName("SERVICE_PERIOD")
    ServicePeriod("SERVICE_PERIOD"),

    @SerialName("NONE")
    None("NONE");
}
