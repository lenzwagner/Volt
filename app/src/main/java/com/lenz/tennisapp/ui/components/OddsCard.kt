package com.lenz.tennisapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lenz.tennisapp.domain.model.BookmakerOdds
import com.lenz.tennisapp.ui.theme.SlamGold
import kotlin.math.roundToInt

@Composable
fun OddsCard(
    odds: List<BookmakerOdds>,
    player1Name: String,
    player2Name: String,
    modifier: Modifier = Modifier
) {
    // Filter: nur Tipico Quoten
    val tipicoOdds = odds.firstOrNull { it.bookmakerName.lowercase().contains("tipico") }
    val noOdds = tipicoOdds == null

    // Berechne wahre Wahrscheinlichkeit aus Tipico-Quoten (bereinigt um Buchmachermarge)
    val (player1TrueProb, player2TrueProb, margin) = if (tipicoOdds != null) {
        val p1Implied = (1.0 / tipicoOdds.homeOdds) * 100
        val p2Implied = (1.0 / tipicoOdds.awayOdds) * 100
        val total = p1Implied + p2Implied
        val p1True = (p1Implied / total) * 100
        val p2True = (p2Implied / total) * 100
        val buchmacherMargin = (total - 100.0)
        Triple(p1True, p2True, buchmacherMargin)
    } else {
        Triple(0.0, 0.0, 0.0)
    }

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp)) {

            if (noOdds) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Noch keine Quoten verfügbar.\nQuoten erscheinen typischerweise kurz vor Spielbeginn.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Spacer(Modifier.height(16.dp))

                // Dezimalquoten
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            player1Name.split(" ").last(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                String.format("%.2f", tipicoOdds.homeOdds),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            player2Name.split(" ").last(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                String.format("%.2f", tipicoOdds.awayOdds),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))

                // Wahre Wahrscheinlichkeit (bereinigt)
                Text(
                    "Wahre Wahrscheinlichkeit (Markt-implizit)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            player1Name.split(" ").last(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${player1TrueProb.roundToInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            player2Name.split(" ").last(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${player2TrueProb.roundToInt()}%",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Buchmachermarge: ${margin.roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
