package com.lenz.tennisapp.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenz.tennisapp.ui.theme.AuraDeep
import com.lenz.tennisapp.ui.theme.AuraPurple
import com.lenz.tennisapp.ui.theme.AuraLime

/**
 * Displays set-by-set scores for both players in a compact grid with physical animations.
 */
@Composable
fun SetScoreGrid(
    score: String?,
    gameScore: String? = null,
    isLive: Boolean = false,
    fontSize: TextUnit = 14.sp,
    gameSize: TextUnit = 11.sp,
    modifier: Modifier = Modifier
) {
    if (score.isNullOrBlank() || score == "-") return

    val sets = score.split(",").mapNotNull { setStr ->
        val trimmed = setStr.trim()
        if ("(" in trimmed && ")" in trimmed) {
            val dashIdx = trimmed.indexOf("-")
            if (dashIdx > 0) {
                val home = trimmed.substring(0, dashIdx).trim()
                val away = trimmed.substring(dashIdx + 1).trim()
                Pair(home, away)
            } else null
        } else {
            val p = trimmed.split("-")
            val a = p.getOrNull(0)?.trim()
            val b = p.getOrNull(1)?.trim()
            if (a != null && b != null) Pair(a, b) else null
        }
    }

    fun parseGames(scoreStr: String): Int? {
        return when {
            "(" in scoreStr -> scoreStr.substring(0, scoreStr.indexOf("(")).toIntOrNull()
            "." in scoreStr -> scoreStr.substring(0, scoreStr.indexOf(".")).toIntOrNull()
            else -> scoreStr.toIntOrNull()
        }
    }

    // When live, check if the last set is already completed (both sides resolved).
    // A set is done when one side reached ≥6 games and leads by ≥2, or either side has ≥7.
    fun isSetComplete(h: Int?, a: Int?): Boolean {
        if (h == null || a == null) return false
        return (h >= 6 && a <= 4 && h - a >= 2) ||
               (a >= 6 && h <= 4 && a - h >= 2) ||
               h >= 7 || a >= 7
    }

    val lastSet = sets.lastOrNull()
    val lastH = parseGames(lastSet?.first ?: "")
    val lastA = parseGames(lastSet?.second ?: "")
    val lastSetDone = isLive && isSetComplete(lastH, lastA)

    // If last set is complete while match is live, the new set is 0-0 — append it.
    val displaySets = if (lastSetDone) sets + Pair("0", "0") else sets

    val inTiebreak = isLive && !lastSetDone &&
            parseGames(lastSet?.first ?: "") == 6 && parseGames(lastSet?.second ?: "") == 6

    val gParts = gameScore?.split("-")
    val gHome  = gParts?.getOrNull(0)?.trim()?.ifBlank { "0" }
    val gAway  = gParts?.getOrNull(1)?.trim()?.ifBlank { "0" }
    val showGame = isLive && (gameScore != null || lastSetDone)

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Home player row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displaySets.forEachIndexed { i, (homeStr, awayStr) ->
                val isCurrentSet = isLive && i == displaySets.size - 1
                val homeNum = parseGames(homeStr)
                val awayNum = parseGames(awayStr)
                val homeWins = !isCurrentSet && homeNum != null && awayNum != null && homeNum > awayNum

                SetDigit(
                    value     = homeStr,
                    isWinner  = homeWins,
                    isCurrent = isCurrentSet,
                    fontSize  = fontSize
                )
            }
            if (showGame) {
                Text(
                    "|",
                    fontSize = gameSize,
                    color = AuraDeep.copy(alpha = 0.25f),
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = gHome ?: "0",
                    fontSize = gameSize,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Away player row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            displaySets.forEachIndexed { i, (homeStr, awayStr) ->
                val isCurrentSet = isLive && i == displaySets.size - 1
                val homeNum = parseGames(homeStr)
                val awayNum = parseGames(awayStr)
                val awayWins = !isCurrentSet && homeNum != null && awayNum != null && awayNum > homeNum

                SetDigit(
                    value     = awayStr,
                    isWinner  = awayWins,
                    isCurrent = isCurrentSet,
                    fontSize  = fontSize
                )
            }
            if (showGame) {
                Text(
                    "|",
                    fontSize = gameSize,
                    color = AuraDeep.copy(alpha = 0.25f),
                    fontWeight = FontWeight.Light,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
                Text(
                    text = gAway ?: "0",
                    fontSize = gameSize,
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(28.dp)
                )
            }
        }
    }
}

@Composable
private fun SetDigit(
    value: String,
    isWinner: Boolean,
    isCurrent: Boolean,
    fontSize: TextUnit
) {
    val games = when {
        "(" in value -> value.substring(0, value.indexOf("("))
        "." in value -> value.substring(0, value.indexOf("."))
        else -> value
    }
    val tbPoints = when {
        "(" in value -> value.substring(value.indexOf("(") + 1, value.indexOf(")"))
        "." in value -> value.substring(value.indexOf(".") + 1)
        else -> null
    }
    val hasDot = "." in value

    val scale by animateFloatAsState(
        targetValue = if (isWinner) 1.2f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "digit_scale"
    )

    val color = when {
        isWinner  -> AuraDeep
        isCurrent -> Color(0xFFE53935)
        else      -> Color.LightGray
    }
    val weight = when {
        isWinner  -> FontWeight.Black
        isCurrent -> FontWeight.Black
        else      -> FontWeight.Normal
    }

    Row(
        modifier = Modifier.widthIn(min = 18.dp).scale(scale),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text      = if (hasDot) "$games." else games,
            fontSize  = fontSize,
            fontWeight = weight,
            color     = color,
            textAlign = TextAlign.Center
        )
        if (tbPoints != null && !isCurrent) {
            Text(
                text = tbPoints,
                fontSize = fontSize * 0.6f, // Slightly smaller
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.7f)
            )
        }
    }
}
