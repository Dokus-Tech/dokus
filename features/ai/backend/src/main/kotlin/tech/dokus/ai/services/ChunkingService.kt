package tech.dokus.ai.services

import tech.dokus.domain.model.ChunkMetadata
import tech.dokus.domain.model.ChunkProvenance
import tech.dokus.domain.model.ChunkingConfig
import tech.dokus.domain.model.TextOffsets
import org.slf4j.LoggerFactory

/**
 * Service responsible for chunking document text into segments suitable for RAG.
 *
 * Provides two main chunking strategies:
 * 1. **Semantic Chunking**: Respects sentence and paragraph boundaries for more coherent chunks
 * 2. **Fixed Chunking**: Simple fixed-size chunks with overlap for predictable sizing
 *
 * The semantic strategy is preferred as it produces more meaningful chunks for retrieval,
 * but fixed chunking can be used as a fallback for documents with unusual formatting.
 */
class ChunkingService {
    private val logger = LoggerFactory.getLogger(ChunkingService::class.java)

    companion object {
        /** Default configuration for chunking */
        val DEFAULT_CONFIG = ChunkingConfig(
            targetChunkSize = 500,
            maxChunkSize = 1000,
            overlapSize = 50,
            minChunkSize = 100,
            splitOnSentences = true,
            splitOnParagraphs = true
        )

        // Regex patterns for text splitting
        private val PARAGRAPH_PATTERN = Regex("\\n{2,}")
        private val SENTENCE_PATTERN = Regex("(?<=[.!?])\\s+")
        private val WHITESPACE_PATTERN = Regex("\\s+")

        // Additional patterns for metadata detection
        private val AMOUNT_PATTERN = Regex("\\$?\\d+[,.]\\d{2}")
        private val TABLE_LINE_PATTERN = Regex("[-=]{3,}")
    }

    /**
     * Chunking strategy type.
     */
    enum class ChunkingStrategy {
        /** Respects sentence and paragraph boundaries */
        SEMANTIC,
        /** Fixed-size chunks with overlap */
        FIXED
    }

    /**
     * A chunk result containing text and provenance information.
     */
    data class Chunk(
        /** The text content of this chunk */
        val content: String,
        /** Index of this chunk within the document (0-indexed) */
        val index: Int,
        /** Provenance information linking back to source */
        val provenance: ChunkProvenance,
        /** Additional metadata about the chunk */
        val metadata: ChunkMetadata? = null,
        /** Estimated token count (rough approximation: chars / 4) */
        val estimatedTokens: Int = content.length / 4
    )

    /**
     * Result of chunking a document.
     */
    data class ChunkingResult(
        /** List of chunks in order */
        val chunks: List<Chunk>,
        /** Total number of chunks */
        val totalChunks: Int,
        /** Strategy used for chunking */
        val strategy: ChunkingStrategy,
        /** Configuration used */
        val config: ChunkingConfig
    )

    /**
     * Chunk a document using the default semantic strategy.
     *
     * @param text The document text to chunk
     * @param config Configuration for chunking (defaults to DEFAULT_CONFIG)
     * @return ChunkingResult containing the chunks with provenance
     */
    fun chunk(
        text: String,
        config: ChunkingConfig = DEFAULT_CONFIG
    ): ChunkingResult {
        return chunkSemantic(text, config)
    }

    /**
     * Chunk a document using semantic boundaries (paragraphs and sentences).
     *
     * This strategy tries to create coherent chunks by:
     * 1. First splitting on paragraph boundaries
     * 2. Then splitting long paragraphs on sentence boundaries
     * 3. Merging small chunks together up to the target size
     *
     * @param text The document text to chunk
     * @param config Configuration for chunking
     * @return ChunkingResult with semantic chunks
     */
    fun chunkSemantic(
        text: String,
        config: ChunkingConfig = DEFAULT_CONFIG
    ): ChunkingResult {
        logger.debug("Semantic chunking document (${text.length} chars)")

        if (text.isBlank()) {
            logger.warn("Empty document provided for chunking")
            return ChunkingResult(
                chunks = emptyList(),
                totalChunks = 0,
                strategy = ChunkingStrategy.SEMANTIC,
                config = config
            )
        }

        val normalizedText = normalizeText(text)
        val segments = mutableListOf<TextSegment>()

        // Step 1: Split on paragraph boundaries
        if (config.splitOnParagraphs) {
            var currentOffset = 0
            PARAGRAPH_PATTERN.split(normalizedText).forEach { paragraph ->
                val trimmed = paragraph.trim()
                if (trimmed.isNotEmpty()) {
                    val startOffset = normalizedText.indexOf(trimmed, currentOffset)
                    segments.add(TextSegment(
                        text = trimmed,
                        startOffset = startOffset,
                        endOffset = startOffset + trimmed.length
                    ))
                    currentOffset = startOffset + trimmed.length
                }
            }
        } else {
            segments.add(TextSegment(
                text = normalizedText,
                startOffset = 0,
                endOffset = normalizedText.length
            ))
        }

        // Step 2: Split long paragraphs on sentence boundaries
        val splitSegments = mutableListOf<TextSegment>()
        for (segment in segments) {
            if (segment.text.length > config.maxChunkSize && config.splitOnSentences) {
                splitSegments.addAll(splitOnSentences(segment, config))
            } else {
                splitSegments.add(segment)
            }
        }

        // Step 3: Merge small segments up to target size
        val mergedSegments = mergeSmallSegments(splitSegments, config)

        // Step 4: Final split for segments still over max size
        val finalSegments = mutableListOf<TextSegment>()
        for (segment in mergedSegments) {
            if (segment.text.length > config.maxChunkSize) {
                finalSegments.addAll(splitBySize(segment, config))
            } else {
                finalSegments.add(segment)
            }
        }

        // Convert to Chunk objects with provenance
        val chunks = finalSegments.mapIndexed { index, segment ->
            Chunk(
                content = segment.text,
                index = index,
                provenance = ChunkProvenance(
                    offsets = TextOffsets(
                        start = segment.startOffset,
                        end = segment.endOffset
                    ),
                    pageNumber = null // Page detection would require document structure
                ),
                metadata = detectChunkMetadata(segment.text),
                estimatedTokens = segment.text.length / 4
            )
        }

        logger.debug("Created ${chunks.size} semantic chunks")

        return ChunkingResult(
            chunks = chunks,
            totalChunks = chunks.size,
            strategy = ChunkingStrategy.SEMANTIC,
            config = config
        )
    }

    /**
     * Chunk a document using fixed-size windows with overlap.
     *
     * This strategy creates chunks of approximately equal size with overlap
     * between consecutive chunks to maintain context across boundaries.
     *
     * @param text The document text to chunk
     * @param config Configuration for chunking
     * @return ChunkingResult with fixed-size chunks
     */
    fun chunkFixed(
        text: String,
        config: ChunkingConfig = DEFAULT_CONFIG
    ): ChunkingResult {
        logger.debug("Fixed chunking document (${text.length} chars)")

        if (text.isBlank()) {
            logger.warn("Empty document provided for chunking")
            return ChunkingResult(
                chunks = emptyList(),
                totalChunks = 0,
                strategy = ChunkingStrategy.FIXED,
                config = config
            )
        }

        val normalizedText = normalizeText(text)
        val chunks = mutableListOf<Chunk>()
        var startOffset = 0
        var chunkIndex = 0

        while (startOffset < normalizedText.length) {
            // Calculate end offset
            var endOffset = minOf(startOffset + config.targetChunkSize, normalizedText.length)

            // Try to end at a word boundary if not at the document end
            if (endOffset < normalizedText.length) {
                val searchEnd = minOf(endOffset + 50, normalizedText.length) // Look ahead up to 50 chars
                val spaceIndex = normalizedText.indexOf(' ', endOffset)
                if (spaceIndex in endOffset until searchEnd) {
                    endOffset = spaceIndex
                }
            }

            val chunkText = normalizedText.substring(startOffset, endOffset).trim()

            // Skip if chunk is too small (except for the last chunk)
            if (chunkText.length >= config.minChunkSize || startOffset + config.targetChunkSize >= normalizedText.length) {
                chunks.add(Chunk(
                    content = chunkText,
                    index = chunkIndex,
                    provenance = ChunkProvenance(
                        offsets = TextOffsets(
                            start = startOffset,
                            end = endOffset
                        ),
                        pageNumber = null
                    ),
                    metadata = detectChunkMetadata(chunkText),
                    estimatedTokens = chunkText.length / 4
                ))
                chunkIndex++
            }

            // Move to next chunk position with overlap
            startOffset = endOffset - config.overlapSize
            if (startOffset >= normalizedText.length) break

            // Prevent infinite loop
            if (endOffset == normalizedText.length) break
        }

        logger.debug("Created ${chunks.size} fixed-size chunks")

        return ChunkingResult(
            chunks = chunks,
            totalChunks = chunks.size,
            strategy = ChunkingStrategy.FIXED,
            config = config
        )
    }

    // =========================================================================
    // Private Helper Methods
    // =========================================================================

    /**
     * Internal representation of a text segment with offsets.
     */
    private data class TextSegment(
        val text: String,
        val startOffset: Int,
        val endOffset: Int
    )

    /**
     * Normalize text by collapsing multiple whitespaces and trimming.
     */
    private fun normalizeText(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .trim()
    }

    /**
     * Split a segment on sentence boundaries.
     */
    private fun splitOnSentences(segment: TextSegment, config: ChunkingConfig): List<TextSegment> {
        val sentences = SENTENCE_PATTERN.split(segment.text)
        val result = mutableListOf<TextSegment>()
        var currentOffset = segment.startOffset

        for (sentence in sentences) {
            val trimmed = sentence.trim()
            if (trimmed.isNotEmpty()) {
                val relativeStart = segment.text.indexOf(trimmed, currentOffset - segment.startOffset)
                val absoluteStart = segment.startOffset + maxOf(0, relativeStart)

                result.add(TextSegment(
                    text = trimmed,
                    startOffset = absoluteStart,
                    endOffset = absoluteStart + trimmed.length
                ))
                currentOffset = absoluteStart + trimmed.length
            }
        }

        return result
    }

    /**
     * Merge small consecutive segments up to the target chunk size.
     */
    private fun mergeSmallSegments(segments: List<TextSegment>, config: ChunkingConfig): List<TextSegment> {
        if (segments.isEmpty()) return emptyList()

        val result = mutableListOf<TextSegment>()
        var currentText = StringBuilder()
        var currentStart = segments.first().startOffset
        var currentEnd = segments.first().startOffset

        for (segment in segments) {
            val wouldBeLength = currentText.length + (if (currentText.isNotEmpty()) 1 else 0) + segment.text.length

            if (wouldBeLength <= config.targetChunkSize) {
                // Merge with current chunk
                if (currentText.isNotEmpty()) {
                    currentText.append(" ")
                }
                currentText.append(segment.text)
                currentEnd = segment.endOffset
            } else {
                // Save current chunk and start new one
                if (currentText.isNotEmpty()) {
                    result.add(TextSegment(
                        text = currentText.toString(),
                        startOffset = currentStart,
                        endOffset = currentEnd
                    ))
                }
                currentText = StringBuilder(segment.text)
                currentStart = segment.startOffset
                currentEnd = segment.endOffset
            }
        }

        // Add the last chunk
        if (currentText.isNotEmpty()) {
            result.add(TextSegment(
                text = currentText.toString(),
                startOffset = currentStart,
                endOffset = currentEnd
            ))
        }

        return result
    }

    /**
     * Split a segment by size when it exceeds max chunk size.
     */
    private fun splitBySize(segment: TextSegment, config: ChunkingConfig): List<TextSegment> {
        val result = mutableListOf<TextSegment>()
        val text = segment.text
        var startIndex = 0

        while (startIndex < text.length) {
            var endIndex = minOf(startIndex + config.targetChunkSize, text.length)

            // Try to break at a word boundary
            if (endIndex < text.length) {
                val lastSpace = text.lastIndexOf(' ', endIndex)
                if (lastSpace > startIndex + config.minChunkSize) {
                    endIndex = lastSpace
                }
            }

            result.add(TextSegment(
                text = text.substring(startIndex, endIndex).trim(),
                startOffset = segment.startOffset + startIndex,
                endOffset = segment.startOffset + endIndex
            ))

            startIndex = endIndex
            // Skip whitespace at start of next chunk
            while (startIndex < text.length && text[startIndex].isWhitespace()) {
                startIndex++
            }
        }

        return result
    }

    /**
     * Detect metadata about a chunk's content.
     */
    private fun detectChunkMetadata(text: String): ChunkMetadata {
        // Simple heuristics for metadata detection
        val containsAmounts = AMOUNT_PATTERN.containsMatchIn(text)
        val containsTable = text.contains("\t") ||
            (text.count { it == '|' } > 2) ||
            TABLE_LINE_PATTERN.containsMatchIn(text)

        val chunkType = when {
            text.length < 100 && text.all { it.isUpperCase() || it.isWhitespace() || it.isDigit() || it in ".-:/" } -> "header"
            containsTable -> "table"
            text.startsWith("Total") || text.startsWith("Subtotal") || text.startsWith("Tax") -> "summary"
            else -> "body"
        }

        return ChunkMetadata(
            containsAmounts = containsAmounts,
            containsTable = containsTable,
            chunkType = chunkType
        )
    }
}
