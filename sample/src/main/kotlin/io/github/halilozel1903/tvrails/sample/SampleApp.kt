package io.github.halilozel1903.tvrails.sample

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import io.github.halilozel1903.tvrails.HeroCarousel
import io.github.halilozel1903.tvrails.ImmersiveBackground
import io.github.halilozel1903.tvrails.TvRails
import io.github.halilozel1903.tvrails.TvRailsDefaults
import io.github.halilozel1903.tvrails.core.FocusPosition
import io.github.halilozel1903.tvrails.core.RailItem
import io.github.halilozel1903.tvrails.core.RailStyle
import io.github.halilozel1903.tvrails.rememberHeroCarouselState
import io.github.halilozel1903.tvrails.rememberTvRailsState

/** The screenshot scenes, see [MainActivity]. */
enum class Scene(val id: String, val initialFocus: FocusPosition?) {
    Home("home", null),
    FocusedRail("focused-rail", FocusPosition(row = 1, column = 2)),
    Continue("continue", FocusPosition(row = 0, column = 1)),
    ;

    companion object {
        fun from(id: String?): Scene = entries.firstOrNull { it.id == id } ?: Home
    }
}

@Composable
fun SampleApp(scene: Scene) {
    val context = LocalContext.current
    val rails = SampleData.rails
    val featured = SampleData.featured
    val railsState = rememberTvRailsState(initialFocus = scene.initialFocus)
    val heroState = rememberHeroCarouselState(itemCount = featured.size, intervalMillis = 9_000L)
    val heroFocus = remember { FocusRequester() }
    val onClick: (RailItem) -> Unit = { item ->
        Toast.makeText(context, "Play ${item.title}", Toast.LENGTH_SHORT).show()
    }

    if (scene.initialFocus == null) {
        LaunchedEffect(Unit) {
            withFrameNanos { }
            runCatching { heroFocus.requestFocus() }
        }
    }

    val backdropItem = railsState.focusedItem(rails) ?: featured[heroState.currentIndex]
    ImmersiveBackground(
        targetState = backdropItem,
        modifier = Modifier.fillMaxSize(),
        backdrop = { item -> Artwork(item) },
    ) {
        TvRails(
            rails = rails,
            modifier = Modifier.fillMaxSize(),
            state = railsState,
            onItemClick = onClick,
            header = {
                Column {
                    TopBar()
                    HeroCarousel(
                        items = featured,
                        modifier = Modifier
                            .padding(horizontal = TvRailsDefaults.HorizontalPadding)
                            .fillMaxWidth()
                            .height(280.dp)
                            .focusRequester(heroFocus),
                        state = heroState,
                        onItemClick = onClick,
                        artwork = { item -> Artwork(item) },
                    )
                }
            },
            artwork = { item ->
                // Posters carry their title in the picture, as real key art does.
                val poster = rails.any { rail -> rail.style == RailStyle.Poster && item in rail.items }
                Artwork(item, text = if (poster) ArtText.Title else ArtText.None)
            },
        )
    }
}

@Composable
private fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = TvRailsDefaults.HorizontalPadding)
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text(
            text = "TV Rails",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(4.dp))
        for ((index, tab) in listOf("Home", "Movies", "Series", "Kids").withIndex()) {
            Text(
                text = tab,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (index == 0) FontWeight.SemiBold else FontWeight.Normal,
                color = if (index == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
