package ai.dokus.foundation.database.services

import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*

/**
 * Peppol Access Point Client
 * Handles transmission of UBL invoices through the Peppol network
 *
 * Peppol (Pan-European Public Procurement On-Line) is a network that enables
 * cross-border e-procurement. Belgium requires all B2B invoices to be sent via
 * Peppol starting January 1, 2026.
 *
 * This implementation supports AS4 protocol (the modern Peppol transport)
 *
 * Reference: https://peppol.org/
 */
interface PeppolAccessPoint {
    /**
     * Send an invoice through the Peppol network
     * @param ublXml The UBL 2.1 invoice XML
     * @param senderParticipantId Sender's Peppol participant ID (format: 0208:BE0123456789)
     * @param receiverParticipantId Receiver's Peppol participant ID
     * @param documentId Unique document identifier
     * @return Transmission result with message ID
     */
    suspend fun sendInvoice(
        ublXml: String,
        senderParticipantId: String,
        receiverParticipantId: String,
        documentId: String
    ): PeppolTransmissionResult

    /**
     * Check transmission status
     * @param messageId The message ID from transmission
     * @return Current status of the transmission
     */
    suspend fun getTransmissionStatus(messageId: String): PeppolTransmissionStatus

    /**
     * Lookup a participant in the Peppol network
     * @param participantId Peppol participant ID to lookup
     * @return True if participant is registered in Peppol network
     */
    suspend fun lookupParticipant(participantId: String): Boolean

    /**
     * Get capabilities of a participant
     * @param participantId Peppol participant ID
     * @return List of document types the participant can receive
     */
    suspend fun getParticipantCapabilities(participantId: String): List<String>
}

/**
 * Peppol transmission result
 */
@kotlinx.serialization.Serializable
data class PeppolTransmissionResult(
    val messageId: String,
    val timestamp: String,
    val status: String,
    val receiverParticipantId: String,
    val documentId: String
)

/**
 * Peppol transmission status
 */
enum class PeppolTransmissionStatus {
    PENDING,      // Waiting to be sent
    SENT,         // Successfully sent to access point
    DELIVERED,    // Delivered to receiver's access point
    RECEIVED,     // Acknowledged by receiver
    FAILED,       // Transmission failed
    REJECTED      // Rejected by receiver
}

/**
 * OpenPeppol Access Point implementation
 * Uses Peppol AS4 protocol for message exchange
 */
class OpenPeppolAccessPoint(
    private val accessPointUrl: String,
    private val participantId: String,
    private val certificate: String,
    private val privateKey: String
) : PeppolAccessPoint {

    private val logger = LoggerFactory.getLogger(OpenPeppolAccessPoint::class.java)

    companion object {
        // Peppol document types
        const val INVOICE_DOCTYPE = "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2::Invoice##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"
        const val CREDIT_NOTE_DOCTYPE = "urn:oasis:names:specification:ubl:schema:xsd:CreditNote-2::CreditNote##urn:cen.eu:en16931:2017#compliant#urn:fdc:peppol.eu:2017:poacc:billing:3.0::2.1"

        // Process identifiers
        const val BILLING_PROCESS = "urn:fdc:peppol.eu:2017:poacc:billing:01:1.0"

        // Belgian participant ID scheme
        const val BELGIAN_COMPANY_SCHEME = "0208" // Belgian company number scheme
    }

    override suspend fun sendInvoice(
        ublXml: String,
        senderParticipantId: String,
        receiverParticipantId: String,
        documentId: String
    ): PeppolTransmissionResult {
        logger.info("Sending invoice to Peppol network: sender=$senderParticipantId, receiver=$receiverParticipantId")

        // TODO: Implement AS4 message construction and transmission
        // This requires:
        // 1. AS4 SOAP envelope creation
        // 2. Digital signature with certificate
        // 3. SMP (Service Metadata Publisher) lookup for receiver endpoint
        // 4. HTTP POST to receiver's access point
        // 5. Process MDN (Message Disposition Notification)
        //
        // Example using Oxalis (open-source Peppol access point):
        // val oxalisClient = OxalisClient(accessPointUrl)
        // val transmission = oxalisClient.send(
        //     sender = PeppolParticipantId.of(senderParticipantId),
        //     receiver = PeppolParticipantId.of(receiverParticipantId),
        //     documentType = PeppolDocumentTypeId.of(INVOICE_DOCTYPE),
        //     processType = PeppolProcessTypeId.of(BILLING_PROCESS),
        //     payload = ublXml.toByteArray()
        // )
        //
        // return PeppolTransmissionResult(
        //     messageId = transmission.transmissionId.toString(),
        //     timestamp = transmission.timestamp.toString(),
        //     status = "SENT",
        //     receiverParticipantId = receiverParticipantId,
        //     documentId = documentId
        // )

        throw NotImplementedError("Peppol AS4 transmission not yet configured - requires Oxalis or similar Peppol access point library")
    }

    override suspend fun getTransmissionStatus(messageId: String): PeppolTransmissionStatus {
        logger.info("Checking transmission status: messageId=$messageId")

        // TODO: Implement status checking
        // This typically involves:
        // 1. Checking local transmission log
        // 2. Processing receipt acknowledgments
        // 3. Handling error notifications

        throw NotImplementedError("Transmission status tracking not yet implemented")
    }

    override suspend fun lookupParticipant(participantId: String): Boolean {
        logger.info("Looking up participant in Peppol SML: $participantId")

        // TODO: Implement SMP lookup
        // Steps:
        // 1. Query SML (Service Metadata Locator) for participant's SMP
        // 2. Query SMP (Service Metadata Publisher) for participant capabilities
        // 3. Return true if participant is registered
        //
        // val smlClient = PeppolSmlClient()
        // val smpUrl = smlClient.lookup(participantId)
        // return smpUrl != null

        throw NotImplementedError("Peppol SML/SMP lookup not yet implemented")
    }

    override suspend fun getParticipantCapabilities(participantId: String): List<String> {
        logger.info("Getting capabilities for participant: $participantId")

        // TODO: Implement capability lookup
        // Query SMP to get list of document types the participant can receive
        //
        // val smpClient = PeppolSmpClient()
        // val capabilities = smpClient.getCapabilities(participantId)
        // return capabilities.documentTypes

        throw NotImplementedError("Participant capability lookup not yet implemented")
    }
}

/**
 * Mock Peppol Access Point for testing
 * Simulates Peppol network without actual transmission
 */
class MockPeppolAccessPoint : PeppolAccessPoint {
    private val logger = LoggerFactory.getLogger(MockPeppolAccessPoint::class.java)
    private val transmissions = mutableMapOf<String, PeppolTransmissionResult>()
    private val statuses = mutableMapOf<String, PeppolTransmissionStatus>()
    private var messageCounter = 0

    override suspend fun sendInvoice(
        ublXml: String,
        senderParticipantId: String,
        receiverParticipantId: String,
        documentId: String
    ): PeppolTransmissionResult {
        logger.info("MOCK: Sending invoice - sender=$senderParticipantId, receiver=$receiverParticipantId")

        val messageId = "MSG-MOCK-${++messageCounter}"

        val result = PeppolTransmissionResult(
            messageId = messageId,
            timestamp = Instant.now().toString(),
            status = "SENT",
            receiverParticipantId = receiverParticipantId,
            documentId = documentId
        )

        transmissions[messageId] = result
        statuses[messageId] = PeppolTransmissionStatus.SENT

        // Simulate automatic delivery after 1 second
        // In real implementation, this would be async via webhook
        statuses[messageId] = PeppolTransmissionStatus.DELIVERED

        return result
    }

    override suspend fun getTransmissionStatus(messageId: String): PeppolTransmissionStatus {
        return statuses[messageId] ?: PeppolTransmissionStatus.FAILED
    }

    override suspend fun lookupParticipant(participantId: String): Boolean {
        logger.info("MOCK: Looking up participant: $participantId")
        // Always return true for mock
        return true
    }

    override suspend fun getParticipantCapabilities(participantId: String): List<String> {
        logger.info("MOCK: Getting capabilities for: $participantId")
        // Return standard Peppol invoice capabilities
        return listOf(
            OpenPeppolAccessPoint.INVOICE_DOCTYPE,
            OpenPeppolAccessPoint.CREDIT_NOTE_DOCTYPE
        )
    }

    /**
     * Get all transmissions (for testing)
     */
    fun getAllTransmissions(): Map<String, PeppolTransmissionResult> = transmissions.toMap()

    /**
     * Clear all data (for testing)
     */
    fun clear() {
        transmissions.clear()
        statuses.clear()
        messageCounter = 0
    }
}

/**
 * Peppol participant ID utilities
 */
object PeppolParticipantIdUtils {
    /**
     * Format a Belgian company number as Peppol participant ID
     * Example: BE0123456789 -> 0208:BE0123456789
     */
    fun formatBelgianCompanyNumber(companyNumber: String): String {
        val cleaned = companyNumber.replace(".", "").replace(" ", "")
        return if (cleaned.startsWith("BE")) {
            "0208:$cleaned"
        } else {
            "0208:BE$cleaned"
        }
    }

    /**
     * Validate Peppol participant ID format
     */
    fun isValidParticipantId(participantId: String): Boolean {
        // Format: scheme:identifier (e.g., 0208:BE0123456789)
        val parts = participantId.split(":")
        return parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()
    }

    /**
     * Extract scheme from participant ID
     */
    fun getScheme(participantId: String): String? {
        return participantId.split(":").firstOrNull()
    }

    /**
     * Extract identifier from participant ID
     */
    fun getIdentifier(participantId: String): String? {
        return participantId.split(":").getOrNull(1)
    }
}

/**
 * Peppol invoice service
 * High-level service for sending invoices via Peppol
 */
class PeppolInvoiceService(
    private val ublConverter: PeppolUblConverter,
    private val ublValidator: PeppolUblValidator,
    private val accessPoint: PeppolAccessPoint
) {
    private val logger = LoggerFactory.getLogger(PeppolInvoiceService::class.java)

    /**
     * Send an invoice via Peppol network
     * Handles UBL conversion, validation, and transmission
     */
    @OptIn(kotlin.uuid.ExperimentalUuidApi::class)
    suspend fun sendInvoice(
        invoice: ai.dokus.foundation.domain.model.Invoice,
        supplier: TenantInfo,
        customer: ai.dokus.foundation.domain.model.Client,
        lineItems: List<ai.dokus.foundation.domain.model.InvoiceItem>
    ): PeppolInvoiceSendResult {
        logger.info("Sending invoice ${invoice.invoiceNumber.value} via Peppol")

        // Step 1: Check if customer has Peppol ID
        val customerPeppolId = customer.peppolId
        if (customerPeppolId == null) {
            return PeppolInvoiceSendResult(
                success = false,
                error = "Customer does not have a Peppol ID registered"
            )
        }

        val supplierPeppolId = supplier.peppolId
        if (supplierPeppolId == null) {
            return PeppolInvoiceSendResult(
                success = false,
                error = "Supplier does not have a Peppol ID registered"
            )
        }

        // Step 2: Validate Peppol IDs
        if (!PeppolParticipantIdUtils.isValidParticipantId(customerPeppolId)) {
            return PeppolInvoiceSendResult(
                success = false,
                error = "Invalid customer Peppol ID format: $customerPeppolId"
            )
        }

        // Step 3: Lookup receiver in Peppol network
        val receiverExists = try {
            accessPoint.lookupParticipant(customerPeppolId)
        } catch (e: NotImplementedError) {
            logger.warn("Peppol lookup not implemented, skipping check")
            true // Skip check if not implemented
        }

        if (!receiverExists) {
            return PeppolInvoiceSendResult(
                success = false,
                error = "Receiver $customerPeppolId not found in Peppol network"
            )
        }

        // Step 4: Convert to UBL 2.1 XML
        val ublXml = try {
            ublConverter.convertToUbl(invoice, supplier, customer, lineItems)
        } catch (e: Exception) {
            logger.error("Failed to convert invoice to UBL", e)
            return PeppolInvoiceSendResult(
                success = false,
                error = "UBL conversion failed: ${e.message}"
            )
        }

        // Step 5: Validate UBL
        val validationResult = ublValidator.validate(ublXml)
        if (!validationResult.isValid) {
            logger.warn("UBL validation failed: ${validationResult.errors}")
            return PeppolInvoiceSendResult(
                success = false,
                error = "UBL validation failed: ${validationResult.errors.joinToString(", ")}",
                validationErrors = validationResult.errors,
                validationWarnings = validationResult.warnings
            )
        }

        // Step 6: Send via Peppol network
        val transmissionResult = try {
            accessPoint.sendInvoice(
                ublXml = ublXml,
                senderParticipantId = supplierPeppolId,
                receiverParticipantId = customerPeppolId,
                documentId = invoice.invoiceNumber.value
            )
        } catch (e: NotImplementedError) {
            logger.warn("Peppol transmission not implemented yet")
            return PeppolInvoiceSendResult(
                success = false,
                error = "Peppol transmission not yet configured: ${e.message}"
            )
        } catch (e: Exception) {
            logger.error("Peppol transmission failed", e)
            return PeppolInvoiceSendResult(
                success = false,
                error = "Transmission failed: ${e.message}"
            )
        }

        logger.info("Invoice ${invoice.invoiceNumber.value} sent successfully via Peppol: ${transmissionResult.messageId}")

        return PeppolInvoiceSendResult(
            success = true,
            messageId = transmissionResult.messageId,
            timestamp = transmissionResult.timestamp,
            ublXml = ublXml,
            validationWarnings = validationResult.warnings
        )
    }
}

/**
 * Result of sending an invoice via Peppol
 */
data class PeppolInvoiceSendResult(
    val success: Boolean,
    val messageId: String? = null,
    val timestamp: String? = null,
    val ublXml: String? = null,
    val error: String? = null,
    val validationErrors: List<String> = emptyList(),
    val validationWarnings: List<String> = emptyList()
)
