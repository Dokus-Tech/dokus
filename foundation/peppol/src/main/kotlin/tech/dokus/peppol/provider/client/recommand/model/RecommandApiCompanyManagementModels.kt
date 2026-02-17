package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Company object as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/companies` (query params: `enterpriseNumber`, `vatNumber`)
 * - `POST /api/v1/companies`
 * - `GET /api/v1/companies/{companyId}` (path params: `companyId`)
 * - `PUT /api/v1/companies/{companyId}` (path params: `companyId`)
 */
@Serializable
data class RecommandCompany(
    val id: String,
    val teamId: String,
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val country: String,
    val enterpriseNumber: String,
    val vatNumber: String,
    val isSmpRecipient: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Query parameters for `GET /api/v1/companies`.
 */
@Serializable
data class RecommandGetCompaniesRequest(
    val enterpriseNumber: String? = null,
    val vatNumber: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/companies`.
 */
@Serializable
data class RecommandGetCompaniesResponse(
    val success: Boolean,
    val companies: List<RecommandCompany>,
)

/**
 * Request body for `POST /api/v1/companies`.
 */
@Serializable
data class RecommandCreateCompanyRequest(
    val name: String,
    val address: String,
    val postalCode: String,
    val city: String,
    val country: RecommandCompanyCountry,
    val enterpriseNumber: String? = null,
    val vatNumber: String? = null,
    val isSmpRecipient: Boolean = true,
    val skipDefaultCompanySetup: Boolean = false,
)

/**
 * Country codes accepted by `POST /api/v1/companies` and `PUT /api/v1/companies/{companyId}`.
 */
@Serializable
enum class RecommandCompanyCountry {
    AU,
    AT,
    BE,
    CA,
    HR,
    DK,
    EE,
    FI,
    FR,
    DE,
    GR,
    IS,
    IE,
    IT,
    JP,
    LU,
    MY,
    NL,
    NZ,
    NO,
    PL,
    SG,
    SK,
    SE,
    AE,
    GB,
    US,
}

/**
 * Wrapper response for `POST /api/v1/companies`.
 */
@Serializable
data class RecommandCreateCompanyResponse(
    val success: Boolean,
    val company: RecommandCompany,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandGetCompanyRequest(
    val companyId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandGetCompanyResponse(
    val success: Boolean,
    val company: RecommandCompany,
)

/**
 * Request body for `PUT /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandUpdateCompanyRequest(
    val name: String? = null,
    val address: String? = null,
    val postalCode: String? = null,
    val city: String? = null,
    val country: RecommandCompanyCountry? = null,
    val enterpriseNumber: String? = null,
    val vatNumber: String? = null,
    val isSmpRecipient: Boolean? = null,
)

/**
 * Wrapper response for `PUT /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandUpdateCompanyResponse(
    val success: Boolean,
    val company: RecommandCompany,
)

/**
 * Path parameters for `PUT /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandUpdateCompanyPath(
    val companyId: String,
)

/**
 * Path parameters for `DELETE /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandDeleteCompanyRequest(
    val companyId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/companies/{companyId}`.
 */
@Serializable
data class RecommandDeleteCompanyResponse(
    val success: Boolean,
)

/**
 * Company identifier (scheme + identifier) as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/companies/{companyId}/identifiers` (path params: `companyId`)
 * - `POST /api/v1/companies/{companyId}/identifiers` (path params: `companyId`)
 * - `GET /api/v1/companies/{companyId}/identifiers/{identifierId}` (path params: `companyId`, `identifierId`)
 * - `PUT /api/v1/companies/{companyId}/identifiers/{identifierId}` (path params: `companyId`, `identifierId`)
 */
@Serializable
data class RecommandCompanyIdentifier(
    val id: String,
    val companyId: String,
    val scheme: String,
    val identifier: String,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}/identifiers`.
 */
@Serializable
data class RecommandGetCompanyIdentifiersRequest(
    val companyId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}/identifiers`.
 */
@Serializable
data class RecommandGetCompanyIdentifiersResponse(
    val success: Boolean,
    val identifiers: List<RecommandCompanyIdentifier>,
)

/**
 * Request body for `POST /api/v1/companies/{companyId}/identifiers`.
 */
@Serializable
data class RecommandCreateCompanyIdentifierRequest(
    val scheme: String,
    val identifier: String,
)

/**
 * Wrapper response for `POST /api/v1/companies/{companyId}/identifiers`.
 */
@Serializable
data class RecommandCreateCompanyIdentifierResponse(
    val success: Boolean,
    val identifier: RecommandCompanyIdentifier,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandGetCompanyIdentifierRequest(
    val companyId: String,
    val identifierId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandGetCompanyIdentifierResponse(
    val success: Boolean,
    val identifier: RecommandCompanyIdentifier,
)

/**
 * Request body for `PUT /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandUpdateCompanyIdentifierRequest(
    val scheme: String,
    val identifier: String,
)

/**
 * Path parameters for `PUT /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandUpdateCompanyIdentifierPath(
    val companyId: String,
    val identifierId: String,
)

/**
 * Wrapper response for `PUT /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandUpdateCompanyIdentifierResponse(
    val success: Boolean,
    val identifier: RecommandCompanyIdentifier,
)

/**
 * Path parameters for `DELETE /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandDeleteCompanyIdentifierRequest(
    val companyId: String,
    val identifierId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/companies/{companyId}/identifiers/{identifierId}`.
 */
@Serializable
data class RecommandDeleteCompanyIdentifierResponse(
    val success: Boolean,
)

/**
 * Company document type as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/companies/{companyId}/document-types` (path params: `companyId`)
 * - `POST /api/v1/companies/{companyId}/document-types` (path params: `companyId`)
 * - `GET /api/v1/companies/{companyId}/document-types/{documentTypeId}` (path params: `companyId`, `documentTypeId`)
 * - `PUT /api/v1/companies/{companyId}/document-types/{documentTypeId}` (path params: `companyId`, `documentTypeId`)
 */
@Serializable
data class RecommandCompanyDocumentType(
    val id: String,
    val companyId: String,
    val docTypeId: String,
    val processId: String,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}/document-types`.
 */
@Serializable
data class RecommandGetCompanyDocumentTypesRequest(
    val companyId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}/document-types`.
 */
@Serializable
data class RecommandGetCompanyDocumentTypesResponse(
    val success: Boolean,
    val documentTypes: List<RecommandCompanyDocumentType>,
)

/**
 * Request body for `POST /api/v1/companies/{companyId}/document-types`.
 */
@Serializable
data class RecommandCreateCompanyDocumentTypeRequest(
    val docTypeId: String,
    val processId: String,
)

/**
 * Wrapper response for `POST /api/v1/companies/{companyId}/document-types`.
 */
@Serializable
data class RecommandCreateCompanyDocumentTypeResponse(
    val success: Boolean,
    val documentType: RecommandCompanyDocumentType,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandGetCompanyDocumentTypeRequest(
    val companyId: String,
    val documentTypeId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandGetCompanyDocumentTypeResponse(
    val success: Boolean,
    val documentType: RecommandCompanyDocumentType,
)

/**
 * Request body for `PUT /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandUpdateCompanyDocumentTypeRequest(
    val docTypeId: String,
    val processId: String,
)

/**
 * Path parameters for `PUT /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandUpdateCompanyDocumentTypePath(
    val companyId: String,
    val documentTypeId: String,
)

/**
 * Wrapper response for `PUT /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandUpdateCompanyDocumentTypeResponse(
    val success: Boolean,
    val documentType: RecommandCompanyDocumentType,
)

/**
 * Path parameters for `DELETE /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandDeleteCompanyDocumentTypeRequest(
    val companyId: String,
    val documentTypeId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/companies/{companyId}/document-types/{documentTypeId}`.
 */
@Serializable
data class RecommandDeleteCompanyDocumentTypeResponse(
    val success: Boolean,
)

/**
 * Notification email address configuration for a company.
 *
 * Returned by:
 * - `GET /api/v1/companies/{companyId}/notification-email-addresses` (path params: `companyId`)
 * - `POST /api/v1/companies/{companyId}/notification-email-addresses` (path params: `companyId`)
 * - `GET /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`
 *   (path params: `companyId`, `notificationEmailAddressId`)
 * - `PUT /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`
 *   (path params: `companyId`, `notificationEmailAddressId`)
 */
@Serializable
data class RecommandCompanyNotificationEmailAddress(
    val id: String,
    val companyId: String,
    val email: String,
    val notifyIncoming: Boolean,
    val notifyOutgoing: Boolean,
    val includeAutoGeneratedPdfIncoming: Boolean,
    val includeAutoGeneratedPdfOutgoing: Boolean,
    val includeDocumentJsonIncoming: Boolean,
    val includeDocumentJsonOutgoing: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}/notification-email-addresses`.
 */
@Serializable
data class RecommandGetCompanyNotificationEmailAddressesRequest(
    val companyId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}/notification-email-addresses`.
 */
@Serializable
data class RecommandGetCompanyNotificationEmailAddressesResponse(
    val success: Boolean,
    val notificationEmailAddresses: List<RecommandCompanyNotificationEmailAddress>,
)

/**
 * Request body for `POST /api/v1/companies/{companyId}/notification-email-addresses`.
 */
@Serializable
data class RecommandCreateCompanyNotificationEmailAddressRequest(
    val email: String,
    val notifyIncoming: Boolean,
    val notifyOutgoing: Boolean,
    val includeAutoGeneratedPdfIncoming: Boolean = false,
    val includeAutoGeneratedPdfOutgoing: Boolean = false,
    val includeDocumentJsonIncoming: Boolean = false,
    val includeDocumentJsonOutgoing: Boolean = false,
)

/**
 * Wrapper response for `POST /api/v1/companies/{companyId}/notification-email-addresses`.
 */
@Serializable
data class RecommandCreateCompanyNotificationEmailAddressResponse(
    val success: Boolean,
    val notificationEmailAddress: RecommandCompanyNotificationEmailAddress,
)

/**
 * Path parameters for `GET /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandGetCompanyNotificationEmailAddressRequest(
    val companyId: String,
    val notificationEmailAddressId: String,
)

/**
 * Wrapper response for `GET /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandGetCompanyNotificationEmailAddressResponse(
    val success: Boolean,
    val notificationEmailAddress: RecommandCompanyNotificationEmailAddress,
)

/**
 * Request body for `PUT /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandUpdateCompanyNotificationEmailAddressRequest(
    val email: String,
    val notifyIncoming: Boolean,
    val notifyOutgoing: Boolean,
    val includeAutoGeneratedPdfIncoming: Boolean = false,
    val includeAutoGeneratedPdfOutgoing: Boolean = false,
    val includeDocumentJsonIncoming: Boolean = false,
    val includeDocumentJsonOutgoing: Boolean = false,
)

/**
 * Path parameters for `PUT /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandUpdateCompanyNotificationEmailAddressPath(
    val companyId: String,
    val notificationEmailAddressId: String,
)

/**
 * Wrapper response for `PUT /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandUpdateCompanyNotificationEmailAddressResponse(
    val success: Boolean,
    val notificationEmailAddress: RecommandCompanyNotificationEmailAddress,
)

/**
 * Path parameters for `DELETE /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandDeleteCompanyNotificationEmailAddressRequest(
    val companyId: String,
    val notificationEmailAddressId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/companies/{companyId}/notification-email-addresses/{notificationEmailAddressId}`.
 */
@Serializable
data class RecommandDeleteCompanyNotificationEmailAddressResponse(
    val success: Boolean,
)
