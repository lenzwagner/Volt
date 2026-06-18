package com.lenz.tennisapp.ui.screens.predictions

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenz.tennisapp.domain.model.PredictionStats
import com.lenz.tennisapp.domain.model.UserPrediction
import com.lenz.tennisapp.ui.components.GreenHeader
import com.lenz.tennisapp.ui.components.PredictionStatsChart
import com.lenz.tennisapp.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PredictionsScreen(
    showHeader: Boolean = true,
    viewModel: PredictionsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPredictionsList by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        if (showHeader) {
            GreenHeader(title = "Prognosen", subtitle = "Deine Trefferquote")
        }

        PullToRefreshBox(
            isRefreshing = false,
            onRefresh = { /* No auto-refresh for predictions */ },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
            // ── Time filter ──────────────────────────────────────────────
            item {
                TimeRangeFilter(
                    selectedRange = state.timeRange,
                    onRangeChanged = viewModel::setTimeRange
                )
            }

            // ── Pie Chart ────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PredictionStatsChart(
                        correct = state.filteredStats.correct,
                        incorrect = if (state.filteredStats.totalResolved > 0) {
                            state.filteredStats.totalResolved - state.filteredStats.correct
                        } else {
                            0
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Summary stats ────────────────────────────────────────────
            item { StatsHeader(state.stats) }

            if (showPredictionsList) {
                if (state.predictions.isEmpty()) {
                    item { EmptyPredictions() }
                } else {
                    val grouped = state.predictions.groupBy { it.matchDate }

                    // Add "Auswertung" header with Clear button before matches
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Auswertung",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = { showPredictionsList = false },
                                modifier = Modifier
                                    .height(32.dp)
                                    .padding(0.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Zurücksetzen", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    grouped.entries.sortedByDescending { it.key }.forEach { (dateStr, preds) ->
                        item {
                            // Format date from "2026-06-05" to "5. Juni 2026"
                            val parsedDate = try {
                                val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
                                val outputFormat = SimpleDateFormat("d. MMMM yyyy", Locale.GERMAN)
                                val date = inputFormat.parse(dateStr)
                                outputFormat.format(date)
                            } catch (e: Exception) {
                                dateStr
                            }

                            Text(
                                parsedDate,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        items(preds, key = { it.matchId }) { prediction ->
                            PredictionRow(prediction)
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                color    = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    }
                }
            }
            }   // end LazyColumn
        }   // end PullToRefreshBox
    }   // end Column
}

// ─── Stats header ─────────────────────────────────────────────────────────────

@Composable
private fun StatsHeader(stats: PredictionStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
        StatChip(
            label   = "Gesamt",
            value   = if (stats.totalResolved > 0) "${stats.overallPct}%" else "—",
            sub     = "${stats.correct}/${stats.totalResolved}",
            color   = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label   = "7 Tage",
            value   = if (stats.weeklyResolved > 0) "${stats.weeklyPct}%" else "—",
            sub     = "${stats.weeklyCorrect}/${stats.weeklyResolved}",
            color   = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.weight(1f)
        )
        StatChip(
            label   = "30 Tage",
            value   = if (stats.monthlyResolved > 0) "${stats.monthlyPct}%" else "—",
            sub     = "${stats.monthlyCorrect}/${stats.monthlyResolved}",
            color   = TertiaryBlue,
            modifier = Modifier.weight(1f)
        )
        if (stats.pending > 0) {
            StatChip(
                label   = "Offen",
                value   = stats.pending.toString(),
                sub     = "läuft noch",
                color   = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                sub,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Prediction row ────────────────────────────────────────────────────────────

@Composable
private fun PredictionRow(prediction: UserPrediction) {
    val bgColor = when (prediction.isCorrect) {
        true  -> MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.05f)
        null  -> Color.Transparent
    }

    Surface(
        color = bgColor,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Result badge
            ResultBadge(prediction.isCorrect)

            Spacer(Modifier.width(12.dp))

            // Match info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${prediction.homePlayerName} vs ${prediction.awayPlayerName}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1
                )
                Text(
                    prediction.tournamentName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Spacer(Modifier.width(8.dp))

            // Pick + outcome
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "Tipp: ${prediction.predictedWinnerName.split(" ").last()}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!prediction.isPending && prediction.actualWinnerName != null) {
                    Text(
                        "Sieger: ${prediction.actualWinnerName.split(" ").last()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (prediction.isPending) {
                    Text(
                        "läuft noch",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultBadge(isCorrect: Boolean?) {
    val (emoji, color) = when (isCorrect) {
        true  -> "✅" to MaterialTheme.colorScheme.primary
        false -> "❌" to MaterialTheme.colorScheme.error
        null  -> "○" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, style = MaterialTheme.typography.bodyMedium)
    }
}

// ─── Empty state ───────────────────────────────────────────────────────────────

@Composable
private fun EmptyPredictions() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Noch keine Prognosen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Tippe auf einem Match, wer gewinnt —\ndann siehst du hier deine Trefferquote!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Time range filter ─────────────────────────────────────────────────────────

@Composable
private fun TimeRangeFilter(
    selectedRange: PredictionTimeRange,
    onRangeChanged: (PredictionTimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PredictionTimeRange.entries.forEach { range ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeChanged(range) },
                label = { Text(range.label, style = MaterialTheme.typography.labelMedium) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
