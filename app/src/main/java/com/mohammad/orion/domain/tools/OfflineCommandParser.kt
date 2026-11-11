package com.mohammad.orion.domain.tools

import java.util.Locale

/** Parsing is not authorization and never invokes an Android API. */
sealed interface OfflineParseResult {
    data object Unrecognized : OfflineParseResult
    data class Clarify(val reason: Clarification) : OfflineParseResult
    data class Timer(val seconds: Int) : OfflineParseResult
    data class Alarm(val hour: Int, val minute: Int) : OfflineParseResult
    data class Flashlight(val enabled: Boolean) : OfflineParseResult
    class OpenApp(val label: String) : OfflineParseResult {
        override fun toString() = "OpenApp(label=[redacted])"
    }
}
enum class Clarification { DURATION, TIME, APP, COMMAND_TOO_LONG }

/** Conservative, exact-match grammar; unclear requests are not guessed. */
class OfflineCommandParser {
    private val timer = Regex("^(?:set (?:a )?timer for|timer for|تایمر برای|زمان سنج برای) ([0-9]{1,6}) (seconds?|minutes?|hours?|ثانیه|دقیقه|ساعت)$", RegexOption.IGNORE_CASE)
    private val alarm = Regex("^(?:set (?:an )?alarm (?:for|at)|alarm (?:for|at)|آلارم برای|زنگ هشدار برای) ([0-9]{1,2}):([0-9]{2})(?: (am|pm))?$", RegexOption.IGNORE_CASE)
    private val openEnglish = Regex("^open (?:app )?(.+)$", RegexOption.IGNORE_CASE)
    private val openPersian = Regex("^برنامه (.+) را باز کن$")
    private val allowedAppLabel = Regex("^[\\p{L}\\p{M}\\p{N} ._\\-]{1,80}$")

    fun parse(text: String): OfflineParseResult {
        if (text.length > MAX_INPUT) return OfflineParseResult.Clarify(Clarification.COMMAND_TOO_LONG)
        val input = normalize(text)
        val lower = input.lowercase(Locale.ROOT)
        when (lower) {
            "turn on flashlight", "turn on the flashlight", "flashlight on", "چراغ قوه را روشن کن", "چراغ قوه روشن" -> return OfflineParseResult.Flashlight(true)
            "turn off flashlight", "turn off the flashlight", "flashlight off", "چراغ قوه را خاموش کن", "چراغ قوه خاموش" -> return OfflineParseResult.Flashlight(false)
        }
        timer.matchEntire(input)?.let { match ->
            val value = match.groupValues[1].toLongOrNull() ?: return OfflineParseResult.Clarify(Clarification.DURATION)
            val unit = match.groupValues[2].lowercase(Locale.ROOT)
            val multiplier = when (unit) {
                "second", "seconds", "ثانیه" -> 1L
                "minute", "minutes", "دقیقه" -> 60L
                else -> 3600L
            }
            val seconds = value * multiplier
            return if (seconds in 1..86_400) OfflineParseResult.Timer(seconds.toInt()) else OfflineParseResult.Clarify(Clarification.DURATION)
        }
        alarm.matchEntire(input)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return OfflineParseResult.Clarify(Clarification.TIME)
            val minute = match.groupValues[2].toIntOrNull() ?: return OfflineParseResult.Clarify(Clarification.TIME)
            val meridiem = match.groupValues[3].lowercase(Locale.ROOT)
            if (minute !in 0..59) return OfflineParseResult.Clarify(Clarification.TIME)
            val normalizedHour = when {
                meridiem.isNotEmpty() && hour !in 1..12 -> return OfflineParseResult.Clarify(Clarification.TIME)
                meridiem == "am" -> hour % 12
                meridiem == "pm" -> hour % 12 + 12
                hour == 0 || hour in 13..23 -> hour
                else -> return OfflineParseResult.Clarify(Clarification.TIME)
            }
            return OfflineParseResult.Alarm(normalizedHour, minute)
        }
        val appMatch = openEnglish.matchEntire(input) ?: openPersian.matchEntire(input)
        if (appMatch != null) {
            val label = appMatch.groupValues[1].trim()
            return if (label.equals("app", ignoreCase = true) || !allowedAppLabel.matches(label)) OfflineParseResult.Clarify(Clarification.APP) else OfflineParseResult.OpenApp(label)
        }
        return when {
            lower.startsWith("set timer") || lower.startsWith("set a timer") || lower.startsWith("timer") || lower.startsWith("تایمر") || lower.startsWith("زمان سنج") -> OfflineParseResult.Clarify(Clarification.DURATION)
            lower.startsWith("set alarm") || lower.startsWith("set an alarm") || lower.startsWith("alarm") || lower.startsWith("آلارم") || lower.startsWith("زنگ هشدار") -> OfflineParseResult.Clarify(Clarification.TIME)
            lower == "open" || lower == "open app" || lower == "برنامه" || lower.startsWith("برنامه ") -> OfflineParseResult.Clarify(Clarification.APP)
            else -> OfflineParseResult.Unrecognized
        }
    }

    private fun normalize(text: String): String = buildString(text.length) {
        text.forEach { char ->
            when (char) {
                in '۰'..'۹' -> append('0' + (char - '۰'))
                in '٠'..'٩' -> append('0' + (char - '٠'))
                'ي' -> append('ی')
                'ك' -> append('ک')
                '\u200c' -> append(' ')
                in '\u064b'..'\u065f' -> Unit
                else -> append(char)
            }
        }
    }.trim().replace(Regex("\\s+"), " ")

    companion object { private const val MAX_INPUT = 256 }
}
