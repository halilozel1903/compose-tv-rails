package io.github.halilozel1903.tvrails

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.tvrails.core.RailItem
import io.github.halilozel1903.tvrails.core.RailStyle
import io.github.halilozel1903.tvrails.core.TimeFormat

/**
 * A focusable media card: the [artwork] in a rounded frame that scales up, glows and gets a border
 * when focused, with the title below it.
 *
 * @param item the title to show.
 * @param onClick called on D-pad center or enter.
 * @param style [RailStyle.Poster] draws a 2:3 poster, the other styles a 16:9 thumbnail.
 * @param width the card width; the height follows from the style's aspect ratio.
 * @param showTitle draw [RailItem.title] (and the subtitle) below the card.
 * @param interactionSource observe focus and presses from outside.
 * @param artwork draws the picture, filling the card. Load an image here or draw a placeholder.
 */
@Composable
public fun MediaCard(
    item: RailItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: RailStyle = RailStyle.Landscape,
    width: Dp = TvRailsDefaults.cardWidth(style),
    showTitle: Boolean = true,
    interactionSource: MutableInteractionSource? = null,
    artwork: @Composable BoxScope.(RailItem) -> Unit,
) {
    RailCard(
        item = item,
        onClick = onClick,
        modifier = modifier,
        width = width,
        aspectRatio = TvRailsDefaults.aspectRatio(style),
        interactionSource = interactionSource,
        artwork = artwork,
        overlay = {},
        footer = { focused ->
            if (showTitle) {
                CardTitle(item.title, focused)
                if (item.subtitle.isNotEmpty() && style != RailStyle.Poster) CardSubtitle(item.subtitle)
            }
        },
    )
}

/**
 * A 16:9 card for titles the viewer has started: the [artwork] with a progress bar along the
 * bottom edge, and the title and time left (`"32m left"`) below it.
 *
 * @param labels unit words for the time left, to translate it.
 */
@Composable
public fun ContinueWatchingCard(
    item: RailItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = TvRailsDefaults.LandscapeWidth,
    labels: TimeFormat.Labels = TimeFormat.Labels(),
    interactionSource: MutableInteractionSource? = null,
    artwork: @Composable BoxScope.(RailItem) -> Unit,
) {
    val remaining = TimeFormat.remaining(item.positionMillis, item.durationMillis, labels)
    RailCard(
        item = item,
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = "${item.title}, $remaining" },
        width = width,
        aspectRatio = TvRailsDefaults.LandscapeAspectRatio,
        interactionSource = interactionSource,
        artwork = artwork,
        overlay = {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))),
            )
            WatchProgressBar(
                progress = item.progress,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            )
        },
        footer = { focused ->
            CardTitle(item.title, focused)
            CardSubtitle(TimeFormat.joinMeta(item.subtitle, remaining))
        },
    )
}

/**
 * A thin rounded progress bar, used by [ContinueWatchingCard].
 *
 * @param progress the filled fraction; values outside `0..1` are clamped.
 */
@Composable
public fun WatchProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = Color.White.copy(alpha = 0.3f),
    height: Dp = 4.dp,
) {
    Box(
        modifier
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun RailCard(
    item: RailItem,
    onClick: () -> Unit,
    modifier: Modifier,
    width: Dp,
    aspectRatio: Float,
    interactionSource: MutableInteractionSource?,
    artwork: @Composable BoxScope.(RailItem) -> Unit,
    overlay: @Composable BoxScope.() -> Unit,
    footer: @Composable (focused: Boolean) -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val colors = MaterialTheme.colorScheme
    Column(modifier.width(width)) {
        Card(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio),
            shape = CardDefaults.shape(shape = TvRailsDefaults.CardShape),
            colors = CardDefaults.colors(containerColor = colors.surfaceVariant),
            scale = CardDefaults.scale(focusedScale = TvRailsDefaults.FocusedScale),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(width = 3.dp, color = colors.onSurface),
                    shape = TvRailsDefaults.CardShape,
                ),
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = colors.primary.copy(alpha = 0.55f), elevation = 18.dp),
            ),
            interactionSource = source,
        ) {
            Box(Modifier.fillMaxSize()) {
                artwork(item)
                overlay()
                val badge = item.badge
                if (badge != null) Badge(badge, Modifier.align(Alignment.TopStart).padding(8.dp))
            }
        }
        Spacer(Modifier.height(if (focused) 14.dp else 10.dp))
        footer(focused)
    }
}

@Composable
private fun Badge(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.primary)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimary,
        maxLines = 1,
    )
}

@Composable
private fun CardTitle(text: String, focused: Boolean) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = if (focused) FontWeight.SemiBold else FontWeight.Medium,
        color = if (focused) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
private fun CardSubtitle(text: String) {
    if (text.isEmpty()) return
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
