package com.lenz.tennisapp.ui.theme

import androidx.compose.ui.graphics.Color
import com.lenz.tennisapp.domain.model.Surface

/**
 * Three court surfaces used as hero background images throughout the app.
 * The session court is picked randomly at app startup and never changes
 * until the app is killed and reopened.
 */
enum class CourtType(
    /** Unsplash photo – loads via Coil. Gradient is used as fallback/placeholder. */
    val imageUrl: String,
    /** Dark overlay applied on top of the photo so white text is always readable. */
    val overlayAlpha: Float,
    /** Gradient shown while the image is loading or if the URL fails. */
    val gradientFallback: List<Color>
) {
    CLAY(
        imageUrl        = "https://images.unsplash.com/photo-1499510318569-1a3d67dc3976?ixlib=rb-4.1.0&q=85&fm=jpg&crop=entropy&cs=srgb&w=1080",
        overlayAlpha    = 0.50f,
        gradientFallback = listOf(Color(0xFF8B3A0F), Color(0xFFCC5500), Color(0xFFE07840))
    ),
    HARD(
        imageUrl        = "https://images.unsplash.com/photo-1547934045-2942d193cb49?ixlib=rb-4.1.0&q=85&fm=jpg&crop=entropy&cs=srgb&w=1080",
        overlayAlpha    = 0.50f,
        gradientFallback = listOf(Color(0xFF0D3B7A), Color(0xFF1565C0), Color(0xFF1976D2))
    ),
    GRASS(
        imageUrl        = "https://images.unsplash.com/photo-1554068865-24cecd4e34b8?ixlib=rb-4.1.0&q=85&fm=jpg&crop=entropy&cs=srgb&w=1080",
        overlayAlpha    = 0.48f,
        gradientFallback = listOf(Color(0xFF0D2B10), Color(0xFF1E6B22), Color(0xFF4CAF50))
    );
}

/** Maps a match surface to the appropriate court photo. */
fun Surface.toCourtType() = when (this) {
    Surface.CLAY        -> CourtType.CLAY
    Surface.GRASS       -> CourtType.GRASS
    Surface.HARD,
    Surface.INDOOR_HARD -> CourtType.HARD
    Surface.CLAY -> CourtType.CLAY
    else                -> CourtType.HARD
}
