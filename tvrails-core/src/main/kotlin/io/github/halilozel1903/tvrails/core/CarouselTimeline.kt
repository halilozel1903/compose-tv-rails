package io.github.halilozel1903.tvrails.core

/**
 * The auto-advance clock of a hero carousel, as an immutable value.
 *
 * Feed it frame or timer deltas with [advanceBy]; it moves to the next slide every
 * [intervalMillis]. Manual moves ([next], [previous], [jumpTo]) restart the clock so a slide the
 * viewer just picked stays up for a full interval.
 *
 * @property itemCount number of slides.
 * @property index the current slide.
 * @property elapsedMillis time spent on the current slide.
 * @property intervalMillis how long each slide stays up.
 * @property loop after the last slide go back to the first. When off, the carousel stops on the
 *   last slide.
 * @property paused auto-advance is on hold, for example while the viewer reads the focused slide.
 */
public data class CarouselTimeline(
    val itemCount: Int,
    val index: Int = 0,
    val elapsedMillis: Long = 0L,
    val intervalMillis: Long = DefaultIntervalMillis,
    val loop: Boolean = true,
    val paused: Boolean = false,
) {
    init {
        require(itemCount >= 0) { "itemCount must not be negative, was $itemCount" }
        require(intervalMillis > 0L) { "intervalMillis must be positive, was $intervalMillis" }
        require(elapsedMillis >= 0L) { "elapsedMillis must not be negative, was $elapsedMillis" }
        require(index == 0 || index in 0 until itemCount) { "index $index is out of 0 until $itemCount" }
    }

    /** `true` on the first slide. */
    public val isFirst: Boolean get() = index == 0

    /** `true` on the last slide (or when there are no slides). */
    public val isLast: Boolean get() = index >= itemCount - 1

    /** `true` when time passing will change the slide. */
    public val isAutoAdvancing: Boolean get() = !paused && itemCount > 1 && (loop || !isLast)

    /** Progress of the current slide's interval in `0..1`, `0` when not auto-advancing. */
    public val progress: Float
        get() = if (!isAutoAdvancing) 0f else (elapsedMillis.toFloat() / intervalMillis).coerceIn(0f, 1f)

    /** Milliseconds until the next automatic move, or `null` when not auto-advancing. */
    public val remainingMillis: Long?
        get() = if (isAutoAdvancing) (intervalMillis - elapsedMillis).coerceAtLeast(0L) else null

    /** Lets [deltaMillis] pass. Does nothing while paused or when there is nowhere to go. */
    public fun advanceBy(deltaMillis: Long): CarouselTimeline {
        require(deltaMillis >= 0L) { "deltaMillis must not be negative, was $deltaMillis" }
        if (!isAutoAdvancing || deltaMillis == 0L) return this
        val total = elapsedMillis + deltaMillis
        val steps = total / intervalMillis
        val leftover = total % intervalMillis
        if (steps == 0L) return copy(elapsedMillis = total)
        return if (loop) {
            copy(index = ((index + steps) % itemCount).toInt(), elapsedMillis = leftover)
        } else {
            val target = (index + steps).coerceAtMost((itemCount - 1).toLong()).toInt()
            copy(index = target, elapsedMillis = if (target == itemCount - 1) 0L else leftover)
        }
    }

    /** The next slide (wrapping when [loop]), restarting the clock. */
    public fun next(): CarouselTimeline {
        if (itemCount == 0) return this
        val target = when {
            !isLast -> index + 1
            loop -> 0
            else -> index
        }
        return copy(index = target, elapsedMillis = 0L)
    }

    /** The previous slide (wrapping when [loop]), restarting the clock. */
    public fun previous(): CarouselTimeline {
        if (itemCount == 0) return this
        val target = when {
            !isFirst -> index - 1
            loop -> itemCount - 1
            else -> index
        }
        return copy(index = target, elapsedMillis = 0L)
    }

    /** Shows slide [target] (clamped), restarting the clock. */
    public fun jumpTo(target: Int): CarouselTimeline {
        if (itemCount == 0) return this
        return copy(index = target.coerceIn(0, itemCount - 1), elapsedMillis = 0L)
    }

    /** Pauses or resumes auto-advance. The elapsed time is kept. */
    public fun withPaused(paused: Boolean): CarouselTimeline = if (paused == this.paused) this else copy(paused = paused)

    /** Adapts to a new number of slides, clamping the index. */
    public fun withItemCount(count: Int): CarouselTimeline {
        require(count >= 0) { "count must not be negative, was $count" }
        if (count == itemCount) return this
        val target = if (count == 0) 0 else index.coerceAtMost(count - 1)
        return copy(itemCount = count, index = target, elapsedMillis = if (target == index) elapsedMillis else 0L)
    }

    public companion object {
        /** Default time a slide stays up: 7 seconds. */
        public const val DefaultIntervalMillis: Long = 7_000L
    }
}
