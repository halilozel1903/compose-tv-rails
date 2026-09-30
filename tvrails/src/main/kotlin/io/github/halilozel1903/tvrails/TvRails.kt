package io.github.halilozel1903.tvrails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.tvrails.core.Rail
import io.github.halilozel1903.tvrails.core.RailItem
import io.github.halilozel1903.tvrails.core.TimeFormat

/**
 * A vertically scrolling screen of [MediaRail]s with an optional [header] on top, usually a
 * [HeroCarousel].
 *
 * ```kotlin
 * val railsState = rememberTvRailsState()
 * TvRails(
 *     rails = rails,
 *     state = railsState,
 *     onItemClick = { play(it) },
 *     header = { HeroCarousel(featured, Modifier.height(300.dp), artwork = { Poster(it) }) },
 *     artwork = { Poster(it) },
 * )
 * ```
 *
 * @param rails the rails, top to bottom. Their indices are the `row` of [TvRailsState].
 * @param state the shared focus state, see [rememberTvRailsState].
 * @param listState the vertical scroll state.
 * @param onItemClick called when a card is clicked.
 * @param contentPadding padding of the whole column.
 * @param header content above the first rail, scrolled with it.
 * @param artwork draws a card's picture.
 */
@Composable
public fun TvRails(
    rails: List<Rail>,
    modifier: Modifier = Modifier,
    state: TvRailsState = rememberTvRailsState(),
    listState: LazyListState = rememberLazyListState(),
    onItemClick: (RailItem) -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(top = 24.dp, bottom = 48.dp),
    timeLabels: TimeFormat.Labels = TimeFormat.Labels(),
    header: (@Composable () -> Unit)? = null,
    artwork: @Composable BoxScope.(RailItem) -> Unit,
) {
    val headerCount = if (header != null) 1 else 0
    val pending = state.pendingFocus
    LaunchedEffect(pending, headerCount) {
        if (pending == null || pending.row !in rails.indices) return@LaunchedEffect
        // Compose the target rail first; the rail itself then moves focus onto the card.
        withFrameNanos { }
        val index = pending.row + headerCount
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.scrollToItem(index)
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(TvRailsDefaults.RailSpacing),
    ) {
        if (header != null) {
            item(key = HeaderKey, contentType = HeaderKey) { header() }
        }
        itemsIndexed(
            items = rails,
            key = { _, rail -> rail.id },
            contentType = { _, rail -> rail.style },
        ) { row, rail ->
            MediaRail(
                rail = rail,
                state = state,
                rowIndex = row,
                onItemClick = onItemClick,
                timeLabels = timeLabels,
                artwork = artwork,
            )
        }
    }
}

private const val HeaderKey = "io.github.halilozel1903.tvrails.header"
