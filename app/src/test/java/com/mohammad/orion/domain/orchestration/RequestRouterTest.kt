package com.mohammad.orion.domain.orchestration

import org.junit.Assert.assertEquals
import org.junit.Test

class RequestRouterTest {
    private val router = RequestRouter()

    @Test fun explicitUserChoiceWinsOverWording() {
        assertEquals(RouteDecision.VOICE, router.route("Research this topic", UserRoute.CONVERSATION))
        assertEquals(RouteDecision.BRAIN, router.route("Hello", UserRoute.RESEARCH))
    }
    @Test fun explicitResearchCommandsAreRecognizedInEnglishAndPersian() {
        listOf("Research the latest findings", "please search for sources", "تحقیق دربارهٔ موضوع", "جست‌وجو دربارهٔ موضوع").forEach {
            assertEquals(it, RouteDecision.BRAIN, router.route(it, UserRoute.AUTO))
        }
    }
    @Test fun ambiguousOrNegatedRequestsAreNotAutomaticallyDelegated() {
        listOf("Hello", "Don't search the web", "The researcher said hello", "Can you research this?").forEach {
            assertEquals(it, RouteDecision.CLARIFY, router.route(it, UserRoute.AUTO))
        }
    }
    @Test fun rejectsEmptyAndOversizedInputRegardlessOfSelection() {
        UserRoute.entries.forEach { route ->
            assertEquals(RouteDecision.INVALID, router.route("   ", route))
            assertEquals(RouteDecision.INVALID, router.route("x".repeat(RequestRouter.MAX_REQUEST_CHARACTERS + 1), route))
        }
    }
    @Test fun acceptsBoundarySizeForExplicitConversation() {
        assertEquals(RouteDecision.VOICE, router.route("x".repeat(RequestRouter.MAX_REQUEST_CHARACTERS), UserRoute.CONVERSATION))
    }
}
