package com.mohammad.orion.domain.memory

sealed interface ExplicitMemoryCommand {
    data object NotCommand : ExplicitMemoryCommand
    data object Clarify : ExplicitMemoryCommand
    data object RejectedSecret : ExplicitMemoryCommand
    class Remember(val text: String) : ExplicitMemoryCommand { override fun toString() = "Remember([redacted])" }
    class ForgetTopic(val topic: String) : ExplicitMemoryCommand { override fun toString() = "ForgetTopic([redacted])" }
}

/** Classifies explicit user text only; it does not authorize or perform memory mutations. */
class ExplicitMemoryCommandParser(private val scanner: MemorySecretScanner = MemorySecretScanner()) {
    private val remember = Regex("^(?:remember that|remember this:|یادت باشه|به خاطر بسپار)\\s+(.+)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val forget = Regex("^(?:forget about|forget this:|فراموش کن)\\s+(.+)$", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    private val commandPrefix = Regex("^(?:remember|forget|یادت باشه|به خاطر بسپار|فراموش کن)(?:\\s|:|$)", RegexOption.IGNORE_CASE)
    fun parse(userText: String): ExplicitMemoryCommand {
        if (userText.length > 4_096) return ExplicitMemoryCommand.Clarify
        val text = userText.trim()
        remember.matchEntire(text)?.let {
            val content = it.groupValues[1].trim()
            if (content.isBlank()) return ExplicitMemoryCommand.Clarify
            if (scanner.inspect(listOf(content)).isNotEmpty()) return ExplicitMemoryCommand.RejectedSecret
            return ExplicitMemoryCommand.Remember(content)
        }
        forget.matchEntire(text)?.let {
            val topic = it.groupValues[1].trim()
            return if (topic.isBlank()) ExplicitMemoryCommand.Clarify else ExplicitMemoryCommand.ForgetTopic(topic)
        }
        return if (commandPrefix.containsMatchIn(text)) ExplicitMemoryCommand.Clarify else ExplicitMemoryCommand.NotCommand
    }
}
