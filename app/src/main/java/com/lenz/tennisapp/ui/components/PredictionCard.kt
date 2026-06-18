package com.lenz.tennisapp.ui.components

import androidx.compose.animation.core.*
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
import androidx.compose.ui.unit.dp
import com.lenz.tennisapp.domain.model.MatchPrediction
import com.lenz.tennisapp.domain.model.PredictionConfidence
import com.lenz.tennisapp.ui.theme.TennisGreen
import com.lenz.tennisapp.ui.theme.TennisGreenLight
import com.lenz.tennisapp.ui.theme.ExpressiveSpring

@Composable
fun PredictionCard(
    prediction: MatchPrediction,
    player1Name: String,
    player2Name: String,
    /** Market-implied win probability for player 1 (from bookmaker odds). null = no odds available. */
    marketProbP1: Float? = null,
    modifier: Modifier = Modifier
) {
    val p1Animated by animateFloatAsState(
        targetValue = prediction.player1WinProbability,
        animationSpec = ExpressiveSpring,
        label = "p1_prob"
    )

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Player names + percentages
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        player1Name.split(" ").last(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${(p1Animated * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Favourite indicator
                val leader = if (p1Animated >= 0.5f) player1Name.split(" ").last()
                             else player2Name.split(" ").last()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Favorit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Normal
                    )
                    Text(
                        leader,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        player2Name.split(" ").last(),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${((1 - p1Animated) * 100).toInt()}%",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Animated probability bar — full gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .clip(CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth(p1Animated)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            }

            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${(p1Animated * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${((1 - p1Animated) * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Key factors
            if (prediction.factors.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Entscheidende Faktoren",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                prediction.factors.forEach { factor ->
                    FactorRow(
                        factor       = factor,
                        player1Name  = player1Name.split(" ").last(),
                        player2Name  = player2Name.split(" ").last()
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }

            // ── Bookmaker comparison ──────────────────────────────────────
            if (marketProbP1 != null) {
                val marketAnimated by animateFloatAsState(
                    targetValue = marketProbP1,
                    animationSpec = ExpressiveSpring,
                    label = "market_prob"
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Wettmarkt-Wahrscheinlichkeit",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${(marketProbP1 * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "${((1 - marketProbP1) * 100).toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier.fillMaxWidth().height(10.dp).clip(CircleShape)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(marketAnimated)
                            .fillMaxHeight()
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                    )
                }
                Spacer(Modifier.height(6.dp))
                // Divergence indicator
                val diff = kotlin.math.abs(prediction.player1WinProbability - marketProbP1) * 100
                if (diff >= 5f) {
                    val aiHigher = prediction.player1WinProbability > marketProbP1
                    Text(
                        buildString {
                            append("KI sieht ")
                            append(if (aiHigher) player1Name.split(" ").last() else player2Name.split(" ").last())
                            append(" ${diff.toInt()}% stärker als der Markt")
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ConfidenceBadge(confidence: PredictionConfidence) {
    val (label, color) = when (confidence) {
        PredictionConfidence.HIGH   -> "Hoch"    to Color(0xFF1E8E3E)
        PredictionConfidence.MEDIUM -> "Mittel"  to Color(0xFFF9A825)
        PredictionConfidence.LOW    -> "Niedrig" to Color(0xFF9E9E9E)
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.15f)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun FactorRow(
    factor: com.lenz.tennisapp.domain.model.PredictionFactor,
    player1Name: String,
    player2Name: String
) {
    val favoredName = if (factor.favoredPlayer == 1) player1Name else player2Name
    val barColor    = if (factor.favoredPlayer == 1)
        MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.secondary

    val strengthAnimated by animateFloatAsState(
        targetValue = factor.strength,
        animationSpec = ExpressiveSpring,
        label = "factor_${factor.label}"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            factor.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(barColor.copy(alpha = 0.18f))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(strengthAnimated)
                    .fillMaxHeight()
                    .clip(CircleShape)
                    .background(barColor)
            )
        }
        Text(
            "→ $favoredName",
            style = MaterialTheme.typography.labelSmall,
            color = barColor,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(60.dp)
        )
    }
}
