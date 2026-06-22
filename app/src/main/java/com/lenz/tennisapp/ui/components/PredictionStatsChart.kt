package com.lenz.tennisapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

import com.lenz.tennisapp.ui.theme.AuraPurple
import com.lenz.tennisapp.ui.theme.AuraDeep

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
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f)), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Noch keine Daten verfügbar",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
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
            .background(Color.White, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f)), RoundedCornerShape(16.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pie chart with actual visualization
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            // Background circle
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = size.minDimension / 2
                val center = center

                // Correct segment (purple)
                val correctAngle = (correct * 360f) / total
                drawArc(
                    color = AuraPurple,
                    startAngle = -90f,
                    sweepAngle = correctAngle,
                    useCenter = true,
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                )

                // Incorrect segment (orange)
                drawArc(
                    color = Color(0xFFE65100),
                    startAngle = -90f + correctAngle,
                    sweepAngle = (incorrect * 360f) / total,
                    useCenter = true,
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
                    topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                )
            }

            // Center hole for donut look
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${(correct * 100) / total}%",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = AuraDeep
                    )
                }
            }
        }

        // Legend
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            LegendItem(
                label = "Korrekt",
                count = correct,
                percent = correctPercent,
                color = AuraPurple
            )
            LegendItem(
                label = "Falsch",
                count = incorrect,
                percent = incorrectPercent,
                color = Color(0xFFE65100)
            )
        }
    }
}

@Composable
private fun LegendItem(label: String, count: Int, percent: Int, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Column {
            Text(
                "$label ($count)",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$percent%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
        }
    }
}
