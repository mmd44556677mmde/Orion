package com.mohammad.orion.domain.orchestration

import java.util.Locale

/** An explicit user selection, not a model-supplied tool call. */
enum class UserRoute { AUTO, CONVERSATION, RESEARCH }
enum class RouteDecision { VOICE, BRAIN, CLARIFY, INVALID }

/**
 * Routing suggests which workflow to use. It does not authorize network access, tools, or data
 * disclosure. The executor must independently validate and authorize every operation.
 */
class RequestRouter {
    private val englishResearch = Regex("^(?:please\\s+)?(?:search|research|investigate|look\\s+up)\\b", RegexOption.IGNORE_CASE)
    private val persianResearch = Regex("^(?:لطفا\\s+)?(?:جستجو|جست\\s*و\\s*جو|تحقیق|پژوهش)(?:\\s|$|[،,:])")

    fun route(text: String, userRoute: UserRoute): RouteDecision {
        if (text.isBlank() || text.length > MAX_REQUEST_CHARACTERS) return RouteDecision.INVALID
        return when (userRoute) {
            UserRoute.CONVERSATION -> RouteDecision.VOICE
            UserRoute.RESEARCH -> RouteDecision.BRAIN
            UserRoute.AUTO -> {
                val normalized = text.trim().lowercase(Locale.ROOT)
                    .replace('ي', 'ی').replace('ك', 'ک').replace('\u200c', ' ')
                if (englishResearch.containsMatchIn(normalized) || persianResearch.containsMatchIn(normalized)) {
                    RouteDecision.BRAIN
                } else {
                    RouteDecision.CLARIFY
                }
            }
        }
    }

    companion object { const val MAX_REQUEST_CHARACTERS = 32_768 }
}
