package com.lenz.tennisapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lenz.tennisapp.domain.model.TennisMatch
import timber.log.Timber

/**
 * Game breakdown chart showing how dominant each game win was.
 * Simplified and robust version with error handling.
 */
@Composable
fun GameBreakdownChart(
    match: TennisMatch,
    modifier: Modifier = Modifier
) {
    // Safety checks
    if (match.score.isNullOrEmpty()) {
        return
    }

    val games = remember(match.score) {
        try {
            extractGameDominance(match)
        } catch (e: Exception) {
            Timber.e(e, "Error extracting games from score: ${match.score}")
            emptyList()
        }
    }

    if (games.isEmpty()) {
        return
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            "Spielverlauf",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp)
        )

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(games.take(20)) { _, game ->
                GameBar(
                    setNumber = game.setNumber,
                    gameNumber = game.gameNumber,
                    winner = game.winner,
                    homePlayerName = match.homePlayer.name.split(" ").last(),
                    awayPlayerName = match.awayPlayer.name.split(" ").last()
                )
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun GameBar(
    setNumber: Int,
    gameNumber: Int,
    winner: String,  // "home" or "away"
    homePlayerName: String,
    awayPlayerName: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Label
        Text(
            "S$setNumber:$gameNumber",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(32.dp)
        )

        // Bar container
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(4.dp))
        ) {
            val barColor = if (winner == "home") Color(0xFF2E7D32) else Color(0xFFCC5500)

            if (winner == "home") {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.6f)
                        .background(barColor, RoundedCornerShape(topStart = 4.dp, bottomStart = 4.dp))
                        .align(Alignment.CenterStart)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.6f)
                        .background(barColor, RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .align(Alignment.CenterEnd)
                )
            }
        }

        // Winner name
        Text(
            if (winner == "home") homePlayerName else awayPlayerName,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (winner == "home") Color(0xFF2E7D32) else Color(0xFFCC5500),
            modifier = Modifier.width(40.dp)
        )
    }
}

// ─── Game data model ──────────────────────────────────────────────────────

private data class GameData(
    val setNumber: Int,
    val gameNumber: Int,
    val winner: String  // "home" or "away"
)

/**
 * Parse score string like "6-4,7-5,4-3" to extract games.
 * Simple and robust implementation. Returns empty list on error.
 */
private fun extractGameDominance(match: TennisMatch): List<GameData> {
    val games = mutableListOf<GameData>()

    val score = match.score ?: return emptyList()
    if (score.isEmpty()) return emptyList()

    try {
        val setScores = score.split(" ").firstOrNull() ?: return emptyList()
        val sets = setScores.split(",")

        sets.forEachIndexed { setIdx, setScore ->
            val parts = setScore.trim().split("-")
            if (parts.size != 2) return@forEachIndexed

            val homeGames = parts[0].toIntOrNull() ?: return@forEachIndexed
            val awayGames = parts[1].toIntOrNull() ?: return@forEachIndexed

            // Limit games per set to prevent memory issues
            if (homeGames > 20 || awayGames > 20) return@forEachIndexed

            val maxGames = maxOf(homeGames, awayGames)

            repeat(maxGames) { gameIdx ->
                if (gameIdx < homeGames) {
                    games.add(GameData(setIdx + 1, gameIdx + 1, "home"))
                } else if (gameIdx < awayGames) {
                    games.add(GameData(setIdx + 1, gameIdx + 1, "away"))
                }
            }
        }
    } catch (e: Exception) {
        Timber.e(e, "Error parsing score: $score")
        return emptyList()
    }

    return games
}
