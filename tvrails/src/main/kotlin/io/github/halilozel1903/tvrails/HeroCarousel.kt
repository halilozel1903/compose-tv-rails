package io.github.halilozel1903.tvrails

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import io.github.halilozel1903.tvrails.core.RailItem
import io.github.halilozel1903.tvrails.core.TimeFormat

/**
 * A large, focusable banner that cycles through featured [items].
 *
 * - Slides advance on their own every [HeroCarouselState]'s interval (with a progress pill in the
 *   indicator) and cross fade.
 * - D-pad `Right` shows the next slide (wrapping when the state loops) and `Left` the previous
 *   one. `Left` on the first slide is not consumed, so focus can leave the carousel, for example
 *   into a navigation drawer. Manual moves restart the slide's clock.
 * - D-pad center or enter calls [onItemClick] with the slide on screen.
 *
 * @param items the featured titles.
 * @param state the slide and clock, see [rememberHeroCarouselState].
 * @param autoAdvance advance slides on a timer.
 * @param pauseWhenFocused hold the current slide while the carousel has focus.
 * @param shape the banner's shape.
 * @param artwork draws a slide's full bleed artwork.
 * @param content draws text and actions over the artwork; defaults to [HeroCarouselDefaults.Content].
 */
@Composable
public fun HeroCarousel(
    items: List<RailItem>,
    modifier: Modifier = Modifier,
    state: HeroCarouselState = rememberHeroCarouselState(items.size),
    onItemClick: (RailItem) -> Unit = {},
    autoAdvance: Boolean = true,
    pauseWhenFocused: Boolean = false,
    shape: Shape = TvRailsDefaults.HeroShape,
    artwork: @Composable BoxScope.(RailItem) -> Unit,
    content: @Composable BoxScope.(RailItem) -> Unit = { item -> HeroCarouselDefaults.Content(item) },
) {
    LaunchedEffect(state, items.size) { state.updateItemCount(items.size) }
    if (items.isEmpty()) return

    var focused by remember { mutableStateOf(false) }
    val ticking = autoAdvance && !(pauseWhenFocused && focused)
    LaunchedEffect(state, ticking) {
        if (!ticking) return@LaunchedEffect
        var last = withFrameMillis { it }
        while (true) {
            val now = withFrameMillis { it }
            state.tick(now - last)
            last = now
        }
    }

    val colors = MaterialTheme.colorScheme
    val current = items[state.currentIndex.coerceIn(0, items.lastIndex)]
    Surface(
        onClick = { onItemClick(items[state.currentIndex.coerceIn(0, items.lastIndex)]) },
        modifier = modifier
            .semantics { contentDescription = current.title }
            .onFocusChanged { focused = it.hasFocus }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val timeline = state.timeline
                when (event.key) {
                    Key.DirectionRight -> if (!timeline.isLast || timeline.loop) {
                        state.next()
                        true
                    } else {
                        false
                    }
                    Key.DirectionLeft -> if (!timeline.isFirst) {
                        state.previous()
                        true
                    } else {
                        false
                    }
                    else -> false
                }
            },
        shape = ClickableSurfaceDefaults.shape(shape = shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = colors.surfaceVariant,
            focusedContainerColor = colors.surfaceVariant,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(3.dp, colors.onSurface), shape = shape),
        ),
    ) {
        AnimatedContent(
            targetState = state.currentIndex,
            modifier = Modifier.fillMaxSize(),
            transitionSpec = {
                fadeIn(tween(TvRailsDefaults.HeroFadeMillis)) togetherWith fadeOut(tween(TvRailsDefaults.HeroFadeMillis))
            },
            label = "HeroCarousel",
        ) { index ->
            val item = items.getOrNull(index) ?: return@AnimatedContent
            Box(Modifier.fillMaxSize()) {
                artwork(item)
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                0f to Color.Black.copy(alpha = 0.78f),
                                0.55f to Color.Black.copy(alpha = 0.25f),
                                1f to Color.Transparent,
                            ),
                        ),
                )
                content(item)
            }
        }
        if (items.size > 1) {
            HeroCarouselIndicator(
                state = state,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 28.dp, bottom = 24.dp),
            )
        }
    }
}

/**
 * Dots for the slides of a [HeroCarousel]; the current one is a pill that fills up until the next
 * slide.
 */
@Composable
public fun HeroCarouselIndicator(
    state: HeroCarouselState,
    modifier: Modifier = Modifier,
    activeColor: Color = Color.White,
    inactiveColor: Color = Color.White.copy(alpha = 0.4f),
    dotSize: Dp = 8.dp,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(state.itemCount) { index ->
            val active = index == state.currentIndex
            val width by animateDpAsState(if (active) dotSize * 4 else dotSize, label = "HeroCarouselDot")
            Box(
                Modifier
                    .size(width = width, height = dotSize)
                    .clip(CircleShape)
                    .background(inactiveColor)
                    .then(
                        if (active) {
                            Modifier.drawBehind {
                                drawRoundRect(
                                    color = activeColor,
                                    size = Size(size.width * state.progress, size.height),
                                    cornerRadius = CornerRadius(size.height / 2f),
                                )
                            }
                        } else {
                            Modifier
                        },
                    ),
            )
        }
    }
}

/** Default slide content of [HeroCarousel]. */
public object HeroCarouselDefaults {

    /**
     * The badge, title, subtitle, description and a play hint, bottom left over the artwork.
     *
     * @param actionLabel the text of the play hint, or `null` to hide it.
     */
    @Composable
    public fun Content(
        item: RailItem,
        modifier: Modifier = Modifier,
        actionLabel: String? = "Play",
    ) {
        Box(modifier.fillMaxSize()) {
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(0.55f)
                    .padding(start = 36.dp, bottom = 32.dp, end = 16.dp),
            ) {
                val badge = item.badge
                if (badge != null) {
                    Text(
                        text = badge,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.height(10.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val meta = TimeFormat.joinMeta(
                    item.subtitle,
                    if (item.durationMillis > 0L) TimeFormat.duration(item.durationMillis) else null,
                )
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = meta,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                    )
                }
                if (item.description.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = item.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (actionLabel != null) {
                    Spacer(Modifier.height(16.dp))
                    PlayHint(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun PlayHint(label: String) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color.White)
            .padding(start = 14.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(12.dp)
                .drawBehind {
                    val path = Path().apply {
                        moveTo(0f, 0f)
                        lineTo(size.width, size.height / 2f)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(path, Color.Black)
                },
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.Black,
        )
    }
}
