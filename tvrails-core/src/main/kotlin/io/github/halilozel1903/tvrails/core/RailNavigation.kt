package io.github.halilozel1903.tvrails.core

import kotlin.math.abs

/** A D-pad key. */
public enum class NavDirection {
    Up,
    Down,
    Left,
    Right,
}

/** A card in the rails grid: [row] is the rail index, [column] the card index inside it. */
public data class FocusPosition(val row: Int, val column: Int)

/**
 * How D-pad presses move between rails and cards.
 *
 * @property wrapColumns `Right` on the last card jumps to the first one and `Left` on the first
 *   card to the last one. Off by default, as on most TV launchers, so focus can leave the rail.
 * @property wrapRows `Down` on the last rail jumps to the first rail and back.
 * @property rememberColumns each rail remembers its last focused card and focus returns to it when
 *   you come back vertically. When off, focus keeps its column (clamped), like a grid.
 */
public data class NavigationRules(
    val wrapColumns: Boolean = false,
    val wrapRows: Boolean = false,
    val rememberColumns: Boolean = true,
)

/**
 * The focused card plus the column each rail remembers.
 *
 * @property row the focused rail.
 * @property column the focused card inside [row].
 * @property memory the last focused column per rail index, including [row].
 */
public data class RailFocusState(
    val row: Int = 0,
    val column: Int = 0,
    val memory: Map<Int, Int> = emptyMap(),
) {
    init {
        require(row >= 0 && column >= 0) { "row and column must not be negative, were $row and $column" }
    }

    /** [row] and [column] as a [FocusPosition]. */
    public val position: FocusPosition get() = FocusPosition(row, column)

    /** The column [row] remembers, `0` when it was never focused. */
    public fun rememberedColumn(row: Int): Int = if (row == this.row) column else memory[row] ?: 0

    /**
     * A flat list for saving: `[row, column, row0, column0, row1, column1, ...]`. Read it back with
     * [decode].
     */
    public fun encode(): List<Int> = buildList<Int> {
        add(row)
        add(column)
        for ((savedRow, savedColumn) in memory.entries.sortedBy { it.key }) {
            add(savedRow)
            add(savedColumn)
        }
    }

    public companion object {
        /** Reads a list written by [encode]. Returns a default state for malformed input. */
        public fun decode(values: List<Int>): RailFocusState {
            if (values.size < 2 || values.size % 2 != 0 || values.any { it < 0 }) return RailFocusState()
            val memory = buildMap<Int, Int> {
                var i = 2
                while (i < values.size) {
                    put(values[i], values[i + 1])
                    i += 2
                }
            }
            return RailFocusState(values[0], values[1], memory)
        }
    }
}

/** The outcome of [RailNavigator.move]. */
public sealed interface MoveResult {
    /** Focus moved to another card. */
    public data class Moved(val state: RailFocusState) : MoveResult

    /**
     * There is no card in [direction]: focus should leave the rails, for example up into the hero
     * or left into a navigation drawer. The state is unchanged.
     */
    public data class Edge(val direction: NavDirection) : MoveResult
}

/**
 * Pure D-pad navigation over rails of [rowSizes] cards each.
 *
 * Empty rails are skipped. Columns are clamped to the target rail's size, so moving down from the
 * 8th card onto a rail of 3 lands on its last card.
 */
public class RailNavigator(
    rowSizes: List<Int>,
    public val rules: NavigationRules = NavigationRules(),
) {
    /** Cards per rail. */
    public val rowSizes: List<Int> = rowSizes.toList()

    init {
        require(this.rowSizes.all { it >= 0 }) { "row sizes must not be negative: ${this.rowSizes}" }
    }

    /** `true` when no rail has a card. */
    public val isEmpty: Boolean get() = rowSizes.all { it == 0 }

    /** The first card of the first non empty rail, or `null` when there is none. */
    public fun initial(): RailFocusState? {
        val row = rowSizes.indexOfFirst { it > 0 }
        return if (row < 0) null else RailFocusState(row, 0, mapOf(row to 0))
    }

    /**
     * Focuses [row], [column] directly, for example when a card got focus by other means. Both are
     * clamped. The column is remembered for that rail.
     */
    public fun focus(state: RailFocusState, row: Int, column: Int): RailFocusState {
        if (isEmpty) return state
        val targetRow = nearestNonEmptyRow(row.coerceIn(0, rowSizes.lastIndex))
        val targetColumn = column.coerceIn(0, rowSizes[targetRow] - 1)
        return RailFocusState(targetRow, targetColumn, clampMemory(state.memory) + (targetRow to targetColumn))
    }

    /**
     * Brings [state] back in range after the rails changed: out of range or empty rails move to the
     * nearest non empty one, columns are clamped, and memory of removed rails is dropped.
     */
    public fun clamp(state: RailFocusState): RailFocusState {
        if (isEmpty) return RailFocusState()
        return focus(state, state.row, state.column)
    }

    /** Moves focus one step in [direction]. */
    public fun move(state: RailFocusState, direction: NavDirection): MoveResult {
        if (isEmpty) return MoveResult.Edge(direction)
        val current = clamp(state)
        return when (direction) {
            NavDirection.Left, NavDirection.Right -> moveHorizontally(current, direction)
            NavDirection.Up, NavDirection.Down -> moveVertically(current, direction)
        }
    }

    private fun moveHorizontally(state: RailFocusState, direction: NavDirection): MoveResult {
        val size = rowSizes[state.row]
        val step = if (direction == NavDirection.Right) 1 else -1
        var target = state.column + step
        if (target !in 0 until size) {
            if (!rules.wrapColumns || size < 2) return MoveResult.Edge(direction)
            target = if (step > 0) 0 else size - 1
        }
        return MoveResult.Moved(state.copy(column = target, memory = state.memory + (state.row to target)))
    }

    private fun moveVertically(state: RailFocusState, direction: NavDirection): MoveResult {
        val step = if (direction == NavDirection.Down) 1 else -1
        val targetRow = nextNonEmptyRow(state.row, step) ?: return MoveResult.Edge(direction)
        if (targetRow == state.row) return MoveResult.Edge(direction)
        val wanted = if (rules.rememberColumns) state.memory[targetRow] ?: 0 else state.column
        val targetColumn = wanted.coerceIn(0, rowSizes[targetRow] - 1)
        val memory = state.memory + (state.row to state.column) + (targetRow to targetColumn)
        return MoveResult.Moved(RailFocusState(targetRow, targetColumn, memory))
    }

    private fun nextNonEmptyRow(from: Int, step: Int): Int? {
        var row = from
        repeat(rowSizes.size) {
            row += step
            if (row !in rowSizes.indices) {
                if (!rules.wrapRows) return null
                row = if (step > 0) 0 else rowSizes.lastIndex
            }
            if (rowSizes[row] > 0) return row
        }
        return null
    }

    private fun nearestNonEmptyRow(row: Int): Int {
        if (rowSizes[row] > 0) return row
        return rowSizes.indices
            .filter { rowSizes[it] > 0 }
            .minWith(compareBy<Int> { abs(it - row) }.thenBy { it })
    }

    private fun clampMemory(memory: Map<Int, Int>): Map<Int, Int> = buildMap<Int, Int> {
        for ((row, column) in memory) {
            if (row in rowSizes.indices && rowSizes[row] > 0) put(row, column.coerceIn(0, rowSizes[row] - 1))
        }
    }
}
