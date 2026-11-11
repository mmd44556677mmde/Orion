package com.mohammad.orion.domain.tools

import org.junit.Assert.*
import org.junit.Test

class OfflineCommandParserTest {
    private val parser = OfflineCommandParser()
    @Test fun timersHandlePersianAndEnglishWithLimits() {
        assertEquals(OfflineParseResult.Timer(300), parser.parse("set a timer for 5 minutes"))
        assertEquals(OfflineParseResult.Timer(120), parser.parse("تایمر برای ۲ دقیقه"))
        assertEquals(OfflineParseResult.Timer(10), parser.parse("timer for ١٠ seconds"))
        assertEquals(OfflineParseResult.Clarify(Clarification.DURATION), parser.parse("timer for 0 seconds"))
        assertEquals(OfflineParseResult.Clarify(Clarification.DURATION), parser.parse("timer for 25 hours"))
        assertEquals(OfflineParseResult.Clarify(Clarification.DURATION), parser.parse("timer for 5"))
    }
    @Test fun ambiguousTimesNeverGuessAmOrPm() {
        assertEquals(OfflineParseResult.Alarm(19, 30), parser.parse("set alarm for 19:30"))
        assertEquals(OfflineParseResult.Alarm(19, 30), parser.parse("alarm at 7:30 pm"))
        assertEquals(OfflineParseResult.Alarm(0, 5), parser.parse("آلارم برای ۰۰:۰۵"))
        assertEquals(OfflineParseResult.Clarify(Clarification.TIME), parser.parse("set alarm for 7:30"))
        assertEquals(OfflineParseResult.Clarify(Clarification.TIME), parser.parse("alarm at 24:30"))
        assertEquals(OfflineParseResult.Clarify(Clarification.TIME), parser.parse("alarm at 8:99 pm"))
    }
    @Test fun flashlightRequiresAnExactUnambiguousCommand() {
        assertEquals(OfflineParseResult.Flashlight(true), parser.parse("turn on the flashlight"))
        assertEquals(OfflineParseResult.Flashlight(false), parser.parse("چراغ قوه را خاموش کن"))
        assertEquals(OfflineParseResult.Unrecognized, parser.parse("do not turn on the flashlight"))
    }
    @Test fun appNameIsAQueryNotAResolvedTarget() {
        val result = parser.parse("open app Maps") as OfflineParseResult.OpenApp
        assertEquals("Maps", result.label)
        assertFalse(result.toString().contains("Maps"))
        assertEquals("نقشه", (parser.parse("برنامه نقشه را باز کن") as OfflineParseResult.OpenApp).label)
        assertEquals(OfflineParseResult.Clarify(Clarification.APP), parser.parse("open app"))
        assertEquals(OfflineParseResult.Clarify(Clarification.APP), parser.parse("open maps; send a message"))
    }
    @Test fun refusesMultipleRequestsAndVeryLongInput() {
        assertEquals(OfflineParseResult.Clarify(Clarification.DURATION), parser.parse("timer for 5 minutes and open maps"))
        assertEquals(OfflineParseResult.Clarify(Clarification.COMMAND_TOO_LONG), parser.parse("x".repeat(257)))
        assertEquals(OfflineParseResult.Unrecognized, parser.parse("hello"))
    }
}
