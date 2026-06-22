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
                    val p1Color = MaterialTheme.colorScheme.primary
                    val p2Color = MaterialTheme.colorScheme.secondary

                    val surfaceColor = when (match.surface) {
                        Surface.CLAY        -> ClayColor
                        Surface.GRASS       -> GrassColor
                        Surface.HARD        -> HardColor
                        Surface.INDOOR_HARD -> IndoorColor
                        else                -> MaterialTheme.colorScheme.primary
                    }
                    val surfaceChar = when (match.surface) {
                        Surface.CLAY        -> "C"
                        Surface.GRASS       -> "G"
                        Surface.INDOOR_HARD -> "I"
                        else                -> "H"
                    }

                    // Build per-set score pairs for display
                    val hasSetScores = match.p1Scores.isNotEmpty() && match.p2Scores.isNotEmpty()
                    val setCount = maxOf(match.p1Scores.size, match.p2Scores.size)

                    ElevatedCard(
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            // Top row: year + round + tournament + surface badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    match.date.take(4),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                                if (match.round.isNotBlank() && match.round != "-") {
                                    Text(
                                        match.round,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    match.tournament,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = surfaceColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        surfaceChar,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = surfaceColor,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            // Score rows: one row per player
                            if (hasSetScores) {
                                @Composable
                                fun PlayerScoreRow(
                                    name: String,
                                    sets: Int,
                                    scores: List<String>,
                                    opponentSets: Int,
                                    opponentScores: List<String>,
                                    isWinner: Boolean,
                                    color: Color
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            name.split(" ").last(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isWinner) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            for (i in 0 until setCount) {
                                                val sRaw = scores.getOrNull(i) ?: ""
                                                val oppRaw = opponentScores.getOrNull(i) ?: ""
                                                val s = stripTiebreak(sRaw)
                                                val opp = stripTiebreak(oppRaw)
                                                
                                                val wonSet = s.isNotEmpty() && opp.isNotEmpty() && run {
                                                    val mine = s.toIntOrNull()
                                                    val theirs = opp.toIntOrNull()
                                                    mine != null && theirs != null && mine > theirs
                                                }
                                                Text(
                                                    s,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = if (wonSet) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (wonSet) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.width(18.dp),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                                PlayerScoreRow(
                                    name = h2h.player1Name,
                                    sets = match.p1Sets,
                                    scores = match.p1Scores,
                                    opponentSets = match.p2Sets,
                                    opponentScores = match.p2Scores,
                                    isWinner = isP1Winner,
                                    color = p1Color
                                )
                                Spacer(Modifier.height(2.dp))
                                PlayerScoreRow(
                                    name = h2h.player2Name,
                                    sets = match.p2Sets,
                                    scores = match.p2Scores,
                                    opponentSets = match.p1Sets,
                                    opponentScores = match.p1Scores,
                                    isWinner = !isP1Winner,
                                    color = p2Color
                                )
                            } else {
                                Text(
                                    formatH2HScore(match.score),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

private fun stripTiebreak(score: String): String {
    return score.substringBefore("(").trim()
}

private fun formatH2HScore(score: String): String {
    return score.replace(Regex("\\([^)]*\\)"), "").trim()
}
