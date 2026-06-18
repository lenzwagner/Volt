package com.lenz.tennisapp.ui.screens.tournament

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenz.tennisapp.domain.model.Surface
import com.lenz.tennisapp.domain.model.TournamentCategory
import com.lenz.tennisapp.ui.components.GreenHeader
import com.lenz.tennisapp.ui.components.MatchCard

@Composable
fun TournamentDetailScreen(
    leagueId: String,
    tournamentName: String,
    onBack: () -> Unit,
    onMatchClick: (String) -> Unit,
    viewModel: TournamentDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(leagueId) {
        viewModel.loadTournament(leagueId, tournamentName)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        GreenHeader(
            title = tournamentName,
            subtitle = "Turnierübersicht",
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Color.White)
                }
            }
        )

        if (state.isLoading && state.rounds.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                state.tournamentInfo?.let { info ->
                    item {
                        TournamentInfoCard(info)
                    }
                }

                state.rounds.forEach { round ->
                    item {
                        Text(
                            round.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    items(round.matches, key = { it.id }) { match ->
                        MatchCard(
                            match = match,
                            onClick = { onMatchClick(match.id) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentInfoCard(info: TournamentInfo) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Turnier-Informationen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                InfoItem("Kategorie", info.category.displayName, Modifier.weight(1f))
                InfoItem("Punkte", info.points, Modifier.weight(1f))
                val surfaceName = when(info.surface) {
                    Surface.CLAY -> "Sand"
                    Surface.GRASS -> "Rasen"
                    Surface.HARD -> "Hartplatz"
                    Surface.INDOOR_HARD -> "Hartplatz (H)"
                    else -> info.surface.displayName
                }
                InfoItem("Belag", surfaceName, Modifier.weight(1f))
            }

            info.lastWinner?.let { winner ->
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(modifier = Modifier.alpha(0.5f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Vorjahressieger:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Text(
                        winner,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
