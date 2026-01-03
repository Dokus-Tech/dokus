package tech.dokus.database.columns

import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.ColumnType
import org.jetbrains.exposed.v1.core.Table
import org.postgresql.util.PGobject

/**
 * Custom Exposed column type for PostgreSQL pgvector extension.
 *
 * Supports storing and retrieving vector embeddings for similarity search.
 * The vector dimensions must match the embedding model being used:
 * - Ollama nomic-embed-text: 768 dimensions
 * - OpenAI text-embedding-3-small: 1536 dimensions
 *
 * Usage:
 * ```kotlin
 * object DocumentChunksTable : UUIDTable("document_chunks") {
 *     val embedding = vector("embedding", 768)  // 768 for Ollama
 * }
 * ```
 *
 * Requires pgvector extension to be installed in PostgreSQL:
 * ```sql
 * CREATE EXTENSION IF NOT EXISTS vector;
 * ```
 *
 * @param dimensions The number of dimensions in the vector (must match embedding model)
 */
class VectorColumnType(private val dimensions: Int) : ColumnType<List<Float>>() {

    init {
        require(dimensions > 0) { "Vector dimensions must be positive, got $dimensions" }
    }

    override fun sqlType(): String = "vector($dimensions)"

    override fun valueFromDB(value: Any): List<Float> {
        return when (value) {
            is PGobject -> parseVectorString(value.value ?: "[]")
            is String -> parseVectorString(value)
            is List<*> -> {
                @Suppress("UNCHECKED_CAST")
                value as List<Float>
            }
            else -> throw IllegalArgumentException(
                "Unexpected value type for vector column: ${value::class.simpleName}"
            )
        }
    }

    override fun notNullValueToDB(value: List<Float>): Any {
        require(value.size == dimensions) {
            "Vector size mismatch: expected $dimensions dimensions, got ${value.size}"
        }
        return PGobject().apply {
            type = "vector"
            this.value = formatVectorString(value)
        }
    }

    override fun nonNullValueToString(value: List<Float>): String {
        return "'${formatVectorString(value)}'"
    }

    /**
     * Parse PostgreSQL vector format `[1.0,2.0,3.0]` to List<Float>
     */
    private fun parseVectorString(str: String): List<Float> {
        if (str.isBlank() || str == "[]") {
            return emptyList()
        }
        return str
            .trim('[', ']')
            .split(',')
            .map { it.trim().toFloat() }
    }

    /**
     * Format List<Float> to PostgreSQL vector format `[1.0,2.0,3.0]`
     */
    private fun formatVectorString(vector: List<Float>): String {
        return "[${vector.joinToString(",")}]"
    }
}

/**
 * Creates a vector column for storing embeddings with pgvector.
 *
 * @param name The column name
 * @param dimensions The number of dimensions in the vector
 * @return Column definition for use in Exposed table
 */
fun Table.vector(name: String, dimensions: Int): Column<List<Float>> {
    return registerColumn(name, VectorColumnType(dimensions))
}

/**
 * Common embedding dimensions for popular models
 */
object EmbeddingDimensions {
    /** Ollama nomic-embed-text model */
    const val OLLAMA_NOMIC_EMBED_TEXT = 768

    /** OpenAI text-embedding-3-small model */
    const val OPENAI_TEXT_EMBEDDING_3_SMALL = 1536

    /** OpenAI text-embedding-3-large model */
    const val OPENAI_TEXT_EMBEDDING_3_LARGE = 3072

    /** OpenAI text-embedding-ada-002 (legacy) */
    const val OPENAI_ADA_002 = 1536
}
