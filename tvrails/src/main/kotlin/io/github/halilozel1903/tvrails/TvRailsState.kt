package io.github.halilozel1903.tvrails

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.halilozel1903.tvrails.core.FocusPosition
import io.github.halilozel1903.tvrails.core.NavigationRules
import io.github.halilozel1903.tvrails.core.Rail
import io.github.halilozel1903.tvrails.core.RailFocusState
import io.github.halilozel1903.tvrails.core.RailItem

/**
 * Focus and scroll state shared by the rails of one screen.
 *
 * It knows which card is focused ([focusedPosition], [focusedItem]), remembers the last focused
 * card of every rail so D-pad up and down return to it, keeps each rail's scroll position while
 * the rail is scrolled off screen, and can move focus to a card with [requestFocus].
 *
 * Create it with [rememberTvRailsState] so the focused card survives configuration changes and
 * process death.
 */
@Stable
public class TvRailsState(
    initialFocus: RailFocusState = RailFocusState(),
    public val rules: NavigationRules = NavigationRules(),
) {
    /** The focused (or last focused) card and the remembered column of every rail. */
    public var focus: RailFocusState by mutableStateOf(initialFocus)
        private set

    /** `true` while a card of these rails has focus. */
    public var isFocused: Boolean by mutableStateOf(false)
        private set

    /** A card that should get focus as soon as it is composed; set by [requestFocus]. */
    public var pendingFocus: FocusPosition? by mutableStateOf(null)
        private set

    private val rowListStates = HashMap<String, LazyListState>()

    /** The focused card, or `null` while focus is outside the rails. */
    public val focusedPosition: FocusPosition? get() = if (isFocused) focus.position else null

    /** The focused item of [rails], or `null` while focus is outside the rails. */
    public fun focusedItem(rails: List<Rail>): RailItem? = focusedPosition?.let { itemAt(rails, it) }

    /** The last focused item of [rails], even when focus has moved elsewhere since. */
    public fun lastFocusedItem(rails: List<Rail>): RailItem? = itemAt(rails, focus.position)

    /** The card index rail [row] returns to, `0` when it was never focused. */
    public fun rememberedColumn(row: Int): Int = focus.rememberedColumn(row)

    /**
     * Moves focus to card [column] of rail [row] (by default the card the rail remembers). The
     * rails scroll to it first when it is off screen.
     */
    public fun requestFocus(row: Int, column: Int = rememberedColumn(row)) {
        require(row >= 0 && column >= 0) { "row and column must not be negative, were $row and $column" }
        pendingFocus = FocusPosition(row, column)
    }

    internal fun consumePendingFocus(request: FocusPosition) {
        if (pendingFocus == request) pendingFocus = null
    }

    internal fun onItemFocused(row: Int, column: Int) {
        if (!isFocused || focus.row != row || focus.column != column) {
            focus = RailFocusState(row, column, focus.memory + (row to column))
        }
        isFocused = true
    }

    internal fun onRailFocusLost(row: Int) {
        if (focus.row == row) isFocused = false
    }

    internal fun rowListState(railId: String, initialIndex: Int): LazyListState =
        rowListStates.getOrPut(railId) { LazyListState(firstVisibleItemIndex = initialIndex) }

    private fun itemAt(rails: List<Rail>, position: FocusPosition): RailItem? =
        rails.getOrNull(position.row)?.items?.getOrNull(position.column)

    public companion object {
        /** Saves the focused card and the remembered column of every rail. */
        public fun saver(rules: NavigationRules = NavigationRules()): Saver<TvRailsState, Any> =
            listSaver<TvRailsState, Int>(
                save = { state -> state.focus.encode() },
                restore = { values -> TvRailsState(RailFocusState.decode(values), rules) },
            )
    }
}

/**
 * Creates a [TvRailsState] that survives configuration changes and process death.
 *
 * @param initialFocus a card to focus once the rails are shown, for example a deep link target.
 *   Leave it `null` to let the screen decide (for example to focus the hero first).
 * @param rules D-pad rules such as wrapping and per rail memory.
 */
@Composable
public fun rememberTvRailsState(
    initialFocus: FocusPosition? = null,
    rules: NavigationRules = NavigationRules(),
): TvRailsState = rememberSaveable(rules, saver = TvRailsState.saver(rules)) {
    val start = initialFocus?.let { RailFocusState(it.row, it.column, mapOf(it.row to it.column)) }
    TvRailsState(start ?: RailFocusState(), rules).apply {
        if (initialFocus != null) requestFocus(initialFocus.row, initialFocus.column)
    }
}
