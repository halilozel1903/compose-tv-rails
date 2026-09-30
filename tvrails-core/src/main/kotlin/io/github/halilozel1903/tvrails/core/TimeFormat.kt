package io.github.halilozel1903.tvrails.core

/**
 * Locale independent duration texts for cards and the hero. Pass your own words through [Labels]
 * to translate them.
 */
public object TimeFormat {

    /** Unit words used by [duration] and [remaining]. */
    public data class Labels(
        val hours: String = "h",
        val minutes: String = "m",
        val seconds: String = "s",
        val left: String = "left",
    )

    private val DefaultLabels = Labels()

    /**
     * A short duration: `"1h 42m"`, `"2h"`, `"48m"`, `"45s"`. Seconds are shown only below one
     * minute; minutes are rounded down. Negative values are treated as `0`.
     */
    public fun duration(millis: Long, labels: Labels = DefaultLabels): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return when {
            hours > 0L && minutes > 0L -> "$hours${labels.hours} $minutes${labels.minutes}"
            hours > 0L -> "$hours${labels.hours}"
            minutes > 0L -> "$minutes${labels.minutes}"
            else -> "$seconds${labels.seconds}"
        }
    }

    /**
     * The time left as a card shows it: `"32m left"`, `"1h 5m left"`. Below one minute it rounds up
     * to `"1m left"` so an almost finished title never reads `"0s left"`.
     */
    public fun remaining(position: Long, duration: Long, labels: Labels = DefaultLabels): String {
        val left = WatchProgress.remainingMillis(position, duration)
        val text = if (left in 1L until 60_000L) "1${labels.minutes}" else duration(left, labels)
        return "$text ${labels.left}"
    }

    /** A player style clock: `"42:07"`, `"1:02:03"`. Negative values are treated as `0`. */
    public fun clock(millis: Long): String {
        val totalSeconds = millis.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        val mm = minutes.toString().padStart(2, '0')
        val ss = seconds.toString().padStart(2, '0')
        return if (hours > 0L) "$hours:$mm:$ss" else "$minutes:$ss"
    }

    /** An episode label: `episode(2, 5) == "S2 E5"`. */
    public fun episode(season: Int, episode: Int): String {
        require(season >= 0 && episode >= 0) { "season and episode must not be negative" }
        return "S$season E$episode"
    }

    /** Joins the non blank [parts] with a middle dot: `"2026 · Drama · 1h 42m"`. */
    public fun joinMeta(vararg parts: String?): String =
        parts.filterNotNull().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" · ")
}
