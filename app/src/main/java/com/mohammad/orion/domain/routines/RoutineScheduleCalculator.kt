package com.mohammad.orion.domain.routines

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId

/**
 * Computes a future daily wall-clock occurrence; it neither schedules nor executes a routine.
 * Missed runs are skipped. DST gaps move forward by the gap; overlaps use the earlier offset,
 * so a wall-clock date has at most one occurrence. Call with the current zone after zone changes.
 */
class RoutineScheduleCalculator {
    fun nextDaily(enabled: Boolean, time: LocalTime, after: Instant, zone: ZoneId): Instant? {
        if (!enabled) return null
        var date = after.atZone(zone).toLocalDate()
        while (true) {
            val candidate = date.atTime(time).atZone(zone).withEarlierOffsetAtOverlap().toInstant()
            if (candidate > after) return candidate
            date = date.plusDays(1)
        }
    }
}
