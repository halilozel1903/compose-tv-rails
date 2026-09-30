package io.github.halilozel1903.tvrails.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class CarouselTimelineTest {

    private val timeline = CarouselTimeline(itemCount = 3, intervalMillis = 1_000L)

    @Test
    fun `time accumulates until the interval`() {
        val later = timeline.advanceBy(400L).advanceBy(300L)
        assertEquals(0, later.index)
        assertEquals(700L, later.elapsedMillis)
        assertEquals(0.7f, later.progress, 0.0001f)
        assertEquals(300L, later.remainingMillis)
    }

    @Test
    fun `advances and keeps the leftover time`() {
        val later = timeline.advanceBy(1_250L)
        assertEquals(1, later.index)
        assertEquals(250L, later.elapsedMillis)
    }

    @Test
    fun `loops back to the first slide`() {
        assertEquals(0, timeline.jumpTo(2).advanceBy(1_000L).index)
        // A long pause (for example a dropped timer) skips several slides at once.
        assertEquals(1, timeline.advanceBy(4_000L).index)
    }

    @Test
    fun `stops on the last slide without looping`() {
        val once = timeline.copy(loop = false)
        val end = once.advanceBy(10_000L)
        assertEquals(2, end.index)
        assertEquals(0L, end.elapsedMillis)
        assertFalse(end.isAutoAdvancing)
        assertSame(end, end.advanceBy(5_000L))
        assertNull(end.remainingMillis)
        assertEquals(0f, end.progress)
    }

    @Test
    fun `paused timelines do not move`() {
        val paused = timeline.advanceBy(300L).withPaused(true)
        assertSame(paused, paused.advanceBy(5_000L))
        assertEquals(0f, paused.progress)
        val resumed = paused.withPaused(false)
        assertEquals(300L, resumed.elapsedMillis, "resuming keeps the elapsed time")
        assertSame(resumed, resumed.withPaused(false))
    }

    @Test
    fun `single and empty carousels never auto advance`() {
        assertFalse(CarouselTimeline(itemCount = 1).isAutoAdvancing)
        assertFalse(CarouselTimeline(itemCount = 0).isAutoAdvancing)
        assertEquals(0, CarouselTimeline(itemCount = 0).next().index)
        assertEquals(0, CarouselTimeline(itemCount = 0).previous().index)
        assertEquals(0, CarouselTimeline(itemCount = 0).jumpTo(4).index)
    }

    @Test
    fun `manual moves restart the clock`() {
        val moved = timeline.advanceBy(900L).next()
        assertEquals(1, moved.index)
        assertEquals(0L, moved.elapsedMillis)
        assertEquals(0, moved.previous().index)
        assertEquals(2, timeline.previous().index, "previous wraps when looping")
        assertEquals(0, timeline.jumpTo(2).next().index, "next wraps when looping")
    }

    @Test
    fun `manual moves clamp without looping`() {
        val once = timeline.copy(loop = false)
        assertEquals(0, once.previous().index)
        assertEquals(2, once.jumpTo(2).next().index)
        assertEquals(2, once.jumpTo(9).index)
        assertEquals(0, once.jumpTo(-4).index)
        assertTrue(once.isFirst)
        assertTrue(once.jumpTo(2).isLast)
    }

    @Test
    fun `item count changes clamp the index`() {
        val onThird = timeline.jumpTo(2).advanceBy(500L)
        assertEquals(1, onThird.withItemCount(2).index)
        assertEquals(0L, onThird.withItemCount(2).elapsedMillis)
        assertEquals(500L, onThird.withItemCount(5).elapsedMillis, "a kept slide keeps its time")
        assertEquals(0, onThird.withItemCount(0).index)
    }

    @Test
    fun `invalid values are rejected`() {
        assertFailsWith<IllegalArgumentException> { CarouselTimeline(itemCount = -1) }
        assertFailsWith<IllegalArgumentException> { CarouselTimeline(itemCount = 2, intervalMillis = 0L) }
        assertFailsWith<IllegalArgumentException> { CarouselTimeline(itemCount = 2, index = 2) }
        assertFailsWith<IllegalArgumentException> { timeline.advanceBy(-1L) }
    }
}
