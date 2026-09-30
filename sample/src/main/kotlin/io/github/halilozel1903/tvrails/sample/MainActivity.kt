package io.github.halilozel1903.tvrails.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.darkColorScheme

/**
 * D-pad presses can't be timed reliably through adb on a fresh emulator, so
 * `scripts/screenshots.sh` starts the app with `--es scene <scene>` to set up each screenshot:
 *
 * - `home`: the hero carousel is focused (same as no extra)
 * - `focused-rail`: the third card of the "Trending now" rail is focused, with its backdrop
 * - `continue`: the second card of the "Continue watching" rail is focused
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scene = Scene.from(intent.getStringExtra(EXTRA_SCENE))
        setContent {
            MaterialTheme(colorScheme = SampleColors) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    shape = RectangleShape,
                    colors = SurfaceDefaults.colors(containerColor = MaterialTheme.colorScheme.background),
                ) {
                    SampleApp(scene)
                }
            }
        }
    }

    companion object {
        const val EXTRA_SCENE = "scene"
    }
}

private val SampleColors = darkColorScheme(
    primary = Color(0xFFFFB86B),
    onPrimary = Color(0xFF3A1D00),
    secondary = Color(0xFF9ECBFF),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFF1F3F8),
    surface = Color(0xFF0B0E14),
    onSurface = Color(0xFFF1F3F8),
    surfaceVariant = Color(0xFF1C2230),
    onSurfaceVariant = Color(0xFFA7B0C0),
)
