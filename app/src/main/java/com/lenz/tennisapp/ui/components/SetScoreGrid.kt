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

    val lastSet = sets.lastOrNull()
    val inTiebreak = isLive && parseGames(lastSet?.first ?: "") == 6 && parseGames(lastSet?.second ?: "") == 6

    val gParts = gameScore?.split("-")
    val gHome  = gParts?.getOrNull(0)?.trim()
    val gAway  = gParts?.getOrNull(1)?.trim()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End
    ) {
        // Home player row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sets.forEachIndexed { i, (homeStr, awayStr) ->
                val isCurrentSet = isLive && i == sets.size - 1
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
            if (isLive && !gHome.isNullOrBlank()) {
                Text(
                    text = gHome,
                    fontSize = gameSize,
                    color = AuraPurple,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        // Away player row
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            sets.forEachIndexed { i, (homeStr, awayStr) ->
                val isCurrentSet = isLive && i == sets.size - 1
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
            if (isLive && !gAway.isNullOrBlank()) {
                Text(
                    text = gAway,
                    fontSize = gameSize,
                    color = AuraDeep.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 2.dp)
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

    val scale by animateFloatAsState(
        targetValue = if (isWinner) 1.2f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "digit_scale"
    )

    val color = when {
        isWinner  -> AuraDeep
        isCurrent -> AuraPurple
        else      -> Color.LightGray
    }
    val weight = when {
        isWinner  -> FontWeight.Black
        isCurrent -> FontWeight.Bold
        else      -> FontWeight.Medium
    }

    Row(
        modifier = Modifier.widthIn(min = 18.dp).scale(scale),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text      = games,
            fontSize  = fontSize,
            fontWeight = weight,
            color     = color,
            textAlign = TextAlign.Center
        )
        if (tbPoints != null && !isCurrent) {
            Text(
                text = tbPoints,
                fontSize = fontSize * 0.65f,
                fontWeight = FontWeight.Bold,
                color = color.copy(alpha = 0.5f),
                modifier = Modifier.padding(top = 1.dp)
            )
        }
    }
}
