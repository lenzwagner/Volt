package com.lenz.tennisapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenz.tennisapp.domain.model.TennisMatch
import kotlin.math.roundToInt

/**
 * Carousel of high-confidence AI picks (>75% confidence).
 * Shows as horizontal scrollable blocks with height indicating confidence.
 * Tap any block to view match details.
 */
@Composable
fun TipsCarousel(
    tips: List<TipItem>,
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onTipClick: (matchId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible && tips.isNotEmpty(),
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header with close button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "🏆 Empfehlungen (KI >75%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Horizontal scrollable carousel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                tips.forEach { tip ->
                    TipBlock(
                        tip = tip,
                        onClick = { onTipClick(tip.match.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TipBlock(
    tip: TipItem,
    onClick: () -> Unit
) {
    val normalizedConfidence = (tip.aiProb).coerceIn(0.75f, 1.0f)
    // Map 0.75-1.0 to 0.4-1.0 height fraction
    val heightFraction = 0.4f + (normalizedConfidence - 0.75f) * (0.6f / 0.25f)

    Column(
        modifier = Modifier
            .width(80.dp)
            .fillMaxHeight()
            .clickable { onClick() },
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Container for the bar and its background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(
                    color = Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            // The active confidence bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(heightFraction)
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF2E7D32),
                                Color(0xFF1B5E20)
                            )
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )
            )

            // Content on top of both background and bar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Confidence percentage at top
                Text(
                    "${(tip.aiProb * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        letterSpacing = 0.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Winner Last Name
                Text(
                    tip.winnerName.split(" ").last(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        lineHeight = 10.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }

        // Matchup text below
        Text(
            "${tip.match.homePlayer.name.split(" ").last().take(3)} - ${tip.match.awayPlayer.name.split(" ").last().take(3)}",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}

