package tech.dokus.features.ai.graph.nodes

import ai.koog.agents.core.dsl.builder.AIAgentNodeDelegate
import ai.koog.agents.core.dsl.builder.AIAgentSubgraphBuilderBase

internal interface InputWithUserFeedback {
    val userFeedback: String? get() = null
}

internal inline fun <reified Input> AIAgentSubgraphBuilderBase<*, *>.userFeedbackInjectorNode(
    feedback: String?
): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input> { args ->
        llm.writeSession {
            appendPrompt {
                if (!feedback.isNullOrBlank()) {
                    user {
                        text(buildUserFeedbackPrompt(feedback))
                    }
                }
            }
        }
        args
    }
}

internal inline fun <reified Input : InputWithUserFeedback> AIAgentSubgraphBuilderBase<*, *>.userFeedbackInjectorNode(): AIAgentNodeDelegate<Input, Input> {
    return node<Input, Input>("inject-user-feedback") { args ->
        llm.writeSession {
            appendPrompt {
                args.userFeedback?.takeIf { it.isNotEmpty() }?.let {
                    user {
                        text(buildUserFeedbackPrompt(it))
                    }
                }
            }
        }
        args
    }
}

private fun buildUserFeedbackPrompt(feedback: String): String = buildString {
    appendLine("## USER CORRECTION")
    appendLine()
    appendLine("The user flagged the previous extraction as incorrect:")
    appendLine(feedback)
    appendLine()
    appendLine("Pay special attention to this feedback and correct accordingly.")
}.trim()