package tech.dokus.features.ai.prompts

@JvmInline
value class Prompt(private val raw: String) {
    val value: String get() = raw.trimIndent()

    override fun toString(): String = value

    infix operator fun plus(other: Prompt): Prompt = Prompt(value + other.value)
    fun format(vararg args: Any?): Prompt = Prompt(value.format(*args))
}