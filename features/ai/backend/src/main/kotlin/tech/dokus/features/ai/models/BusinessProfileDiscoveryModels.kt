package tech.dokus.features.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import tech.dokus.domain.enums.Language

@Serializable
data class BusinessProfileEnrichmentInput(
    val tenantId: String,
    val subjectType: String,
    val subjectId: String,
    val companyName: String,
    val companyVatNumber: String? = null,
    val companyCountry: String? = null,
    val companyCity: String? = null,
    val companyPostalCode: String? = null,
    val companyEmail: String? = null,
    val companyPhone: String? = null,
    val outputLanguage: Language,
    val maxPages: Int = 5,
)

@Serializable
enum class BusinessDiscoveryStatus {
    @SerialName("FOUND")
    Found,

    @SerialName("NOT_FOUND")
    NotFound,
}

@Serializable
data class BusinessProfileDiscoveryResult(
    val status: BusinessDiscoveryStatus,
    val candidateWebsiteUrl: String? = null,
    val businessSummary: String? = null,
    val activities: List<String> = emptyList(),
    val logoUrl: String? = null,
    val confidence: Double = 0.0,
    val candidateReasons: List<String> = emptyList(),
    val searchResultUrls: List<String> = emptyList(),
)

@Serializable
data class BusinessProfileContentPage(
    val url: String,
    val title: String? = null,
    val description: String? = null,
    val textContent: String,
    val structuredDataSnippets: List<String> = emptyList(),
)

@Serializable
data class BusinessProfileContentExtractionInput(
    val companyName: String,
    val companyVatNumber: String? = null,
    val websiteUrl: String,
    val outputLanguage: Language,
    val pages: List<BusinessProfileContentPage>,
)

@Serializable
data class BusinessProfileContentExtractionResult(
    val businessSummary: String? = null,
    val activities: List<String> = emptyList(),
    val confidence: Double = 0.0,
)
