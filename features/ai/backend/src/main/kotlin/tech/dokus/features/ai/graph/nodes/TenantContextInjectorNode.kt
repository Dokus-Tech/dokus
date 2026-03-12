package tech.dokus.features.ai.graph.nodes

import ai.koog.agents.core.agent.entity.AIAgentStorageKey
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
                    text(buildClassificationTenantPrompt(tenant, emptyList()))
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
                    text(buildClassificationTenantPrompt(args.tenant, args.associatedPersonNames))
                }
            }
        }
        args
    }
}

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.extractionTenantContextInjectorNode(
    tenantKey: AIAgentStorageKey<Tenant>,
    associatedNamesKey: AIAgentStorageKey<List<String>>,
): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input>("inject-extraction-tenant-context") { args ->
        val tenant = storage.getValue(tenantKey)
        val names = storage.getValue(associatedNamesKey)
        llm.writeSession {
            appendPrompt {
                user {
                    text(buildExtractionTenantPrompt(tenant, names))
                }
            }
        }
        args
    }
}

private fun buildClassificationTenantPrompt(tenant: Tenant, associatedPersonNames: List<String>): String = with(tenant) {
    buildString {
        appendLine("## TENANT CONTEXT")
        appendLine()
        appendLine("You are processing documents for this business:")
        appendLine()
        appendLine("**Company:** ${legalName.value}")
        appendLine("**Language:** ${language.code} (prefer this for field names if ambiguous)")
        appendLine("**Type:** ${type.description}")
        if (associatedPersonNames.isNotEmpty()) {
            appendLine("**Associated persons:** ${associatedPersonNames.joinToString(", ")}")
            appendLine("These people are part of \"${legalName.value}\". If any of them appear in the buyer/client section, the document is addressed TO the tenant.")
        }
        appendLine()
        appendLine("## HOW TO USE THIS CONTEXT")
        appendLine()
        appendLine("- Classify by legal document nature (Invoice/CreditNote/Receipt/etc.), not business direction.")
        appendLine("- If the paper is an invoice, classify as INVOICE regardless of seller/buyer perspective.")
        appendLine("- Incoming/outgoing direction is resolved later using seller/buyer facts and tenant matching.")
    }.trim()
}

private fun buildExtractionTenantPrompt(tenant: Tenant, associatedPersonNames: List<String>): String = with(tenant) {
    buildString {
        appendLine("## TENANT CONTEXT FOR EXTRACTION")
        appendLine()
        appendLine("**Company:** ${legalName.value}")
        appendLine("**Language:** ${language.code} (prefer this for field names if ambiguous)")
        appendLine("**Type:** ${type.description}")
        if (associatedPersonNames.isNotEmpty()) {
            appendLine("**Associated persons:** ${associatedPersonNames.joinToString(", ")}")
        }
        appendLine()
        appendLine("## EXTRACTION RULES")
        appendLine()
        appendLine("- \"${legalName.value}\" is the tenant — the business that owns this document.")
        appendLine("- Extract seller/issuer and buyer/recipient as neutral facts from the document.")
        appendLine("- Do not swap roles based on assumptions; direction is resolved in deterministic code.")
        appendLine("- Prefer ${language.code} field labels when document is multilingual.")
        appendLine()
        appendLine("**Counterparty Rule:**")
        appendLine("- \"${legalName.value}\" is YOUR tenant — never the counterparty.")
        appendLine("- counterpartyName must NEVER be \"${legalName.value}\" or match VAT ${vatNumber.normalized}.")
        appendLine("- Counterparty = the OTHER party in the transaction, not the tenant.")
        appendLine()
        appendLine("**Name Matching:**")
        appendLine("- Company names on documents may differ from the legal name: different casing, dots instead of spaces, domain names, abbreviations.")
        appendLine("- Match by similarity — do NOT require an exact string match.")
    }.trim()
}

private val TenantType.description: String
    get() = when (this) {
        TenantType.Freelancer -> "Freelancer / Sole proprietor (zelfstandige/indépendant) — legal name is person's name"
        TenantType.Company -> "Company (BV, NV, VOF, etc.) — separate legal entity"
    }
