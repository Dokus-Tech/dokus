package tech.dokus.features.ai.graph.nodes

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.model.Tenant

internal interface InputWithTenantContext {
    val tenant: Tenant
    val associatedPersonNames: List<String> get() = emptyList()
}

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.tenantContextInjectorNode(
    tenant: Tenant,
): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input> { args ->
        llm.writeSession {
            appendPrompt {
                user {
                    text(buildTenantPrompt(tenant, emptyList()))
                }
            }
        }
        args
    }
}

internal inline fun <reified Input : InputWithTenantContext> AIAgentSubgraphBuilderBase<*, *>.tenantContextInjectorNode(): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input>("inject-tenant-context") { args ->
        llm.writeSession {
            appendPrompt {
                user {
                    text(buildTenantPrompt(args.tenant, args.associatedPersonNames))
                }
            }
        }
        args
    }
}

private fun buildTenantPrompt(tenant: Tenant, associatedPersonNames: List<String>): String = with(tenant) {
    buildString {
        appendLine("## TENANT CONTEXT")
        appendLine()
        appendLine("You are processing documents for this business:")
        appendLine()
        appendLine("**Company:** ${legalName.value}")
        appendLine("**VAT Number:** ${vatNumber.value}")
        appendLine("**Language:** ${language.code} (prefer this for field names if ambiguous)")
        appendLine("**Type:** ${type.description}")
        if (associatedPersonNames.isNotEmpty()) {
            appendLine("**Associated persons:** ${associatedPersonNames.joinToString(", ")}")
            appendLine("These people are part of \"${legalName.value}\". If any of them appear in the buyer/client section, the document is addressed TO the tenant.")
        }
        appendLine()
        appendLine("## HOW TO USE THIS CONTEXT")
        appendLine()
        appendLine("**For CLASSIFICATION:**")
        appendLine("- Classify by legal document nature (Invoice/CreditNote/Receipt/etc.), not business direction.")
        appendLine("- If the paper is an invoice, classify as INVOICE regardless of seller/buyer perspective.")
        appendLine("- Incoming/outgoing direction is resolved later using seller/buyer facts and tenant matching.")
        appendLine()
        appendLine("**For EXTRACTION:**")
        appendLine("- Tenant details should match: \"${legalName.value}\", VAT: ${vatNumber.value}")
        appendLine("- Extract seller/issuer and buyer/recipient as neutral facts from the document.")
        appendLine("- Do not swap roles based on assumptions; direction is resolved in deterministic code.")
        appendLine("- Prefer ${language.code} field labels when document is multilingual")
        appendLine()
        appendLine("**Name Matching:**")
        appendLine("- Company names on documents may differ from the legal name: different casing, dots instead of spaces, domain names, abbreviations (e.g., \"Invoid.vision\" = \"Invoid Vision\").")
        appendLine("- Match by similarity — do NOT require an exact string match.")
        appendLine("- VAT number is the strongest identifier. If the VAT matches, the entity is the tenant regardless of name spelling.")
        appendLine()
        appendLine("**VAT Number Matching:**")
        appendLine("- Belgian format: BE 0XXX.XXX.XXX or BE0XXXXXXXXX")
        appendLine("- \"${vatNumber.value}\" belongs to THIS tenant")
        appendLine("- Any OTHER VAT number is a counterparty (customer or supplier)")
    }.trim()
}

private val TenantType.description: String
    get() = when (this) {
        TenantType.Freelancer -> "Freelancer / Sole proprietor (zelfstandige/indépendant) — legal name is person's name"
        TenantType.Company -> "Company (BV, NV, VOF, etc.) — separate legal entity"
    }
