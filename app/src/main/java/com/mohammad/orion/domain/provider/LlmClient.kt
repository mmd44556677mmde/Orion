package com.mohammad.orion.domain.provider

import kotlinx.coroutines.flow.Flow

/** Protocol-independent messages; adapters must never log these values. */
enum class MessageRole { SYSTEM, USER, ASSISTANT, TOOL }
class LlmMessage(val role: MessageRole, val content: String, val toolCallId: String? = null) {
    override fun toString() = "LlmMessage(role=$role, content=[redacted], toolCallId=[redacted])"
}

/** Parameters are JSON Schema, not executable Android capabilities. */
class ToolDefinition(val name: String, val description: String, val parametersJsonSchema: String) {
    override fun toString() = "ToolDefinition([redacted])"
}
class ToolCall(val id: String, val name: String, val argumentsJson: String) {
    override fun toString() = "ToolCall([redacted])"
}
class GenerationRequest(val messages: List<LlmMessage>, val tools: List<ToolDefinition> = emptyList()) {
    override fun toString() = "GenerationRequest([redacted])"
}
data class Usage(val inputTokens: Long?, val outputTokens: Long?)
class GenerationResult(val text: String, val toolCalls: List<ToolCall>, val usage: Usage?) {
    override fun toString() = "GenerationResult([redacted])"
}

sealed interface GenerationEvent {
    class TextDelta(val text: String) : GenerationEvent { override fun toString() = "TextDelta([redacted])" }
    class ToolCallReady(val call: ToolCall) : GenerationEvent { override fun toString() = "ToolCallReady([redacted])" }
    data class Completed(val usage: Usage?) : GenerationEvent
}

/** Safe-to-display categories only: never embed a raw server body, URL, exception, or headers. */
enum class ProviderError {
    HTTP, TLS, TIMEOUT, CANCELLED, AUTHENTICATION, RATE_LIMIT, QUOTA,
    MALFORMED_RESPONSE, SERVER, EMPTY_STREAM, UNSUPPORTED_TOOL, UNSUPPORTED_STREAM, NETWORK
}

/** Implementations must propagate coroutine cancellation and not expose protocol-specific types. */
interface LlmClient {
    suspend fun generate(request: GenerationRequest): LlmResult
    fun stream(request: GenerationRequest): Flow<LlmStreamResult>
}
sealed interface LlmResult {
    class Success(val result: GenerationResult) : LlmResult
    data class Failure(val error: ProviderError) : LlmResult
}
sealed interface LlmStreamResult {
    class Event(val value: GenerationEvent) : LlmStreamResult
    data class Failure(val error: ProviderError) : LlmStreamResult
}
