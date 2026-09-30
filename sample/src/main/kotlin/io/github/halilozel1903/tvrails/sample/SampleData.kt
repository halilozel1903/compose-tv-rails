package io.github.halilozel1903.tvrails.sample

import io.github.halilozel1903.tvrails.core.Rail
import io.github.halilozel1903.tvrails.core.RailItem
import io.github.halilozel1903.tvrails.core.RailStyle
import io.github.halilozel1903.tvrails.core.TimeFormat

/** Made up titles for the sample. All artwork is drawn by [Artwork], nothing is loaded. */
object SampleData {

    private const val MINUTE = 60_000L

    val featured: List<RailItem> = listOf(
        RailItem(
            id = "quiet-orbit",
            title = "The Quiet Orbit",
            subtitle = "2026 · Science fiction",
            description = "A lone engineer keeps a failing station running while its crew sleeps through a hundred year voyage.",
            durationMillis = 128 * MINUTE,
            badge = "NEW",
        ),
        RailItem(
            id = "lanterns-of-vell",
            title = "Lanterns of Vell",
            subtitle = "Series · Season 2",
            description = "A river town lights one lantern for every secret it keeps. This winter, the lanterns start going out.",
            durationMillis = 52 * MINUTE,
            badge = "S2",
        ),
        RailItem(
            id = "paper-mountains",
            title = "Paper Mountains",
            subtitle = "2025 · Animated adventure",
            description = "Two siblings fold a map into a mountain range and have to walk all the way home across it.",
            durationMillis = 96 * MINUTE,
        ),
        RailItem(
            id = "saltwind-kitchen",
            title = "Saltwind Kitchen",
            subtitle = "Documentary · 8 episodes",
            description = "Coastal cooks, one catch a day, and whatever the weather allows.",
            durationMillis = 38 * MINUTE,
        ),
    )

    private val continueWatching = Rail.continueWatching(
        id = "continue",
        title = "Continue watching",
        items = listOf(
            RailItem("midnight-tram", "Midnight Tram", TimeFormat.episode(1, 4), durationMillis = 47 * MINUTE, positionMillis = 31 * MINUTE),
            RailItem("copper-valley", "Copper Valley", "Western", durationMillis = 112 * MINUTE, positionMillis = 40 * MINUTE),
            RailItem("lanterns-s2e3", "Lanterns of Vell", TimeFormat.episode(2, 3), durationMillis = 52 * MINUTE, positionMillis = 9 * MINUTE),
            RailItem("glass-orchard", "The Glass Orchard", "Mystery", durationMillis = 104 * MINUTE, positionMillis = 88 * MINUTE),
            RailItem("tidepool", "Tidepool Detectives", TimeFormat.episode(3, 1), durationMillis = 24 * MINUTE, positionMillis = 6 * MINUTE),
            RailItem("finished", "Northbound Static", "Thriller", durationMillis = 99 * MINUTE, positionMillis = 99 * MINUTE),
        ),
    )

    private val trending = Rail(
        id = "trending",
        title = "Trending now",
        items = listOf(
            RailItem("velvet-circuit", "Velvet Circuit", "Heist · 2026", durationMillis = 117 * MINUTE, badge = "4K"),
            RailItem("hollow-pines", "Hollow Pines", "Horror · 2025", durationMillis = 93 * MINUTE),
            RailItem("moonlit-relay", "Moonlit Relay", "Sports drama", durationMillis = 108 * MINUTE),
            RailItem("kestrel-bay", "Signal at Kestrel Bay", "Thriller series", durationMillis = 49 * MINUTE, badge = "NEW"),
            RailItem("ashgrove", "Ashgrove Academy", "Fantasy series", durationMillis = 44 * MINUTE),
            RailItem("amber-line", "The Amber Line", "Romance · 2024", durationMillis = 101 * MINUTE),
            RailItem("low-tide", "Low Tide Radio", "Comedy", durationMillis = 22 * MINUTE),
            RailItem("far-meridian", "Far Meridian", "Space documentary", durationMillis = 58 * MINUTE),
        ),
    )

    private val topPicks = Rail(
        id = "top-picks",
        title = "Top picks for you",
        style = RailStyle.Poster,
        items = listOf(
            RailItem("poster-orbit", "The Quiet Orbit"),
            RailItem("poster-tram", "Midnight Tram"),
            RailItem("poster-paper", "Paper Mountains"),
            RailItem("poster-copper", "Copper Valley", badge = "TOP 10"),
            RailItem("poster-velvet", "Velvet Circuit"),
            RailItem("poster-pines", "Hollow Pines"),
            RailItem("poster-orchard", "The Glass Orchard"),
            RailItem("poster-ashgrove", "Ashgrove Academy"),
            RailItem("poster-amber", "The Amber Line"),
        ),
    )

    private val newReleases = Rail(
        id = "new-releases",
        title = "New releases",
        items = listOf(
            RailItem("frost-market", "Frost Market", "Drama · 2026", durationMillis = 98 * MINUTE, badge = "NEW"),
            RailItem("stillwater", "Stillwater Nine", "Crime series", durationMillis = 51 * MINUTE),
            RailItem("lumen", "Lumen Fields", "Nature", durationMillis = 46 * MINUTE),
            RailItem("brass-garden", "The Brass Garden", "Family · 2026", durationMillis = 89 * MINUTE),
            RailItem("night-ferry", "Night Ferry", "Mystery", durationMillis = 106 * MINUTE),
            RailItem("open-road", "Open Road Diaries", "Travel", durationMillis = 30 * MINUTE),
        ),
    )

    /** Row 0 is continue watching, row 1 trending, row 2 posters, row 3 new releases. */
    val rails: List<Rail> = listOf(continueWatching, trending, topPicks, newReleases)
}
