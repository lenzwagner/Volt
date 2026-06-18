package com.lenz.tennisapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.lenz.tennisapp.domain.model.TennisMatch
import kotlin.math.roundToInt

/**
 * Banner showing the match with highest AI confidence (tip of the day).
 * Displays when confidence difference > threshold, can be dismissed.
 */
@Composable
fun TipOfTheDayBanner(
    match: TennisMatch,
    aiProbHome: Float,  // 0.0 to 1.0
    isVisible: Boolean,
    onDismiss: () -> Unit,
    onMatchClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically(),
        modifier = modifier
    ) {
        val aiProbAway = 1f - aiProbHome
        val confidence = maxOf(aiProbHome, aiProbAway)
        val recommendedWinner = if (aiProbHome > aiProbAway) {
            match.homePlayer.name.split(" ").last()
        } else {
            match.awayPlayer.name.split(" ").last()
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            Color(0xFF1B5E20).copy(alpha = 0.9f),
                            Color(0xFF2E7D32).copy(alpha = 0.9f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(enabled = true) { onMatchClick() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left: Content
                Column(
                    modifier = Modifier
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "🏆 Tipp des Tages",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.9f)
                    )

                    Text(
                        "${match.homePlayer.name.split(" ").last()} vs ${match.awayPlayer.name.split(" ").last()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "KI sieht: $recommendedWinner mit ${(confidence * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }

                // Right: Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Schließen",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
