package io.github.halilozel1903.tvrails

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme

/**
 * A full screen backdrop that cross fades to the artwork of whatever is focused, with scrims that
 * keep text on the left and at the bottom readable. Put your rails in [content].
 *
 * ```kotlin
 * val railsState = rememberTvRailsState()
 * ImmersiveBackground(
 *     targetState = railsState.focusedItem(rails) ?: featured[hero.currentIndex],
 *     backdrop = { item -> Backdrop(item) },
 * ) {
 *     TvRails(rails, state = railsState, artwork = { Poster(it) })
 * }
 * ```
 *
 * @param targetState the value the backdrop shows, usually the focused [io.github.halilozel1903.tvrails.core.RailItem].
 *   A new value cross fades in.
 * @param scrimColor the color the artwork fades into, normally the screen background.
 * @param scrimStrength how opaque the scrims are, `0` (none) to `1`.
 * @param animationSpec the cross fade.
 * @param backdrop draws the artwork for a value, filling the screen.
 * @param content the screen content over the backdrop.
 */
@Composable
public fun <T> ImmersiveBackground(
    targetState: T,
    modifier: Modifier = Modifier,
    scrimColor: Color = MaterialTheme.colorScheme.background,
    scrimStrength: Float = 1f,
    animationSpec: FiniteAnimationSpec<Float> = tween(TvRailsDefaults.BackgroundFadeMillis),
    backdrop: @Composable BoxScope.(T) -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val strength = scrimStrength.coerceIn(0f, 1f)
    Box(modifier.background(scrimColor)) {
        Crossfade(
            targetState = targetState,
            modifier = Modifier.fillMaxSize(),
            animationSpec = animationSpec,
            label = "ImmersiveBackground",
        ) { value ->
            Box(Modifier.fillMaxSize()) { backdrop(value) }
        }
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        0f to scrimColor.copy(alpha = 0.92f * strength),
                        0.45f to scrimColor.copy(alpha = 0.55f * strength),
                        1f to scrimColor.copy(alpha = 0.1f * strength),
                    ),
                ),
        )
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.5f to scrimColor.copy(alpha = 0.35f * strength),
                        1f to scrimColor.copy(alpha = 0.95f * strength),
                    ),
                ),
        )
        content()
    }
}
