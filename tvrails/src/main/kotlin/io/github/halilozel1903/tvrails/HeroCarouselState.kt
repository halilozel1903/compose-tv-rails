package io.github.halilozel1903.tvrails

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.halilozel1903.tvrails.core.CarouselTimeline

/**
 * The current slide and auto-advance clock of a [HeroCarousel], backed by a [CarouselTimeline].
 *
 * Reading [currentIndex] only recomposes when the slide changes; the per frame [progress] is meant
 * to be read in a draw phase, as [HeroCarouselIndicator] does.
 */
@Stable
public class HeroCarouselState(timeline: CarouselTimeline) {

    /** The underlying timeline. */
    public var timeline: CarouselTimeline by mutableStateOf(timeline)
        private set

    private val indexState: State<Int> = derivedStateOf { this.timeline.index }
    private val countState: State<Int> = derivedStateOf { this.timeline.itemCount }

    /** The slide on screen. */
    public val currentIndex: Int get() = indexState.value

    /** Number of slides. */
    public val itemCount: Int get() = countState.value

    /** Progress of the current slide's interval in `0..1`; `1` when the carousel does not advance. */
    public val progress: Float
        get() = timeline.let { if (it.isAutoAdvancing) it.progress else 1f }

    /** `true` when auto-advance was paused with [pause]. */
    public val isPaused: Boolean get() = timeline.paused

    /** Shows the next slide. */
    public fun next() {
        timeline = timeline.next()
    }

    /** Shows the previous slide. */
    public fun previous() {
        timeline = timeline.previous()
    }

    /** Shows slide [index]. */
    public fun jumpTo(index: Int) {
        timeline = timeline.jumpTo(index)
    }

    /** Stops auto-advance until [resume]. */
    public fun pause() {
        timeline = timeline.withPaused(true)
    }

    /** Restarts auto-advance after [pause]. */
    public fun resume() {
        timeline = timeline.withPaused(false)
    }

    internal fun tick(deltaMillis: Long) {
        if (deltaMillis > 0L) timeline = timeline.advanceBy(deltaMillis)
    }

    internal fun updateItemCount(count: Int) {
        timeline = timeline.withItemCount(count)
    }

    public companion object {
        /** Saves the slide, interval, loop and pause flags. */
        public val Saver: Saver<HeroCarouselState, Any> = listSaver<HeroCarouselState, Any>(
            save = { state ->
                val t = state.timeline
                listOf<Any>(t.itemCount, t.index, t.intervalMillis, t.loop, t.paused)
            },
            restore = { values ->
                HeroCarouselState(
                    CarouselTimeline(
                        itemCount = values[0] as Int,
                        index = values[1] as Int,
                        intervalMillis = values[2] as Long,
                        loop = values[3] as Boolean,
                        paused = values[4] as Boolean,
                    ),
                )
            },
        )
    }
}

/**
 * Creates a [HeroCarouselState] that survives configuration changes and process death.
 *
 * @param itemCount number of slides; [HeroCarousel] keeps it in sync with its items.
 * @param initialIndex the first slide shown.
 * @param intervalMillis how long each slide stays up.
 * @param loop go back to the first slide after the last one.
 */
@Composable
public fun rememberHeroCarouselState(
    itemCount: Int,
    initialIndex: Int = 0,
    intervalMillis: Long = CarouselTimeline.DefaultIntervalMillis,
    loop: Boolean = true,
): HeroCarouselState = rememberSaveable(saver = HeroCarouselState.Saver) {
    val start = if (itemCount == 0) 0 else initialIndex.coerceIn(0, itemCount - 1)
    HeroCarouselState(CarouselTimeline(itemCount, start, intervalMillis = intervalMillis, loop = loop))
}
