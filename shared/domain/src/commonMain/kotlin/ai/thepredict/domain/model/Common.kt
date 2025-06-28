package ai.thepredict.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
enum class Currency { EUR, USD }

@Serializable
data class Address(
    val streetName: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val country: String? = null
)

@Serializable
data class Company(
    val id: String? = null, // UUID as string for KMP
    val name: String? = null,
    val address: Address? = null,
    val taxId: String? = null,
    val matchedVat: String? = null,
    val isOwner: Boolean
)

@Serializable
data class User(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class CreateCompanyRequest(
    val name: String,
    val address: Address? = null,
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
    val firstName: String,
    val lastName: String,
    val email: String,
    val password: String
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