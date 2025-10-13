package ai.dokus.foundation.domain.model

import ai.dokus.foundation.domain.Email
import ai.dokus.foundation.domain.Name
import ai.dokus.foundation.domain.Password
import ai.dokus.foundation.domain.enums.Currency
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
enum class Role {
    owner,
    editor,
    viewer,
    unknown
}

@Serializable
enum class DocumentType {
    receipt,
    invoice
}

@Serializable
enum class ProcessStatus {
    completed,
    ongoing,
    incomplete
}

@Serializable
data class Address(
    @SerialName("street_name")
    val streetName: String? = null,
    @SerialName("city")
    val city: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    @SerialName("country")
    val country: String? = null
)

@Serializable
data class Company(
    val id: String, // UUID as string for KMP
    val name: String,
    val address: Address? = null,
    @SerialName("tax_id")
    val taxId: String,
    @SerialName("is_owner")
    val isOwner: Boolean,
    val avatar: String? = null
)

@Serializable
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String
) {
    companion object {
        fun from(schema: JwtTokenDataSchema): User {
            return User(
                id = schema.id,
                firstName = schema.firstName,
                lastName = schema.lastName,
                email = schema.email
            )
        }
    }
}

@Serializable
data class LoginRequest(
    val email: Email,
    val password: Password
)

@Serializable
data class CreateCompanyRequest(
    val name: String,
    val address: Address,
    val taxId: String
)

@Serializable
data class UpdateCompanyRequest(
    val name: String? = null,
    val address: Address? = null,
    val taxId: String? = null
)

@Serializable
data class CreateUserRequest(
    val firstName: Name,
    val lastName: Name,
    val email: Email,
    val password: Password
)

@Serializable
data class UpdateUserRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null
)

@Serializable
data class Transaction(
    val id: String,                  // UUID as string
    val transactionId: String,       // UUID as string
    val fileName: String,
    val fileUrl: String,
    val amount: Double,
    val date: String,                // ISO8601 string
    val description: String,
    val currency: Currency? = null,
    val status: ProcessStatus = ProcessStatus.incomplete,
    val vatNumber: String? = null,
    val clientName: String? = null,
    val createdAt: String? = null,   // ISO8601 string
    val updatedAt: String? = null    // ISO8601 string
)

@Serializable
data class MatchedSchema(
    val document: Document, // see sealed class below
    val transaction: Transaction
)

@Serializable
data class SimpleMatchDocumentsResult(
    val documents: List<Document> = emptyList(),
    val transactions: List<Transaction> = emptyList(),
    val matches: List<MatchedSchema> = emptyList(),
    val potentialMatches: List<MatchedSchema> = emptyList(),
    val zeroAmountDocuments: List<Document> = emptyList(),
    val failed: List<Document> = emptyList()
)

@Serializable
sealed class Document {
    @Serializable
    @SerialName("invoice")
    data class InvoiceDoc(
        val id: String,
        val fileName: String,
        val fileUrl: String,
        val invoiceDate: String? = null,
        val changeRate: Double? = null,
        val totalAmount: Double? = null,
        val totalAmountWithoutVat: Double? = null,
        val totalVat: Double? = null,
        val sender: Company? = null,
        val receiver: Company? = null,
        val invoiceNumber: String? = null,
        val currency: Currency? = null,
        val confidenceScore: Double? = null,
        val status: ProcessStatus? = null,
        val documentType: DocumentType = DocumentType.invoice
    ) : Document()

    @Serializable
    @SerialName("receipt")
    data class ReceiptDoc(
        val id: String,
        val fileName: String,
        val fileUrl: String,
        val receiptDate: String? = null,
        val changeRate: Double? = null,
        val totalAmount: Double? = null,
        val totalAmountWithoutVat: Double? = null,
        val totalVat: Double? = null,
        val merchant: Company? = null,
        val receiptNumber: String? = null,
        val currency: Currency? = null,
        val confidenceScore: Double? = null,
        val status: ProcessStatus? = null,
        val documentType: DocumentType = DocumentType.receipt
    ) : Document()
}

@Serializable
data class InfoSchema(
    val version: String, val llmConfig: LlmConfig
)

@Serializable
data class LlmConfig(
    val model: String,
    val provider: String,
    val temperature: Double? = null,
    val responseFormat: String? = null,
    val modelKwargs: Map<String, String>? = null
)

@JvmInline
value class TaxNumber(private val value: String) {
    companion object {
        const val LENGTH = 12

        val formattedRegex = Regex("^[A-Z]{2}\\d{4}\\.\\d{3}\\.\\d{3}\$")

        private fun formatNumber(value: String): String {
            return buildString {
                append(value.substring(0, 2))
                append(value.substring(2, 6))
                append(".")
                append(value.substring(6, 9))
                append(".")
                append(value.substring(9, 12))
            }
        }

        fun canBeUsed(value: String): Boolean {
            return TaxNumber(value).raw.length == LENGTH
        }
    }

    val raw: String
        get() = value.replace(".", "").replace(" ", "")

    val country: String
        get() = formatted.substring(0, 2)

    val formatted: String
        get() = if (formattedRegex.matches(value)) value else formatNumber(value)
}

enum class Country {
    BE;

    companion object {
        val default = BE
    }
}