package com.lenz.tennisapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenz.tennisapp.domain.model.H2HResult
import com.lenz.tennisapp.domain.model.Surface
import com.lenz.tennisapp.ui.theme.*

@Composable
fun H2HCard(
    h2h: H2HResult,
    modifier: Modifier = Modifier
) {
    val total = h2h.player1Wins + h2h.player2Wins

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            Spacer(Modifier.height(20.dp))

            val p1Frac = if (total > 0) h2h.player1Wins.toFloat() / total else 0.5f
            val p1Animated by animateFloatAsState(
                targetValue = p1Frac.coerceIn(0.05f, 0.95f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                ),
                label = "h2h_bar"
            )

            // ── Big score display ────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Player 1
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        h2h.player1Wins.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        h2h.player1Name.split(" ").last(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Middle dash
                Text(
                    "–",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Player 2
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        h2h.player2Wins.toString(),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        h2h.player2Name.split(" ").last(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Percentage labels and bar only if there's actually a match history
            if (total > 0) {
                Spacer(Modifier.height(16.dp))

                // ── Segmented bar ────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(CircleShape)
                ) {
                    // Background (p2 colour)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                    )
                    // P1 fill
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(p1Animated)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                )
                            )
                    )
                }

                // Percentage labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(p1Frac * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${((1 - p1Frac) * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                // Just a bit of space if no bar is shown
                Spacer(Modifier.height(8.dp))
            }

            // ── Recent matches ────────────────────────────────────────────
            if (h2h.recentMatches.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))

                Text(
                    "Letzte Begegnungen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(10.dp))

                h2h.recentMatches.forEach { match ->
                    val isP1Winner = match.winner == h2h.player1Name
                    val winnerColor = if (isP1Winner)
                        MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary

                    val surfaceColor = when (match.surface) {
                        Surface.CLAY        -> ClayColor
                        Surface.GRASS       -> GrassColor
                        Surface.HARD        -> HardColor
                        Surface.INDOOR_HARD -> IndoorColor
                        else                -> MaterialTheme.colorScheme.primary
                    }

                    val surfaceName = when (match.surface) {
                        Surface.CLAY        -> "Sand"
                        Surface.GRASS       -> "Rasen"
                        Surface.HARD        -> "Hartplatz"
                        Surface.INDOOR_HARD -> "Hartplatz (H)"
                        else                -> match.surface.displayName
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        tonalElevation = 1.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Date
                            Text(
                                match.date.take(7),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp)
                            )
                            // Surface name with color
                            Text(
                                surfaceName,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = surfaceColor,
                                modifier = Modifier.padding(horizontal = 4.dp).width(64.dp)
                            )
                            // Winner name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(shape = CircleShape, color = winnerColor) {
                                    Text(
                                        "W",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                                Text(
                                    match.winner.split(" ").last(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = winnerColor
                                )
                            }
                            // Score
                            Text(
                                match.score,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(64.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
