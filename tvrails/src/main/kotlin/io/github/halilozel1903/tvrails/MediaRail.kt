package io.github.halilozel1903.tvrails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.tvrails.core.MoveResult
import io.github.halilozel1903.tvrails.core.NavDirection
import io.github.halilozel1903.tvrails.core.Rail
import io.github.halilozel1903.tvrails.core.RailFocusState
import io.github.halilozel1903.tvrails.core.RailItem
import io.github.halilozel1903.tvrails.core.RailNavigator
import io.github.halilozel1903.tvrails.core.RailStyle
import io.github.halilozel1903.tvrails.core.TimeFormat
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * A titled, horizontally scrolling row of focusable cards.
 *
 * - Focus comes back to the card that was focused last when you return to the rail with the
 *   D-pad (a focus restorer, backed by [state] so it also works after the rail was scrolled off
 *   screen and disposed).
 * - With [io.github.halilozel1903.tvrails.core.NavigationRules.wrapColumns], `Right` on the last
 *   card jumps to the first one and `Left` on the first card to the last one.
 * - Cards are drawn by [MediaCard] or, for [RailStyle.ContinueWatching], [ContinueWatchingCard].
 *
 * An empty rail draws nothing.
 *
 * @param rail the title and items.
 * @param state the screen's shared [TvRailsState].
 * @param rowIndex this rail's index in the screen, used for focus memory and [TvRailsState.requestFocus].
 * @param onItemClick called when a card is clicked.
 * @param contentPadding padding of the row; keep some vertical room for the focus scale.
 * @param timeLabels unit words for continue watching cards.
 * @param artwork draws a card's picture.
 */
@Composable
public fun MediaRail(
    rail: Rail,
    modifier: Modifier = Modifier,
    state: TvRailsState = rememberTvRailsState(),
    rowIndex: Int = 0,
    onItemClick: (RailItem) -> Unit = {},
    contentPadding: PaddingValues = TvRailsDefaults.RailContentPadding,
    timeLabels: TimeFormat.Labels = TimeFormat.Labels(),
    artwork: @Composable BoxScope.(RailItem) -> Unit,
) {
    if (rail.isEmpty) return
    val scope = rememberCoroutineScope()
    val rememberedColumn by remember(state, rowIndex) { derivedStateOf { state.rememberedColumn(rowIndex) } }
    // A new row starts scrolled so the remembered card is visible, with up to two cards before it.
    val listState = remember(state, rail.id) {
        state.rowListState(rail.id, (rememberedColumn.coerceIn(0, rail.size - 1) - 2).coerceAtLeast(0))
    }
    val requesters = remember(rail.id) { HashMap<String, FocusRequester>() }
    val fallback = remember { FocusRequester() }
    val navigator = remember(rail.size, state.rules) { RailNavigator(listOf(rail.size), state.rules) }
    // The focus restorer's fallback must point at a composed card: the remembered one when it is
    // on screen, otherwise the first visible card.
    val fallbackColumn by remember(listState, rail.size) {
        derivedStateOf {
            val wanted = rememberedColumn.coerceIn(0, rail.size - 1)
            val visible = listState.layoutInfo.visibleItemsInfo
            if (visible.isEmpty() || visible.any { it.index == wanted }) {
                wanted
            } else {
                listState.firstVisibleItemIndex.coerceIn(0, rail.size - 1)
            }
        }
    }

    suspend fun focusColumn(column: Int) {
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == column }) listState.scrollToItem(column)
        val id = rail.items[column].id
        // The card may need a frame or two to be composed after scrolling.
        repeat(10) {
            withFrameNanos { }
            val requester = requesters[id]
            if (requester != null && runCatching { requester.requestFocus() }.isSuccess) return
        }
    }

    val pending = state.pendingFocus
    LaunchedEffect(pending, rowIndex) {
        if (pending == null || pending.row != rowIndex) return@LaunchedEffect
        withFrameNanos { }
        focusColumn(pending.column.coerceIn(0, rail.size - 1))
        state.consumePendingFocus(pending)
    }

    Column(modifier.onFocusChanged { if (!it.hasFocus) state.onRailFocusLost(rowIndex) }) {
        SectionTitle(
            text = rail.title,
            modifier = Modifier.padding(horizontal = TvRailsDefaults.HorizontalPadding),
            trailing = null,
        )
        Spacer(Modifier.height(6.dp))
        LazyRow(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (state.rules.rememberColumns) Modifier.focusRestorer(fallback) else Modifier)
                .onPreviewKeyEvent { event ->
                    if (!state.rules.wrapColumns || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val direction = when (event.key) {
                        Key.DirectionRight -> NavDirection.Right
                        Key.DirectionLeft -> NavDirection.Left
                        else -> return@onPreviewKeyEvent false
                    }
                    val current = state.focus.takeIf { state.isFocused && it.row == rowIndex }?.column
                        ?: return@onPreviewKeyEvent false
                    val moved = navigator.move(RailFocusState(0, current), direction) as? MoveResult.Moved
                        ?: return@onPreviewKeyEvent false
                    val target = moved.state.column
                    // Neighbours are left to the platform's focus search; only wrap jumps are handled here.
                    if (abs(target - current) <= 1) return@onPreviewKeyEvent false
                    scope.launch { focusColumn(target) }
                    true
                },
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(TvRailsDefaults.CardSpacing),
        ) {
            itemsIndexed(
                items = rail.items,
                key = { _, item -> item.id },
                contentType = { _, _ -> rail.style },
            ) { column, item ->
                val requester = remember { FocusRequester() }
                DisposableEffect(item.id, requester) {
                    requesters[item.id] = requester
                    onDispose { if (requesters[item.id] === requester) requesters.remove(item.id) }
                }
                val itemModifier = Modifier
                    .focusRequester(requester)
                    .then(if (column == fallbackColumn) Modifier.focusRequester(fallback) else Modifier)
                    .onFocusChanged { if (it.hasFocus) state.onItemFocused(rowIndex, column) }
                if (rail.style == RailStyle.ContinueWatching) {
                    ContinueWatchingCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = itemModifier,
                        labels = timeLabels,
                        artwork = artwork,
                    )
                } else {
                    MediaCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        modifier = itemModifier,
                        style = rail.style,
                        artwork = artwork,
                    )
                }
            }
        }
    }
}
