package tech.dokus.features.ai.config

import ai.koog.agents.core.agent.GraphAIAgent.FeatureContext
import ai.koog.agents.features.eventHandler.feature.handleEvents
import tech.dokus.foundation.backend.utils.loggerFor

private val logger = loggerFor("KoogEventLogging")

internal fun FeatureContext.installKoogEventLogging(
    agentName: String,
    enabled: Boolean,
) {
    if (!enabled) return

    handleEvents {
        onAgentStarting { event ->
            logger.debug(
                "Koog agent starting: agent={}, runId={}, agentId={}",
                agentName,
                event.runId,
                event.agent.id
            )
        }
        onLLMCallStarting { event ->
            logger.debug(
                "Koog LLM call starting: agent={}, runId={}, model={}, messages={}, tools={}",
                agentName,
                event.runId,
                event.model.id,
                event.prompt.messages.size,
                event.tools.size
            )
        }
        onLLMCallCompleted { event ->
            logger.debug(
                "Koog LLM call completed: agent={}, runId={}, model={}, responses={}",
                agentName,
                event.runId,
                event.model.id,
                event.responses.size
            )
        }
        onToolCallStarting { event ->
            logger.debug(
                "Koog tool call starting: agent={}, runId={}, tool={}, args={}",
                agentName,
                event.runId,
                event.toolName,
                event.toolArgs
            )
        }
        onToolValidationFailed { event ->
            logger.warn(
                "Koog tool validation failed: agent={}, runId={}, tool={}, message={}",
                agentName,
                event.runId,
                event.toolName,
                event.message
            )
        }
        onToolCallFailed { event ->
            logger.warn(
                "Koog tool call failed: agent={}, runId={}, tool={}, message={}",
                agentName,
                event.runId,
                event.toolName,
                event.message
            )
        }
        onToolCallCompleted { event ->
            logger.debug(
                "Koog tool call completed: agent={}, runId={}, tool={}",
                agentName,
                event.runId,
                event.toolName
            )
        }
        onAgentCompleted { event ->
            logger.debug(
                "Koog agent completed: agent={}, runId={}, agentId={}",
                agentName,
                event.runId,
                event.agentId
            )
        }
        onAgentExecutionFailed { event ->
            logger.warn(
                "Koog agent failed: agent={}, runId={}, agentId={}, error={}",
                agentName,
                event.runId,
                event.agentId,
                event.throwable.message ?: event.throwable::class.simpleName ?: "unknown"
            )
        }
    }
}
