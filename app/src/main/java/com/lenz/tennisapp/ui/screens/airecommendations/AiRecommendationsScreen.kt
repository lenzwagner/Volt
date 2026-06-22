package com.lenz.tennisapp.ui.screens.airecommendations

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenz.tennisapp.data.api.PredictionMatchDto
import com.lenz.tennisapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendationsScreen(
    viewModel: AiRecommendationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            if (state.date.isNotEmpty()) {
                item {
                    AiInfoBanner(state.date, state.matches.size)
                }
            }

            if (state.isLoading && state.matches.isEmpty()) {
                item { LoadingState() }
            } else if (state.error != null) {
                item { ErrorState(state.error!!) }
            } else if (state.matches.isEmpty()) {
                item { EmptyState() }
            } else {
                items(state.matches, key = { "${it.p1Fullname}_${it.p2Fullname}" }) { match ->
                    AiMatchCard(match)
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AiInfoBanner(date: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = AuraPurple,
                modifier = Modifier.size(18.dp)
            )
            Column {
                Text(
                    "KI-Empfehlungen",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = AuraPurple
                )
                Text(
                    date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = AuraPurple.copy(alpha = 0.1f)
        ) {
            Text(
                "$count Matches",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = AuraPurple
            )
        }
    }
}

@Composable
private fun AiMatchCard(match: PredictionMatchDto) {
    val p1Pct = (match.p1Prob * 100).toInt()
    val p2Pct = (match.p2Prob * 100).toInt()
    val p1Wins = match.p1Prob >= match.p2Prob

    val animatedP1 by animateFloatAsState(
        targetValue = match.p1Prob,
        animationSpec = tween(900),
        label = "p1"
    )

    val (confLabel, confColor) = when {
        match.confidence >= 0.65f -> "HOHE KONFIDENZ" to AuraPurple
        match.confidence >= 0.50f -> "MITTLERE KONFIDENZ" to Color(0xFFF59E0B)
        else                      -> "NIEDRIGE KONFIDENZ" to Color(0xFFEF4444)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        // Players row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    match.p1Fullname,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (p1Wins) FontWeight.Bold else FontWeight.Normal,
                    color = if (p1Wins) AuraDeep else AuraDeep.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
            Text(
                "vs",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                Text(
                    match.p2Fullname,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (!p1Wins) FontWeight.Bold else FontWeight.Normal,
                    color = if (!p1Wins) AuraDeep else AuraDeep.copy(alpha = 0.6f),
                    maxLines = 1,
                    textAlign = TextAlign.End
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Probability bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(Color(0xFFEF4444).copy(alpha = 0.25f), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedP1)
                    .fillMaxHeight()
                    .background(AuraPurple, RoundedCornerShape(3.dp))
            )
        }

        Spacer(Modifier.height(6.dp))

        // Percentages
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$p1Pct%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AuraPurple)
            Text("$p2Pct%", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
        }

        Spacer(Modifier.height(8.dp))

        // Confidence badge
        Box(
            modifier = Modifier
                .background(confColor.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(confLabel, fontSize = 10.sp, fontWeight = FontWeight.Black, color = confColor)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(64.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(color = AuraPurple)
    }
}

@Composable
private fun ErrorState(msg: String) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(msg, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
    }
}

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Filled.AutoAwesome, null, tint = AuraPurple.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text(
                "Keine KI-Empfehlungen",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Heute liegen noch keine KI-Prognosen vor.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
