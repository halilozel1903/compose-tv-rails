package io.github.halilozel1903.tvrails

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.halilozel1903.tvrails.core.RailStyle

/** Sizes, shapes and timings used by the compose-tv-rails components. */
public object TvRailsDefaults {

    /** Horizontal safe area on TV screens (about 5% overscan of a 960 dp wide screen). */
    public val HorizontalPadding: Dp = 48.dp

    /** Padding of a rail's row: the safe area on both sides, and room for the focus scale. */
    public val RailContentPadding: PaddingValues = PaddingValues(horizontal = HorizontalPadding, vertical = 10.dp)

    /** Space between cards in a rail. */
    public val CardSpacing: Dp = 16.dp

    /** Space between rails. */
    public val RailSpacing: Dp = 20.dp

    /** Width of a portrait poster card. */
    public val PosterWidth: Dp = 124.dp

    /** Width of a landscape or continue watching card. */
    public val LandscapeWidth: Dp = 208.dp

    /** Portrait card ratio, width / height. */
    public const val PosterAspectRatio: Float = 2f / 3f

    /** Landscape card ratio, width / height. */
    public const val LandscapeAspectRatio: Float = 16f / 9f

    /** Scale of a focused card. */
    public const val FocusedScale: Float = 1.1f

    /** Corner shape of cards. */
    public val CardShape: Shape = RoundedCornerShape(12.dp)

    /** Corner shape of the hero carousel. */
    public val HeroShape: Shape = RoundedCornerShape(20.dp)

    /** Crossfade duration of [ImmersiveBackground]. */
    public const val BackgroundFadeMillis: Int = 600

    /** Fade duration between hero slides. */
    public const val HeroFadeMillis: Int = 500

    /** The card width for a rail [style]. */
    public fun cardWidth(style: RailStyle): Dp = if (style == RailStyle.Poster) PosterWidth else LandscapeWidth

    /** The card aspect ratio for a rail [style]. */
    public fun aspectRatio(style: RailStyle): Float =
        if (style == RailStyle.Poster) PosterAspectRatio else LandscapeAspectRatio
}
