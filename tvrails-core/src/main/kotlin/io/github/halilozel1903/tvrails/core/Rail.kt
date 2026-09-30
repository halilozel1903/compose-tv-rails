package io.github.halilozel1903.tvrails.core

/**
 * One title in a rail: a movie, an episode, a channel or anything else a viewer can pick.
 *
 * Artwork is not part of the model on purpose. The Compose components take an `artwork` slot, so
 * you can draw it with Coil, Glide, a resource or a generated gradient, keyed by [id].
 *
 * @property id a stable, unique key. It is used as the lazy list key and to remember focus.
 * @property title the main line, for example the show's name.
 * @property subtitle a short second line, for example `"S2 E5 · Drama"`.
 * @property description a longer text shown by the hero carousel.
 * @property durationMillis the length, or `0` when unknown.
 * @property positionMillis how far the viewer has watched, for continue watching cards.
 * @property badge an optional short label such as `"NEW"` or `"4K"`.
 */
public data class RailItem(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val description: String = "",
    val durationMillis: Long = 0L,
    val positionMillis: Long = 0L,
    val badge: String? = null,
) {
    init {
        require(id.isNotBlank()) { "RailItem id must not be blank" }
        require(durationMillis >= 0L) { "durationMillis must not be negative, was $durationMillis" }
        require(positionMillis >= 0L) { "positionMillis must not be negative, was $positionMillis" }
    }

    /** Watched fraction in `0..1`, `0` when the duration is unknown. */
    public val progress: Float get() = WatchProgress.fraction(positionMillis, durationMillis)

    /** Milliseconds left to watch, never negative. */
    public val remainingMillis: Long get() = WatchProgress.remainingMillis(positionMillis, durationMillis)

    /** `true` when the viewer started this title and has not finished it. */
    public val isInProgress: Boolean get() = WatchProgress.isInProgress(positionMillis, durationMillis)
}

/** How a rail draws its cards. */
public enum class RailStyle {
    /** Portrait 2:3 posters, the classic movie shelf. */
    Poster,

    /** Landscape 16:9 thumbnails. */
    Landscape,

    /** Landscape cards with a progress bar and the time left. */
    ContinueWatching,
}

/**
 * A titled row of [items].
 *
 * @property id a stable, unique key for the rail, used as the lazy list key.
 * @property title the section title shown above the row.
 * @property items the cards, from left to right.
 * @property style the card style of the whole row.
 */
public data class Rail(
    val id: String,
    val title: String,
    val items: List<RailItem>,
    val style: RailStyle = RailStyle.Landscape,
) {
    init {
        require(id.isNotBlank()) { "Rail id must not be blank" }
        val duplicate = items.groupingBy { it.id }.eachCount().entries.firstOrNull { it.value > 1 }
        require(duplicate == null) { "Rail '$id' contains the item id '${duplicate?.key}' more than once" }
    }

    /** Number of cards. */
    public val size: Int get() = items.size

    /** `true` when the rail has no cards. Empty rails are skipped by D-pad navigation. */
    public val isEmpty: Boolean get() = items.isEmpty()

    public companion object {
        /**
         * A continue watching rail made of the [items] that are [RailItem.isInProgress], in their
         * original order.
         */
        public fun continueWatching(id: String, title: String, items: List<RailItem>): Rail =
            Rail(id, title, items.filter { it.isInProgress }, RailStyle.ContinueWatching)
    }
}
