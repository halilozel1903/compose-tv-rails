package io.github.halilozel1903.tvrails.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class RailNavigatorTest {

    private fun RailNavigator.moved(state: RailFocusState, direction: NavDirection): RailFocusState {
        val result = move(state, direction)
        assertIs<MoveResult.Moved>(result, "expected a move $direction from $state")
        return result.state
    }

    private fun RailNavigator.path(start: RailFocusState, vararg directions: NavDirection): RailFocusState =
        directions.fold(start) { state, direction -> moved(state, direction) }

    @Test
    fun `initial focus is the first card of the first non empty rail`() {
        assertEquals(RailFocusState(0, 0, mapOf(0 to 0)), RailNavigator(listOf(3, 4)).initial())
        assertEquals(1, RailNavigator(listOf(0, 4)).initial()?.row)
        assertNull(RailNavigator(listOf(0, 0)).initial())
        assertNull(RailNavigator(emptyList()).initial())
    }

    @Test
    fun `right and left move inside a rail`() {
        val nav = RailNavigator(listOf(5))
        val state = nav.path(RailFocusState(), NavDirection.Right, NavDirection.Right, NavDirection.Left)
        assertEquals(FocusPosition(0, 1), state.position)
        assertEquals(1, state.memory[0])
    }

    @Test
    fun `edges are reported without wrapping`() {
        val nav = RailNavigator(listOf(3, 3))
        assertEquals(MoveResult.Edge(NavDirection.Left), nav.move(RailFocusState(0, 0), NavDirection.Left))
        assertEquals(MoveResult.Edge(NavDirection.Right), nav.move(RailFocusState(0, 2), NavDirection.Right))
        assertEquals(MoveResult.Edge(NavDirection.Up), nav.move(RailFocusState(0, 1), NavDirection.Up))
        assertEquals(MoveResult.Edge(NavDirection.Down), nav.move(RailFocusState(1, 1), NavDirection.Down))
    }

    @Test
    fun `columns wrap when enabled`() {
        val nav = RailNavigator(listOf(3), NavigationRules(wrapColumns = true))
        assertEquals(0, nav.moved(RailFocusState(0, 2), NavDirection.Right).column)
        assertEquals(2, nav.moved(RailFocusState(0, 0), NavDirection.Left).column)
    }

    @Test
    fun `a single card rail never wraps onto itself`() {
        val nav = RailNavigator(listOf(1), NavigationRules(wrapColumns = true))
        assertIs<MoveResult.Edge>(nav.move(RailFocusState(0, 0), NavDirection.Right))
    }

    @Test
    fun `rows wrap when enabled`() {
        val nav = RailNavigator(listOf(2, 2, 2), NavigationRules(wrapRows = true))
        assertEquals(0, nav.moved(RailFocusState(2, 0), NavDirection.Down).row)
        assertEquals(2, nav.moved(RailFocusState(0, 0), NavDirection.Up).row)
    }

    @Test
    fun `a single rail with row wrapping reports an edge`() {
        val nav = RailNavigator(listOf(4), NavigationRules(wrapRows = true))
        assertIs<MoveResult.Edge>(nav.move(RailFocusState(0, 0), NavDirection.Down))
    }

    @Test
    fun `each rail remembers its column`() {
        val nav = RailNavigator(listOf(10, 10))
        // Rail 0 -> card 3, down, rail 1 -> card 1, up: back on card 3.
        var state = nav.path(RailFocusState(), NavDirection.Right, NavDirection.Right, NavDirection.Right)
        state = nav.moved(state, NavDirection.Down)
        assertEquals(FocusPosition(1, 0), state.position, "an unvisited rail starts at its first card")
        state = nav.moved(state, NavDirection.Right)
        state = nav.moved(state, NavDirection.Up)
        assertEquals(FocusPosition(0, 3), state.position)
        state = nav.moved(state, NavDirection.Down)
        assertEquals(FocusPosition(1, 1), state.position)
    }

    @Test
    fun `without memory focus keeps its column like a grid`() {
        val nav = RailNavigator(listOf(10, 10), NavigationRules(rememberColumns = false))
        val state = nav.moved(RailFocusState(0, 6), NavDirection.Down)
        assertEquals(FocusPosition(1, 6), state.position)
    }

    @Test
    fun `columns are clamped to a shorter rail`() {
        val nav = RailNavigator(listOf(10, 3), NavigationRules(rememberColumns = false))
        assertEquals(FocusPosition(1, 2), nav.moved(RailFocusState(0, 8), NavDirection.Down).position)
    }

    @Test
    fun `empty rails are skipped`() {
        val nav = RailNavigator(listOf(4, 0, 0, 4))
        assertEquals(3, nav.moved(RailFocusState(0, 0), NavDirection.Down).row)
        assertEquals(0, nav.moved(RailFocusState(3, 0), NavDirection.Up).row)
    }

    @Test
    fun `navigation over no cards is always an edge`() {
        val nav = RailNavigator(listOf(0, 0))
        for (direction in NavDirection.entries) {
            assertEquals(MoveResult.Edge(direction), nav.move(RailFocusState(), direction))
        }
    }

    @Test
    fun `focus clamps and remembers`() {
        val nav = RailNavigator(listOf(5, 2))
        val state = nav.focus(RailFocusState(), row = 1, column = 9)
        assertEquals(FocusPosition(1, 1), state.position)
        assertEquals(1, state.memory[1])
        assertEquals(FocusPosition(1, 0), nav.focus(state, row = 7, column = -3).position)
    }

    @Test
    fun `clamp moves focus off removed or emptied rails`() {
        val before = RailFocusState(row = 3, column = 7, memory = mapOf(0 to 4, 3 to 7, 5 to 1))
        val nav = RailNavigator(listOf(3, 6, 0, 0))
        val after = nav.clamp(before)
        // Row 3 is empty now: the nearest non empty rail is 1.
        assertEquals(FocusPosition(1, 5), after.position)
        assertEquals(mapOf(0 to 2, 1 to 5), after.memory)
        assertEquals(RailFocusState(), RailNavigator(listOf(0)).clamp(before))
    }

    @Test
    fun `nearest rail prefers the one above on a tie`() {
        val nav = RailNavigator(listOf(2, 0, 2))
        assertEquals(0, nav.clamp(RailFocusState(1, 0)).row)
    }

    @Test
    fun `move clamps a stale state first`() {
        val nav = RailNavigator(listOf(2, 2))
        assertEquals(FocusPosition(1, 0), nav.moved(RailFocusState(0, 9), NavDirection.Down).position)
    }

    @Test
    fun `remembered column falls back to zero`() {
        val state = RailFocusState(1, 4, mapOf(0 to 2))
        assertEquals(2, state.rememberedColumn(0))
        assertEquals(4, state.rememberedColumn(1))
        assertEquals(0, state.rememberedColumn(5))
    }

    @Test
    fun `state encodes and decodes`() {
        val state = RailFocusState(2, 3, mapOf(0 to 1, 2 to 3, 1 to 0))
        assertEquals(listOf(2, 3, 0, 1, 1, 0, 2, 3), state.encode())
        assertEquals(state, RailFocusState.decode(state.encode()))
        assertEquals(RailFocusState(), RailFocusState.decode(listOf(1)))
        assertEquals(RailFocusState(), RailFocusState.decode(listOf(1, 2, 3)))
        assertEquals(RailFocusState(), RailFocusState.decode(listOf(-1, 2)))
    }
}
