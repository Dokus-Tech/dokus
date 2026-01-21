package tech.dokus.features.ai.orchestrator.tools

import ai.koog.agents.core.tools.SimpleTool
import ai.koog.agents.core.tools.annotations.LLMDescription
import kotlinx.serialization.Serializable

fun interface ContactLookupHandler {
    suspend operator fun invoke(tenantId: String, vatNumber: String): LookupContactTool.ContactInfo?
}

/**
 * Tool for looking up existing contacts by VAT number.
 *
 * Searches the tenant's contact database for a contact with the given VAT number.
 * Used to link documents to existing contacts rather than creating duplicates.
 */
class LookupContactTool(
    private val contactLookup: ContactLookupHandler
) : SimpleTool<LookupContactTool.Args>(
    argsSerializer = Args.serializer(),
    name = "lookup_contact",
    description = """
        Looks up an existing contact by VAT number.

        Searches the tenant's contact database for a contact with the given VAT number.
        Use this before creating a new contact to avoid duplicates.

        Returns contact details if found.
    """.trimIndent()
) {
    @Serializable
    data class Args(
        @property:LLMDescription("The tenant ID")
        val tenantId: String,

        @property:LLMDescription("VAT number to search for (e.g., BE0123456789)")
        val vatNumber: String
    )

    /**
     * Contact information returned from lookup.
     */
    data class ContactInfo(
        val id: String,
        val name: String,
        val vatNumber: String,
        val address: String?
    )

    override suspend fun execute(args: Args): String {
        if (args.vatNumber.isBlank()) {
            return "ERROR: VAT number is required for contact lookup"
        }

        return try {
            val contact = contactLookup(args.tenantId, args.vatNumber)

            if (contact != null) {
                buildString {
                    appendLine("SUCCESS: Found contact")
                    appendLine("ID: ${contact.id}")
                    appendLine("Name: ${contact.name}")
                    appendLine("VAT: ${contact.vatNumber}")
                    if (contact.address != null) {
                        appendLine("Address: ${contact.address}")
                    }
                }
            } else {
                "NOT_FOUND: No contact found with VAT number ${args.vatNumber}"
            }
        } catch (e: Exception) {
            "ERROR: Failed to lookup contact: ${e.message}"
        }
    }
}
