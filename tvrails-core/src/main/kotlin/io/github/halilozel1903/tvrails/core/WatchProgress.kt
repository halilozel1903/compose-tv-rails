package io.github.halilozel1903.tvrails.core

/** Watch progress math shared by the continue watching card and [RailItem]. */
public object WatchProgress {

    /** A title counts as finished from this fraction on (credits usually start here). */
    public const val DefaultFinishedThreshold: Float = 0.95f

    /** A title counts as started once this much has been watched. */
    public const val DefaultStartedMillis: Long = 30_000L

    /** [position] divided by [duration], clamped to `0..1`. `0` when the duration is unknown. */
    public fun fraction(position: Long, duration: Long): Float {
        if (duration <= 0L) return 0f
        return (position.toDouble() / duration.toDouble()).coerceIn(0.0, 1.0).toFloat()
    }

    /** Milliseconds left, never negative. `0` when the duration is unknown. */
    public fun remainingMillis(position: Long, duration: Long): Long {
        if (duration <= 0L) return 0L
        return (duration - position.coerceAtLeast(0L)).coerceAtLeast(0L)
    }

    /** `true` when at least [finishedThreshold] of the title has been watched. */
    public fun isFinished(
        position: Long,
        duration: Long,
        finishedThreshold: Float = DefaultFinishedThreshold,
    ): Boolean = duration > 0L && fraction(position, duration) >= finishedThreshold

    /** `true` when the title was started (at least [startedMillis]) and is not finished. */
    public fun isInProgress(
        position: Long,
        duration: Long,
        startedMillis: Long = DefaultStartedMillis,
        finishedThreshold: Float = DefaultFinishedThreshold,
    ): Boolean = duration > 0L && position >= startedMillis && !isFinished(position, duration, finishedThreshold)
}
