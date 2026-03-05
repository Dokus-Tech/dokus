package tech.dokus.domain.enums

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.database.DbEnum

@Serializable
enum class BusinessProfileSubjectType(override val dbValue: String) : DbEnum {
    @SerialName("TENANT")
    Tenant("TENANT"),

    @SerialName("CONTACT")
    Contact("CONTACT")
}

@Serializable
enum class BusinessProfileVerificationState(override val dbValue: String) : DbEnum {
    @SerialName("UNSET")
    Unset("UNSET"),

    @SerialName("SUGGESTED")
    Suggested("SUGGESTED"),

    @SerialName("VERIFIED")
    Verified("VERIFIED")
}

@Serializable
enum class BusinessProfileEnrichmentJobStatus(override val dbValue: String) : DbEnum {
    @SerialName("PENDING")
    Pending("PENDING"),

    @SerialName("PROCESSING")
    Processing("PROCESSING"),

    @SerialName("RETRY")
    Retry("RETRY"),

    @SerialName("COMPLETED")
    Completed("COMPLETED")
}

@Serializable
enum class BusinessProfileEvidenceCheck {
    // Hard checks
    @SerialName("VAT_FOUND_ON_SITE")
    VatFoundOnSite,

    @SerialName("DOMAIN_CONTAINS_COMPANY_NAME")
    DomainContainsCompanyName,

    @SerialName("STRUCTURED_DATA_MATCH")
    StructuredDataMatch,

    // Supporting checks
    @SerialName("ADDRESS_MATCH")
    AddressMatch,

    @SerialName("PHONE_OR_EMAIL_MATCH")
    PhoneOrEmailMatch,

    @SerialName("DOMAIN_TLD_COUNTRY_MATCH")
    DomainTldCountryMatch,

    @SerialName("NOT_AGGREGATOR")
    NotAggregator,

    // Weak checks
    @SerialName("LLM_CONFIDENCE_HIGH")
    LlmConfidenceHigh,

    @SerialName("SINGLE_DOMINANT_RESULT")
    SingleDominantResult,
}
