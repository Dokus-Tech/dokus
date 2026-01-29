package tech.dokus.features.ai.graph.nodes

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase
import tech.dokus.domain.enums.TenantType
import tech.dokus.domain.model.Tenant

internal interface InputWithTenantContext {
    val tenant: Tenant
}

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.tenantContextInjectorNode(
    tenant: Tenant,
): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input> { args ->
        llm.writeSession {
            appendPrompt {
                user {
                    text(tenant.prompt)
                }
            }
        }
        args
    }
}

internal inline fun <reified Input : InputWithTenantContext> AIAgentSubgraphBuilderBase<*, *>.tenantContextInjectorNode(): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input> { args ->
        llm.writeSession {
            appendPrompt {
                user {
                    text(args.tenant.prompt)
                }
            }
        }
        args
    }
}

private val Tenant.prompt
    get() = """
    ## TENANT CONTEXT
    
    You are processing documents for this business:
    
    **Company:** ${legalName.value}
    **VAT Number:** ${vatNumber.value}
    **Language:** ${language.code} (prefer this for field names if ambiguous)
    **Type:** ${type.description}
    
    ## HOW TO USE THIS CONTEXT
    
    **For CLASSIFICATION:**
    - If document header shows "${legalName.value}" or VAT "${vatNumber.value}" as SELLER → INVOICE (issued by tenant)
    - If document shows "${legalName.value}" or VAT "${vatNumber.value}" as BUYER/CLIENT → BILL (received from supplier)
    - If document is addressed TO "${legalName.value}" → incoming document (Bill, Reminder, Statement, etc.)
    - If document is FROM "${legalName.value}" → outgoing document (Invoice, Quote, Credit Note, etc.)
    
    **For EXTRACTION:**
    - Tenant details should match: "${legalName.value}", VAT: ${vatNumber.value}
    - If tenant is SELLER: extract customer/buyer details as counterparty
    - If tenant is BUYER: extract supplier/vendor details as counterparty
    - Prefer ${language.code} field labels when document is multilingual
    
    **VAT Number Matching:**
    - Belgian format: BE 0XXX.XXX.XXX or BE0XXXXXXXXX
    - "${vatNumber.value}" belongs to THIS tenant
    - Any OTHER VAT number is a counterparty (customer or supplier)
    """.trimIndent()

private val TenantType.description: String
    get() = when (this) {
        TenantType.Freelancer -> "Freelancer / Sole proprietor (zelfstandige/indépendant) — legal name is person's name"
        TenantType.Company -> "Company (BV, NV, VOF, etc.) — separate legal entity"
    }