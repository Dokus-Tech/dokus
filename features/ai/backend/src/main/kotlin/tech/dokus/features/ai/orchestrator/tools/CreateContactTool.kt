package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

/**
 * Tool for creating new contacts from document extraction.
 *
 * Creates a new contact record from vendor/supplier information extracted from documents.
 * Should only be used after verifying:
 * 1. No existing contact with this VAT number (via lookup_contact)
 * 2. Vendor is verified (via lookup_company for Belgian companies)
 * 3. Extraction confidence is above threshold
 */
class CreateContactTool(
    private val contactCreator: suspend (
        tenantId: String,
        name: String,
        vatNumber: String?,
        address: String?
    ) -> CreateResult
) : SimpleTool<CreateContactTool.Args>(
    argsSerializer = Args.serializer(),
    name = "create_contact",
    description = """
        Creates a new contact from document vendor information.

        Creates a contact record with name, VAT number, and address.

        Prerequisites before calling:
        1. Check no existing contact exists (use lookup_contact first)
        2. For Belgian vendors, verify the company exists (use lookup_company)
        3. Ensure extraction confidence is above 85%

        Returns the created contact ID.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription("Contact name (company/vendor name)")
        val name: String,

        @property:LLMDescription("VAT number (optional but recommended)")
        val vatNumber: String? = null,

        @property:LLMDescription("Address (optional)")
        val address: String? = null
    )

    /**
     * Result of contact creation.
     */
    data class CreateResult(
        val success: Boolean,
        val contactId: String?,
        val error: String?
    )

    override suspend fun execute(args: Args): String {
        if (args.name.isBlank()) {
            return "ERROR: Contact name is required"
        }

        return try {
            val result = contactCreator(
                args.tenantId,
                args.name,
                args.vatNumber?.takeIf { it.isNotBlank() },
                args.address?.takeIf { it.isNotBlank() }
            )

            if (result.success && result.contactId != null) {
                buildString {
                    appendLine("SUCCESS: Created contact ${result.contactId}")
                    appendLine("Name: ${args.name}")
                    if (args.vatNumber != null) {
                        appendLine("VAT: ${args.vatNumber}")
                    }
                    if (args.address != null) {
                        appendLine("Address: ${args.address}")
                    }
                }
            } else {
                "ERROR: Failed to create contact: ${result.error ?: "Unknown error"}"
            }
        } catch (e: Exception) {
            "ERROR: Failed to create contact: ${e.message}"
        }
    }
}
