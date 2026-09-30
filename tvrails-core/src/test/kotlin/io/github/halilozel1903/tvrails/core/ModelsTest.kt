package io.github.halilozel1903.tvrails.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ModelsTest {

    private val minute = 60_000L

    @Test
    fun `progress is clamped and zero for unknown durations`() {
        assertEquals(0.25f, RailItem("a", "A", durationMillis = 40 * minute, positionMillis = 10 * minute).progress)
        assertEquals(1f, RailItem("a", "A", durationMillis = minute, positionMillis = 5 * minute).progress)
        assertEquals(0f, RailItem("a", "A", positionMillis = 5 * minute).progress)
        assertEquals(0L, RailItem("a", "A", positionMillis = 5 * minute).remainingMillis)
    }

    @Test
    fun `in progress means started and not finished`() {
        assertFalse(WatchProgress.isInProgress(10_000L, 40 * minute), "barely started")
        assertTrue(WatchProgress.isInProgress(10 * minute, 40 * minute))
        assertFalse(WatchProgress.isInProgress(39 * minute, 40 * minute), "in the credits")
        assertTrue(WatchProgress.isFinished(39 * minute, 40 * minute))
        assertFalse(WatchProgress.isFinished(39 * minute, 40 * minute, finishedThreshold = 0.99f))
        assertFalse(WatchProgress.isInProgress(10 * minute, 0L))
    }

    @Test
    fun `items validate their values`() {
        assertFailsWith<IllegalArgumentException> { RailItem(" ", "Blank") }
        assertFailsWith<IllegalArgumentException> { RailItem("a", "A", durationMillis = -1L) }
        assertFailsWith<IllegalArgumentException> { RailItem("a", "A", positionMillis = -1L) }
    }

    @Test
    fun `rails reject duplicate item ids`() {
        val error = assertFailsWith<IllegalArgumentException> {
            Rail("r", "Row", listOf(RailItem("a", "A"), RailItem("a", "Again")))
        }
        assertTrue("'a'" in error.message.orEmpty())
        assertFailsWith<IllegalArgumentException> { Rail("", "Row", emptyList()) }
    }

    @Test
    fun `continue watching keeps only started titles in order`() {
        val items = listOf(
            RailItem("new", "New", durationMillis = 40 * minute),
            RailItem("half", "Half", durationMillis = 40 * minute, positionMillis = 20 * minute),
            RailItem("done", "Done", durationMillis = 40 * minute, positionMillis = 40 * minute),
            RailItem("start", "Start", durationMillis = 90 * minute, positionMillis = 5 * minute),
        )
        val rail = Rail.continueWatching("cw", "Continue watching", items)
        assertEquals(listOf("half", "start"), rail.items.map { it.id })
        assertEquals(RailStyle.ContinueWatching, rail.style)
        assertEquals(2, rail.size)
        assertFalse(rail.isEmpty)
    }
}
