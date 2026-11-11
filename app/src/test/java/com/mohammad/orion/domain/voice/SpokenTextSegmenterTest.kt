package com.mohammad.orion.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SpokenTextSegmenterTest {
    private val segmenter = SpokenTextSegmenter()
    @Test fun splitsPersianAndEnglishSentences() {
        assertEquals(listOf("سلام!", "حال شما چطور است؟", "خوبم."), segmenter.segment("سلام! حال شما چطور است؟ خوبم."))
        assertEquals(listOf("Hello!", "What next?", "Done."), segmenter.segment("Hello! What next? Done."))
    }
    @Test fun decimalsAndEllipsesAreNotSplitIntoFragments() {
        assertEquals(listOf("Value is 3.14.", "عدد ۳.۵ است.", "Wait...", "Okay…"), segmenter.segment("Value is 3.14. عدد ۳.۵ است. Wait... Okay…"))
    }
    @Test fun stripsCodeAndLinkTargetsWithoutSpeakingTheirContents() {
        val source = "Read [the guide](https://example.org/private?q=secret). `password=secret` Done.\n```kotlin\nprivateCode()\n``` Next."
        val result = segmenter.segment(source)
        assertEquals(listOf("Read the guide.", "Done.", "Next."), result)
        listOf("secret", "example", "privateCode").forEach { secret -> assertFalse(result.joinToString().contains(secret)) }
    }
    @Test fun handlesUrlsNestedTargetsAndUnclosedCodeSafely() {
        assertEquals(listOf("Visit .", "Then read label!"), segmenter.segment("Visit https://example.org/path. Then read [label](https://x.test/a_(b))!"))
        assertEquals(listOf("Before"), segmenter.segment("Before `unclosed secret"))
        assertEquals(listOf("Before"), segmenter.segment("Before [label](https://unfinished"))
    }
    @Test fun ignoresBlankOrCodeOnlyContentAndKeepsClosingQuotes() {
        assertEquals(emptyList<String>(), segmenter.segment("```secret```"))
        assertEquals(emptyList<String>(), segmenter.segment(" ... "))
        assertEquals(listOf("\"Hi!\"", "بعداً؟»"), segmenter.segment("\"Hi!\" بعداً؟»"))
    }
}
