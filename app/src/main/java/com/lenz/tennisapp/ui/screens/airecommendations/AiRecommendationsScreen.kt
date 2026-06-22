package com.lenz.tennisapp.ui.screens.airecommendations

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenz.tennisapp.data.api.PredictionMatchDto
import com.lenz.tennisapp.ui.theme.*

private fun PredictionMatchDto.isTopPick(): Boolean =
    (0.45f * confidence + 0.55f * kotlin.math.abs(p1Prob - p2Prob)) >= 0.28f

private data class ConfStyle(val label: String, val color: Color)
private fun confStyle(confidence: Float) = when {
    confidence >= 0.65f -> ConfStyle("HOHE KONFIDENZ", AuraPurple)
    confidence >= 0.50f -> ConfStyle("MITTLERE KONFIDENZ", Color(0xFFF59E0B))
    else                -> ConfStyle("NIEDRIGE KONFIDENZ", Color(0xFFEF4444))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiRecommendationsScreen(
    onMatchClick: (String) -> Unit = {},
    viewModel: AiRecommendationsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var allExpanded by remember { mutableStateOf(false) }

    val topPicks = remember(state.filtered) {
        state.filtered.filter { it.dto.isTopPick() }.sortedByDescending { it.dto.sortScore(AiSortMode.WEIGHTED) }
    }
    val allSorted = state.allSorted

    PullToRefreshBox(
        isRefreshing = state.isLoading,
        onRefresh = viewModel::refresh,
        modifier = Modifier.fillMaxSize().background(Color.White)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            // ── Date header ──────────────────────────────────────────────
            if (state.date.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = AuraPurple, modifier = Modifier.size(16.dp))
                            Text("KI-Empfehlungen", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = AuraPurple)
                        }
                        val displayDate = remember(state.date) {
                            try {
                                val inf = SimpleDateFormat("yyyy-MM-dd", Locale.GERMAN)
                                val outf = SimpleDateFormat("d. MMMM", Locale.GERMAN)
                                outf.format(inf.parse(state.date)!!)
                            } catch (e: Exception) { state.date }
                        }
                        Text(displayDate, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Tour filter chips ─────────────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(AiTourFilter.entries.size) { i ->
                        val f = AiTourFilter.entries[i]
                        FilterChip(
                            selected = state.tourFilter == f,
                            onClick = { viewModel.setTourFilter(f) },
                            label = { Text(f.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AuraPurple,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.tourFilter == f,
                                borderColor = AuraDeep.copy(alpha = 0.15f),
                                selectedBorderColor = AuraPurple
                            )
                        )
                    }
                }
            }

            // ── Category filter chips ─────────────────────────────────────
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(AiCategoryFilter.entries.size) { i ->
                        val f = AiCategoryFilter.entries[i]
                        FilterChip(
                            selected = state.categoryFilter == f,
                            onClick = { viewModel.setCategoryFilter(f) },
                            label = { Text(f.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AuraPurple,
                                selectedLabelColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = state.categoryFilter == f,
                                borderColor = AuraDeep.copy(alpha = 0.15f),
                                selectedBorderColor = AuraPurple
                            )
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
            }

            // ── Loading / Error / Empty ───────────────────────────────────
            if (state.error != null) {
                item {
                    Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                        Text(state.error!!, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
                    }
                }
                return@LazyColumn
            }
            if (state.enriched.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(48.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = AuraPurple.copy(alpha = 0.3f), modifier = Modifier.size(48.dp))
                        Spacer(Modifier.height(16.dp))
                        Text("Keine KI-Empfehlungen", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("Heute liegen noch keine KI-Prognosen vor.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                    }
                }
                return@LazyColumn
            }

            // ── Top Picks label ──────────────────────────────────────────
            if (topPicks.isNotEmpty()) {
                item {
                    Text(
                        "Top Picks",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Black,
                        color = AuraDeep,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }

                // ── Carousel ─────────────────────────────────────────────
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(topPicks, key = { "${it.dto.p1Fullname}_${it.dto.p2Fullname}_top" }) { match ->
                            TopPickCard(match.dto, onClick = { viewModel.findAndNavigate(match.dto.p1Fullname, match.dto.p2Fullname, onMatchClick) })
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }

            // ── Sort toggle (for Alle Prognosen) ────────────────────────
            item {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    AiSortMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = state.sortMode == mode,
                            onClick = { viewModel.setSortMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = AiSortMode.entries.size),
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = AuraPurple,
                                activeContentColor = Color.White,
                                inactiveContainerColor = Color.Transparent,
                                inactiveContentColor = AuraDeep.copy(alpha = 0.5f),
                                activeBorderColor = AuraPurple,
                                inactiveBorderColor = AuraDeep.copy(alpha = 0.1f)
                            )
                        ) {
                            Text(mode.label, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // ── Sort mode info card ──────────────────────────────────────
            item {
                val (title, desc) = when (state.sortMode) {
                    AiSortMode.WEIGHTED   -> "Gewichtete Stärke" to "Kombiniert Konfidenz (45 %) und Wahrscheinlichkeitsdifferenz (55 %) — ausgewogene Gesamtbewertung."
                    AiSortMode.CONFIDENCE -> "KI-Konfidenz" to "Wie sicher das Modell in seiner Vorhersage ist — unabhängig davon, wie knapp oder klar das Ergebnis erwartet wird."
                    AiSortMode.ACCURACY   -> "Wahrscheinlichkeitsdifferenz" to "Wie groß der Abstand zwischen den Siegchancen beider Spieler ist — je höher, desto klarer der Favorit."
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = AuraPurple.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraPurple.copy(alpha = 0.12f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = AuraPurple, modifier = Modifier.size(16.dp).padding(top = 2.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = AuraPurple)
                            Text(desc, style = MaterialTheme.typography.labelSmall, color = AuraDeep.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // ── All Picks expandable ─────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { allExpanded = !allExpanded }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Alle Prognosen",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Black,
                            color = AuraDeep
                        )
                        Text(
                            "${allSorted.size} Matches",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (allExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = null,
                        tint = AuraPurple
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }

            if (allExpanded) {
                items(allSorted, key = { "${it.dto.p1Fullname}_${it.dto.p2Fullname}_all" }) { match ->
                    CompactPickRow(match.dto, onClick = { viewModel.findAndNavigate(match.dto.p1Fullname, match.dto.p2Fullname, onMatchClick) })
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

// ── Top Pick Carousel Card ─────────────────────────────────────────────────────

@Composable
private fun TopPickCard(match: PredictionMatchDto, onClick: () -> Unit = {}) {
    val p1Wins = match.p1Prob >= match.p2Prob
    val favName = if (p1Wins) match.p1Fullname else match.p2Fullname
    val underName = if (p1Wins) match.p2Fullname else match.p1Fullname
    val favPct = if (p1Wins) (match.p1Prob * 100).toInt() else (match.p2Prob * 100).toInt()
    val undPct = 100 - favPct
    val favProb = if (p1Wins) match.p1Prob else match.p2Prob

    val conf = confStyle(match.confidence)

    val animFav by animateFloatAsState(targetValue = favProb, animationSpec = tween(900), label = "fav")

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(220.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Confidence chip
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = conf.color.copy(alpha = 0.12f)
            ) {
                Text(
                    conf.label,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = conf.color
                )
            }

            Spacer(Modifier.height(12.dp))

            // Favorite name
            Text(
                favName.split(" ").last(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = AuraDeep,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "vs ${underName.split(" ").last()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(14.dp))

            // Probability bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animFav)
                        .fillMaxHeight()
                        .background(
                            Brush.horizontalGradient(listOf(AuraPurple, AuraPurple.copy(alpha = 0.7f))),
                            RoundedCornerShape(4.dp)
                        )
                )
            }

            Spacer(Modifier.height(6.dp))

            // Percentages
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("$favPct%", fontSize = 13.sp, fontWeight = FontWeight.Black, color = AuraPurple)
                Text("$undPct%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444).copy(alpha = 0.7f))
            }
        }
    }
}

// ── Compact row for "Alle" section ────────────────────────────────────────────

@Composable
private fun CompactPickRow(match: PredictionMatchDto, onClick: () -> Unit = {}) {
    val p1Wins = match.p1Prob >= match.p2Prob
    val p1Pct = (match.p1Prob * 100).toInt()
    val p2Pct = (match.p2Prob * 100).toInt()
    val conf = confStyle(match.confidence)

    val animP1 by animateFloatAsState(targetValue = match.p1Prob, animationSpec = tween(700), label = "cp1")

    Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    match.p1Fullname.split(" ").last(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (p1Wins) FontWeight.Black else FontWeight.Normal,
                    color = if (p1Wins) AuraDeep else AuraDeep.copy(alpha = 0.5f),
                    maxLines = 1
                )
                Text("vs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    match.p2Fullname.split(" ").last(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (!p1Wins) FontWeight.Black else FontWeight.Normal,
                    color = if (!p1Wins) AuraDeep else AuraDeep.copy(alpha = 0.5f),
                    maxLines = 1
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = conf.color.copy(alpha = 0.1f)
            ) {
                Text(
                    when {
                        match.confidence >= 0.65f -> "HOCH"
                        match.confidence >= 0.50f -> "MITTEL"
                        else -> "NIEDRIG"
                    },
                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                    fontSize = 9.sp, fontWeight = FontWeight.Black, color = conf.color
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        Box(
            modifier = Modifier.fillMaxWidth().height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFEF4444).copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(animP1).fillMaxHeight()
                    .background(AuraPurple, RoundedCornerShape(2.dp))
            )
        }

        Spacer(Modifier.height(3.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$p1Pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraPurple)
            Text("$p2Pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444).copy(alpha = 0.7f))
        }
    }
}
