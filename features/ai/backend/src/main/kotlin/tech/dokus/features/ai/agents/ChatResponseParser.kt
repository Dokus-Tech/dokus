package tech.dokus.features.ai.agents

import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import tech.dokus.domain.model.ai.ChatContentBlock
import tech.dokus.domain.model.ai.DocumentReferenceDto
import tech.dokus.domain.model.ai.InvoiceLineDto
import tech.dokus.domain.model.ai.SummaryRowDto
import tech.dokus.domain.model.ai.TransactionReferenceDto

/**
 * Parses LLM text output into structured [ChatContentBlock] list.
 *
 * The LLM is instructed to embed structured data using XML-like tags:
 * - `<summary>[{"label":"...","value":"..."},...]</summary>`
 * - `<documents>[{"name":"...","ref":"...","type":"...","amount":0.0},...]</documents>`
 * - `<invoice>{"name":"...","ref":"...","date":"...","lines":[...],"total":"..."}</invoice>`
 * - `<transactions>[{"description":"...","amount":0.0,"status":"..."},...]</transactions>`
 *
 * Text outside tags becomes [ChatContentBlock.Text] blocks.
 * Falls back to a single Text block if no tags are found.
 */
object ChatResponseParser {

    private val logger = LoggerFactory.getLogger(ChatResponseParser::class.java)
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val TAG_PATTERN = Regex("<(summary|documents|invoice|transactions)>(.*?)</\\1>", RegexOption.DOT_MATCHES_ALL)

    fun parse(rawAnswer: String): List<ChatContentBlock> {
        val matches = TAG_PATTERN.findAll(rawAnswer).toList()
        if (matches.isEmpty()) {
            return listOf(ChatContentBlock.Text(rawAnswer.trim()))
        }

        val blocks = mutableListOf<ChatContentBlock>()
        var lastEnd = 0

        for (match in matches) {
            // Text before this tag
            val textBefore = rawAnswer.substring(lastEnd, match.range.first).trim()
            if (textBefore.isNotEmpty()) {
                blocks.add(ChatContentBlock.Text(textBefore))
            }

            val tagName = match.groupValues[1]
            val content = match.groupValues[2].trim()

            val block = parseTagContent(tagName, content)
            if (block != null) {
                blocks.add(block)
            }

            lastEnd = match.range.last + 1
        }

        // Text after last tag
        val textAfter = rawAnswer.substring(lastEnd).trim()
        if (textAfter.isNotEmpty()) {
            blocks.add(ChatContentBlock.Text(textAfter))
        }

        return blocks
    }

    private fun parseTagContent(tagName: String, content: String): ChatContentBlock? {
        return try {
            when (tagName) {
                "summary" -> {
                    val rows = json.decodeFromString<List<SummaryRowDto>>(content)
                    ChatContentBlock.Summary(rows)
                }
                "documents" -> {
                    val items = json.decodeFromString<List<DocumentReferenceDto>>(content)
                    ChatContentBlock.Documents(items, showDownloadAll = items.size > 1)
                }
                "invoice" -> {
                    json.decodeFromString<ChatContentBlock.InvoiceDetail>(content)
                }
                "transactions" -> {
                    val items = json.decodeFromString<List<TransactionReferenceDto>>(content)
                    ChatContentBlock.Transactions(items)
                }
                else -> null
            }
        } catch (e: Exception) {
            logger.warn("Failed to parse <{}> block: {}", tagName, e.message)
            // Return the raw content as text if parsing fails
            ChatContentBlock.Text("<$tagName>$content</$tagName>")
        }
    }
}
