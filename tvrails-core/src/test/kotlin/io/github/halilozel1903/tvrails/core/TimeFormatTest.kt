package io.github.halilozel1903.tvrails.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeFormatTest {

    private val minute = 60_000L
    private val hour = 60 * minute

    @Test
    fun `durations are short and rounded down`() {
        assertEquals("1h 42m", TimeFormat.duration(hour + 42 * minute + 59_000L))
        assertEquals("2h", TimeFormat.duration(2 * hour))
        assertEquals("48m", TimeFormat.duration(48 * minute))
        assertEquals("45s", TimeFormat.duration(45_000L))
        assertEquals("0s", TimeFormat.duration(0L))
        assertEquals("0s", TimeFormat.duration(-5_000L))
    }

    @Test
    fun `remaining time rounds the last minute up`() {
        assertEquals("32m left", TimeFormat.remaining(10 * minute, 42 * minute))
        assertEquals("1h 5m left", TimeFormat.remaining(0L, hour + 5 * minute))
        assertEquals("1m left", TimeFormat.remaining(42 * minute - 20_000L, 42 * minute))
        assertEquals("0s left", TimeFormat.remaining(50 * minute, 42 * minute))
    }

    @Test
    fun `labels can be translated`() {
        val german = TimeFormat.Labels(hours = " Std.", minutes = " Min.", left = "übrig")
        assertEquals("1 Std. 5 Min. übrig", TimeFormat.remaining(0L, hour + 5 * minute, german))
    }

    @Test
    fun `clock text`() {
        assertEquals("42:07", TimeFormat.clock(42 * minute + 7_000L))
        assertEquals("1:02:03", TimeFormat.clock(hour + 2 * minute + 3_000L))
        assertEquals("0:05", TimeFormat.clock(5_999L))
        assertEquals("0:00", TimeFormat.clock(-1L))
    }

    @Test
    fun `episode and meta text`() {
        assertEquals("S2 E5", TimeFormat.episode(2, 5))
        assertFailsWith<IllegalArgumentException> { TimeFormat.episode(-1, 1) }
        assertEquals("2026 · Drama · 1h 42m", TimeFormat.joinMeta("2026", " Drama ", null, "", "1h 42m"))
        assertEquals("", TimeFormat.joinMeta())
    }
}
