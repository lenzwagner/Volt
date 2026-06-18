package com.lenz.tennisapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lenz.tennisapp.domain.model.PlayerEloProfile
import com.lenz.tennisapp.ui.theme.*

@Composable
fun EloOverviewCard(
    player1Name: String,
    player2Name: String,
    player1Elo: PlayerEloProfile?,
    player2Elo: PlayerEloProfile?,
    modifier: Modifier = Modifier
) {
    if (player1Elo == null && player2Elo == null) return

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Player name header
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    player1Name.split(" ").last(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Belag",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    player2Name.split(" ").last(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(12.dp))

            // Overall ELO — Green only color scheme
            val greenColor = Color(0xFF2E7D32)  // Tennis green
            val grayColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)

            EloComparisonRow(
                label = "Gesamt",
                p1Value = player1Elo?.eloOverall,
                p2Value = player2Elo?.eloOverall,
                greenColor = greenColor,
                grayColor = grayColor
            )

            Spacer(Modifier.height(10.dp))

            EloComparisonRow(
                label = "Hartplatz",
                p1Value = player1Elo?.eloHard,
                p2Value = player2Elo?.eloHard,
                greenColor = greenColor,
                grayColor = grayColor
            )

            Spacer(Modifier.height(10.dp))

            EloComparisonRow(
                label = "Sand",
                p1Value = player1Elo?.eloClay,
                p2Value = player2Elo?.eloClay,
                greenColor = greenColor,
                grayColor = grayColor
            )

            Spacer(Modifier.height(10.dp))

            EloComparisonRow(
                label = "Rasen",
                p1Value = player1Elo?.eloGrass,
                p2Value = player2Elo?.eloGrass,
                greenColor = greenColor,
                grayColor = grayColor
            )

            // Matches played (data quality badge)
            val maxMatches = maxOf(player1Elo?.matchesPlayed ?: 0, player2Elo?.matchesPlayed ?: 0)
            if (maxMatches > 0) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DataQualityChip(
                        matches = player1Elo?.matchesPlayed ?: 0,
                        name = player1Name.split(" ").last()
                    )
                    DataQualityChip(
                        matches = player2Elo?.matchesPlayed ?: 0,
                        name = player2Name.split(" ").last()
                    )
                }
            }
        }
    }
}

@Composable
private fun EloComparisonRow(
    label: String,
    p1Value: Int?,
    p2Value: Int?,
    greenColor: Color,
    grayColor: Color
) {
    val p1 = p1Value?.toFloat() ?: 1500f
    val p2 = p2Value?.toFloat() ?: 1500f
    val total = p1 + p2
    val p1Frac = if (total > 0) p1 / total else 0.5f

    val animatedFrac by animateFloatAsState(
        targetValue = p1Frac.coerceIn(0.05f, 0.95f),
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioLowBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
        ),
        label = "elo_bar_$label"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // P1 value — Green if stronger, else gray
            Text(
                text = p1Value?.toString() ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (p1 >= p2) FontWeight.Bold else FontWeight.Normal,
                color = if (p1 >= p2) greenColor else grayColor,
                modifier = Modifier.weight(1f)
            )
            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            // P2 value — Green if stronger, else gray
            Text(
                text = p2Value?.toString() ?: "—",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (p2 > p1) FontWeight.Bold else FontWeight.Normal,
                color = if (p2 > p1) greenColor else grayColor,
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(4.dp))

        // Comparison bar — Green for strong, gray for weak
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .weight(animatedFrac)
                    .fillMaxHeight()
                    .background(if (p1 >= p2) greenColor else grayColor)
            )
            Spacer(Modifier.width(2.dp))
            Box(
                modifier = Modifier
                    .weight(1f - animatedFrac)
                    .fillMaxHeight()
                    .background(if (p2 > p1) greenColor else grayColor)
            )
        }
    }
}

@Composable
private fun DataQualityChip(matches: Int, name: String) {
    val (label, color) = when {
        matches >= 100 -> "viele Daten"  to MaterialTheme.colorScheme.primary
        matches >= 20  -> "mittel"       to MaterialTheme.colorScheme.secondary
        else           -> "wenig Daten"  to MaterialTheme.colorScheme.error
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.12f)
    ) {
        Text(
            "$name · $label",
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}
