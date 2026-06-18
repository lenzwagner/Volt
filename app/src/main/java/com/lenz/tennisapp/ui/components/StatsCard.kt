package com.lenz.tennisapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lenz.tennisapp.domain.model.StatLine

@Composable
fun StatsCard(
    stats: List<StatLine>,
    player1Name: String,
    player2Name: String,
    modifier: Modifier = Modifier
) {
    if (stats.isEmpty()) return

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            // Player name header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    player1Name.split(" ").last(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    "Statistik",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    player2Name.split(" ").last(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))

            stats.forEach { stat ->
                StatBarRow(stat = stat)
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

@Composable
private fun StatBarRow(stat: StatLine) {
    val homeNum = stat.homeValue.replace("%", "").toFloatOrNull()
    val awayNum = stat.awayValue.replace("%", "").toFloatOrNull()
    val total   = if (homeNum != null && awayNum != null) homeNum + awayNum else null

    val p1Color = MaterialTheme.colorScheme.primary
    val p2Color = MaterialTheme.colorScheme.secondary

    Column {
        // Value row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stat.homeValue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (stat.homeIsWinning == true) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (stat.homeIsWinning == true) p1Color
                else MaterialTheme.colorScheme.onSurface
            )
            Text(
                stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
            )
            Text(
                stat.awayValue,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (stat.homeIsWinning == false) FontWeight.ExtraBold else FontWeight.Normal,
                color = if (stat.homeIsWinning == false) p2Color
                else MaterialTheme.colorScheme.onSurface
            )
        }

        if (total != null && total > 0) {
            Spacer(Modifier.height(6.dp))
            val homeRatio = homeNum!! / total

            val animatedRatio by animateFloatAsState(
                targetValue = homeRatio.coerceIn(0.03f, 0.97f),
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness    = Spring.StiffnessMediumLow
                ),
                label = "stat_bar_${stat.label}"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(CircleShape)
            ) {
                // Background (p2)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(p2Color.copy(alpha = 0.2f))
                )
                // P1 fill with gradient
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedRatio)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    p1Color,
                                    p1Color.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            }
        }
    }
}
