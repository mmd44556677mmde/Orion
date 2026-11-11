package com.mohammad.orion.domain.routines

import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.*
import org.junit.Test

class RoutineScheduleCalculatorTest {
    private val calc = RoutineScheduleCalculator()
    private val berlin = ZoneId.of("Europe/Berlin")
    @Test fun disabledAndMissedSchedulesDoNotCatchUp() {
        val now = Instant.parse("2026-01-01T09:00:00Z")
        assertNull(calc.nextDaily(false, LocalTime.of(9, 0), now, berlin))
        assertEquals(Instant.parse("2026-01-02T08:00:00Z"), calc.nextDaily(true, LocalTime.of(9, 0), now, berlin))
        assertEquals(Instant.parse("2026-01-02T08:00:00Z"), calc.nextDaily(true, LocalTime.of(9, 0), Instant.parse("2026-01-01T08:00:00Z"), berlin))
    }
    @Test fun movesSpringGapForwardAndUsesOnlyEarlierAutumnOccurrence() {
        assertEquals(Instant.parse("2026-03-29T01:30:00Z"), calc.nextDaily(true, LocalTime.of(2, 30), Instant.parse("2026-03-28T23:00:00Z"), berlin))
        assertEquals(Instant.parse("2026-10-25T00:30:00Z"), calc.nextDaily(true, LocalTime.of(2, 30), Instant.parse("2026-10-24T23:00:00Z"), berlin))
        assertEquals(Instant.parse("2026-10-26T01:30:00Z"), calc.nextDaily(true, LocalTime.of(2, 30), Instant.parse("2026-10-25T00:45:00Z"), berlin))
    }
    @Test fun changedZoneIsUsedWhenRecalculating() {
        val now = Instant.parse("2026-01-01T07:00:00Z")
        assertEquals(Instant.parse("2026-01-01T08:00:00Z"), calc.nextDaily(true, LocalTime.of(9, 0), now, berlin))
        assertEquals(Instant.parse("2026-01-01T09:00:00Z"), calc.nextDaily(true, LocalTime.of(9, 0), now, ZoneId.of("UTC")))
    }
}
