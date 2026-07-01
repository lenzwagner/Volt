package com.lenz.tennisapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.style.TextOverflow
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "HEAD TO HEAD",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = AuraDeep,
                letterSpacing = 0.5.sp
            )
            
            Spacer(Modifier.height(16.dp))

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
                Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                    Text(
                        h2h.player1Wins.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = AuraPurple
                    )
                    Text(
                        h2h.player1Name.split(" ").last().uppercase(),
                        fontSize = 12.sp,
                        color = AuraPurple,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // VS
                Text(
                    "VS",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = AuraDeep.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Player 2
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                    Text(
                        h2h.player2Wins.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = AuraDeep
                    )
                    Text(
                        h2h.player2Name.split(" ").last().uppercase(),
                        fontSize = 12.sp,
                        color = AuraDeep.copy(alpha = 0.6f),
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Segmented bar ────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(AuraDeep.copy(alpha = 0.08f))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(p1Animated)
                            .background(AuraPurple)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f - p1Animated)
                            .background(Color.Transparent)
                    )
                }
            }

            // Percentage labels
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${(p1Frac * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = AuraPurple,
                    fontWeight = FontWeight.Black
                )
                Text(
                    "${((1 - p1Frac) * 100).toInt()}%",
                    fontSize = 11.sp,
                    color = AuraDeep.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Black
                )
            }

            // ── Recent matches ────────────────────────────────────────────
            if (h2h.recentMatches.isNotEmpty()) {
                Spacer(Modifier.height(24.dp))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(16.dp))

                Text(
                    "LETZTE BEGEGNUNGEN",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = AuraDeep.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(12.dp))

                h2h.recentMatches.forEachIndexed { index, match ->
                    val isP1Winner = match.winner == h2h.player1Name
                    
                    val surfaceColor = when (match.surface) {
                        Surface.CLAY        -> ClayColor
                        Surface.GRASS       -> GrassColor
                        Surface.HARD        -> HardColor
                        Surface.INDOOR_HARD -> IndoorColor
                        else                -> Color.Gray
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        // Match info line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = match.date.take(4),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = AuraDeep.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = match.tournament.uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = AuraDeep.copy(alpha = 0.6f),
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Box(
                                modifier = Modifier
                                    .background(surfaceColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = match.surface.name.take(1),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    color = surfaceColor
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(6.dp))
                        
                        // Score line
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Player 1 Name
                            Text(
                                h2h.player1Name.split(" ").last().uppercase(),
                                fontSize = 12.sp,
                                fontWeight = if (isP1Winner) FontWeight.Black else FontWeight.Medium,
                                color = if (isP1Winner) AuraPurple else AuraDeep.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Sets/Score
                            val displayScore = if (match.p1Scores.isNotEmpty() && match.p2Scores.isNotEmpty()) {
                                val p1S = match.p1Sets
                                val p2S = match.p2Sets
                                "$p1S : $p2S"
                            } else {
                                match.score
                            }
                            
                            Text(
                                text = displayScore,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = AuraDeep,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            
                            // Player 2 Name
                            Text(
                                h2h.player2Name.split(" ").last().uppercase(),
                                fontSize = 12.sp,
                                fontWeight = if (!isP1Winner) FontWeight.Black else FontWeight.Medium,
                                color = if (!isP1Winner) AuraPurple else AuraDeep.copy(alpha = 0.4f),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                        
                        if (index < h2h.recentMatches.size - 1) {
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 0.5.dp)
                        }
                    }
                }
            }
        }
    }
}
