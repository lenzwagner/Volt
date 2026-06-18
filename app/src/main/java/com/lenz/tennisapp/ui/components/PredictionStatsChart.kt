package com.lenz.tennisapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Simple pie chart showing correct vs incorrect predictions.
 */
@Composable
fun PredictionStatsChart(
    correct: Int,
    incorrect: Int,
    modifier: Modifier = Modifier
) {
    if (correct + incorrect == 0) {
        Box(
            modifier = modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Keine Daten",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    val total = correct + incorrect
    val correctPercent = (correct * 100) / total
    val incorrectPercent = (incorrect * 100) / total

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie chart with actual visualization
        Box(
            modifier = Modifier.size(140.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            Canvas(modifier = Modifier.size(140.dp)) {
                val radius = 70.dp.toPx()
                val center = androidx.compose.ui.geometry.Offset(70.dp.toPx(), 70.dp.toPx())

                // Correct segment (green)
                val correctAngle = (correct * 360f) / total
                drawArc(
                    color = Color(0xFF2E7D32),
                    startAngle = -90f,
                    sweepAngle = correctAngle,
                    useCenter = true,
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                )

                // Incorrect segment (orange)
                drawArc(
                    color = Color(0xFFCC5500),
                    startAngle = -90f + correctAngle,
                    sweepAngle = (incorrect * 360f) / total,
                    useCenter = true,
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                )
            }

            // Center text
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "${(correct * 100) / total}%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    "korrekt",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2E7D32))
                )
                Column {
                    Text(
                        "$correct richtig",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$correctPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFCC5500))
                )
                Column {
                    Text(
                        "$incorrect falsch",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "$incorrectPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
