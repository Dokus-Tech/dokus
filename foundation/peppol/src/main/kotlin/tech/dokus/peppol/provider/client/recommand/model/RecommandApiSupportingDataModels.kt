package tech.dokus.peppol.provider.client.recommand.model

import kotlinx.serialization.Serializable

/**
 * Supplier object as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/suppliers` (query params: `page`, `limit`, `search`)
 * - `POST /api/v1/suppliers`
 * - `GET /api/v1/suppliers/{supplierId}` (path params: `supplierId`)
 */
@Serializable
data class RecommandSupplier(
    val id: String,
    val teamId: String,
    val externalId: String? = null,
    val name: String,
    val vatNumber: String? = null,
    val peppolAddresses: List<String> = emptyList(),
    val createdAt: String,
    val updatedAt: String,
    val labels: List<RecommandDocumentLabel> = emptyList(),
)

/**
 * Query parameters for `GET /api/v1/suppliers`.
 */
@Serializable
data class RecommandGetSuppliersRequest(
    val page: Int = 1,
    val limit: Int = 10,
    val search: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/suppliers`.
 */
@Serializable
data class RecommandGetSuppliersResponse(
    val success: Boolean,
    val suppliers: List<RecommandSupplier>,
    val pagination: RecommandApiPagination,
)

/**
 * Request body for `POST /api/v1/suppliers` (upsert supplier).
 */
@Serializable
data class RecommandUpsertSupplierRequest(
    val name: String,
    val id: String? = null,
    val externalId: String? = null,
    val vatNumber: String? = null,
    val peppolAddresses: List<String> = emptyList(),
)

/**
 * Wrapper response for `POST /api/v1/suppliers`.
 */
@Serializable
data class RecommandUpsertSupplierResponse(
    val success: Boolean,
    val supplier: RecommandSupplier,
)

/**
 * Path parameters for `GET /api/v1/suppliers/{supplierId}`.
 */
@Serializable
data class RecommandGetSupplierRequest(
    val supplierId: String,
)

/**
 * Wrapper response for `GET /api/v1/suppliers/{supplierId}`.
 */
@Serializable
data class RecommandGetSupplierResponse(
    val success: Boolean,
    val supplier: RecommandSupplier,
)

/**
 * Path parameters for `DELETE /api/v1/suppliers/{supplierId}`.
 */
@Serializable
data class RecommandDeleteSupplierRequest(
    val supplierId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/suppliers/{supplierId}`.
 */
@Serializable
data class RecommandDeleteSupplierResponse(
    val success: Boolean,
)

/**
 * Path parameters for assigning a label to a supplier.
 *
 * Used in:
 * - `POST /api/v1/suppliers/{supplierId}/labels/{labelId}` (path params: `supplierId`, `labelId`)
 */
@Serializable
data class RecommandAssignLabelToSupplierRequest(
    val supplierId: String,
    val labelId: String,
)

/**
 * Response for assigning a label to a supplier.
 */
@Serializable
data class RecommandAssignLabelToSupplierResponse(
    val success: Boolean,
)

/**
 * Path parameters for unassigning a label from a supplier.
 *
 * Used in:
 * - `DELETE /api/v1/suppliers/{supplierId}/labels/{labelId}` (path params: `supplierId`, `labelId`)
 */
@Serializable
data class RecommandUnassignLabelFromSupplierRequest(
    val supplierId: String,
    val labelId: String,
)

/**
 * Response for unassigning a label from a supplier.
 */
@Serializable
data class RecommandUnassignLabelFromSupplierResponse(
    val success: Boolean,
)

/**
 * Customer object as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/customers` (query params: `page`, `limit`, `search`)
 * - `POST /api/v1/customers`
 * - `GET /api/v1/customers/{customerId}` (path params: `customerId`)
 */
@Serializable
data class RecommandCustomer(
    val id: String,
    val teamId: String,
    val externalId: String? = null,
    val name: String,
    val vatNumber: String? = null,
    val enterpriseNumber: String? = null,
    val peppolAddresses: List<String> = emptyList(),
    val address: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val email: String? = null,
    val phone: String? = null,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Query parameters for `GET /api/v1/customers`.
 */
@Serializable
data class RecommandGetCustomersRequest(
    val page: Int = 1,
    val limit: Int = 10,
    val search: String? = null,
)

/**
 * Wrapper response for `GET /api/v1/customers`.
 */
@Serializable
data class RecommandGetCustomersResponse(
    val success: Boolean,
    val customers: List<RecommandCustomer>,
    val pagination: RecommandApiPagination,
)

/**
 * Request body for `POST /api/v1/customers` (upsert customer).
 */
@Serializable
data class RecommandUpsertCustomerRequest(
    val name: String,
    val address: String,
    val city: String,
    val postalCode: String,
    val country: String,
    val id: String? = null,
    val externalId: String? = null,
    val vatNumber: String? = null,
    val enterpriseNumber: String? = null,
    val peppolAddresses: List<String> = emptyList(),
    val email: String? = null,
    val phone: String? = null,
)

/**
 * Wrapper response for `POST /api/v1/customers`.
 */
@Serializable
data class RecommandUpsertCustomerResponse(
    val success: Boolean,
    val customer: RecommandCustomer,
)

/**
 * Path parameters for `GET /api/v1/customers/{customerId}`.
 */
@Serializable
data class RecommandGetCustomerRequest(
    val customerId: String,
)

/**
 * Wrapper response for `GET /api/v1/customers/{customerId}`.
 */
@Serializable
data class RecommandGetCustomerResponse(
    val success: Boolean,
    val customer: RecommandCustomer,
)

/**
 * Path parameters for `DELETE /api/v1/customers/{customerId}`.
 */
@Serializable
data class RecommandDeleteCustomerRequest(
    val customerId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/customers/{customerId}`.
 */
@Serializable
data class RecommandDeleteCustomerResponse(
    val success: Boolean,
)

/**
 * Label object as returned by the Recommand API.
 *
 * Returned by:
 * - `GET /api/v1/labels`
 * - `POST /api/v1/labels`
 * - `GET /api/v1/labels/{labelId}` (path params: `labelId`)
 * - `PUT /api/v1/labels/{labelId}` (path params: `labelId`)
 */
@Serializable
data class RecommandLabel(
    val id: String,
    val teamId: String,
    val externalId: String? = null,
    val name: String,
    val colorHex: String,
    val createdAt: String,
    val updatedAt: String,
)

/**
 * Wrapper response for `GET /api/v1/labels`.
 */
@Serializable
data class RecommandGetLabelsResponse(
    val success: Boolean,
    val labels: List<RecommandLabel>,
)

/**
 * Request body for `POST /api/v1/labels`.
 */
@Serializable
data class RecommandCreateLabelRequest(
    val name: String,
    val colorHex: String,
    val externalId: String? = null,
)

/**
 * Wrapper response for `POST /api/v1/labels`.
 */
@Serializable
data class RecommandCreateLabelResponse(
    val success: Boolean,
    val label: RecommandLabel,
)

/**
 * Path parameters for `GET /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandGetLabelRequest(
    val labelId: String,
)

/**
 * Wrapper response for `GET /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandGetLabelResponse(
    val success: Boolean,
    val label: RecommandLabel,
)

/**
 * Request body for `PUT /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandUpdateLabelRequest(
    val name: String? = null,
    val colorHex: String? = null,
    val externalId: String? = null,
)

/**
 * Path parameters for `PUT /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandUpdateLabelPath(
    val labelId: String,
)

/**
 * Wrapper response for `PUT /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandUpdateLabelResponse(
    val success: Boolean,
    val label: RecommandLabel,
)

/**
 * Path parameters for `DELETE /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandDeleteLabelRequest(
    val labelId: String,
)

/**
 * Wrapper response for `DELETE /api/v1/labels/{labelId}`.
 */
@Serializable
data class RecommandDeleteLabelResponse(
    val success: Boolean,
)

