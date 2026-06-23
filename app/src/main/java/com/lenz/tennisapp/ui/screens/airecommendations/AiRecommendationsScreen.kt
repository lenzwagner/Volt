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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.lenz.tennisapp.data.api.PredictionMatchDto
import com.lenz.tennisapp.domain.model.TournamentCategory
import com.lenz.tennisapp.ui.screens.home.CategoryFilter
import com.lenz.tennisapp.ui.screens.home.FilterSection
import com.lenz.tennisapp.ui.screens.home.FormatFilter
import com.lenz.tennisapp.ui.screens.home.TourFilter
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
    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val activeFilterCount = (if (state.tourFilter != TourFilter.ALL) 1 else 0) +
                            (if (state.formatFilter != FormatFilter.ALL) 1 else 0) +
                            (if (state.categoryFilter != CategoryFilter.ALL) 1 else 0)

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

            // ── Filter button row ─────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = { showFilters = true },
                        color = AuraDeep,
                        shape = CircleShape,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (activeFilterCount > 0) {
                                Box(
                                    modifier = Modifier.size(20.dp).background(Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(activeFilterCount.toString(), color = AuraDeep, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Icon(Icons.Outlined.FilterList, null, tint = Color.White, modifier = Modifier.size(18.dp))
                            }
                            Text("Filter", color = Color.White, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
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

            // ── Top Picks carousel ───────────────────────────────────────
            if (topPicks.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    Icons.Filled.AutoAwesome,
                                    null,
                                    tint = AuraPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    "Top Picks",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Black,
                                    color = AuraDeep
                                )
                            }
                            Text(
                                "${topPicks.size} Treffer",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Box {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(topPicks, key = { "${it.dto.p1Fullname}_${it.dto.p2Fullname}_top" }) { match ->
                                    TopPickCard(match, onClick = { viewModel.findAndNavigate(match.dto.p1Fullname, match.dto.p2Fullname, onMatchClick) })
                                }
                            }
                            // Right edge fade
                            Box(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .width(40.dp)
                                    .height(180.dp)
                                    .background(
                                        Brush.horizontalGradient(
                                            listOf(Color.White.copy(alpha = 0f), Color.White)
                                        )
                                    )
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
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
                ElevatedCard(
                    onClick = { allExpanded = !allExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = AuraPurple
                            ) {
                                Text(
                                    "${allSorted.size}",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                            Column {
                                Text(
                                    "Alle Prognosen",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = if (allExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = AuraPurple
                        )
                    }
                }
            }

            if (allExpanded) {
                item { Spacer(Modifier.height(2.dp)) }
                items(allSorted, key = { "${it.dto.p1Fullname}_${it.dto.p2Fullname}_all" }) { match ->
                    ElevatedCard(
                        onClick = { viewModel.findAndNavigate(match.dto.p1Fullname, match.dto.p2Fullname, onMatchClick) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(14.dp),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = Color.White)
                    ) {
                        CompactPickRow(match)
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }

    // ── Filter BottomSheet ────────────────────────────────────────────────────
    if (showFilters) {
        val view = androidx.compose.ui.platform.LocalView.current
        val window = (view.context as? android.app.Activity)?.window
        DisposableEffect(Unit) {
            window?.let { w ->
                val ctrl = androidx.core.view.WindowCompat.getInsetsController(w, view)
                w.navigationBarColor = android.graphics.Color.parseColor("#1D1B20")
                ctrl.isAppearanceLightNavigationBars = false
            }
            onDispose {
                window?.let { w ->
                    val ctrl = androidx.core.view.WindowCompat.getInsetsController(w, view)
                    w.navigationBarColor = android.graphics.Color.WHITE
                    ctrl.isAppearanceLightNavigationBars = true
                }
            }
        }
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheetState,
            containerColor = Color.Transparent,
            dragHandle = null,
            contentWindowInsets = { WindowInsets(0) }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .background(AuraDeep, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp)) {
                    Box(modifier = Modifier.size(40.dp, 4.dp).background(Color.White.copy(alpha = 0.3f), CircleShape).align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Filter", style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showFilters = false }) {
                            Icon(Icons.Filled.Check, null, tint = AuraLime, modifier = Modifier.size(28.dp))
                        }
                    }
                    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)) {
                        Spacer(Modifier.height(20.dp))
                        FilterSection(
                            title = "Tour",
                            options = TourFilter.entries,
                            selected = state.tourFilter,
                            onSelect = viewModel::setTourFilter
                        )
                        Spacer(Modifier.height(20.dp))
                        FilterSection(
                            title = "Format",
                            options = FormatFilter.entries,
                            selected = state.formatFilter,
                            onSelect = viewModel::setFormatFilter
                        )
                        Spacer(Modifier.height(20.dp))
                        FilterSection(
                            title = "Kategorie",
                            options = CategoryFilter.entries,
                            selected = state.categoryFilter,
                            onSelect = viewModel::setCategoryFilter
                        )
                    }
                }
            }
        }
        Box(modifier = Modifier.fillMaxSize().blur(16.dp).background(Color.White.copy(alpha = 0.4f)))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun TournamentCategory?.shortLabel() = when (this) {
    TournamentCategory.GRAND_SLAM -> "GS"
    TournamentCategory.ATP_MASTERS_1000, TournamentCategory.WTA_1000 -> "1000"
    TournamentCategory.ATP_500, TournamentCategory.WTA_500 -> "500"
    TournamentCategory.ATP_250, TournamentCategory.WTA_250 -> "250"
    TournamentCategory.WTA_125 -> "WTA 125"
    TournamentCategory.CHALLENGER_175, TournamentCategory.CHALLENGER_125,
    TournamentCategory.CHALLENGER_100, TournamentCategory.CHALLENGER_75,
    TournamentCategory.CHALLENGER_50, TournamentCategory.CHALLENGER -> "CH"
    TournamentCategory.ITF -> "ITF"
    else -> null
}

private fun EnrichedAiPrediction.tourLabel() = when {
    isDoubles -> "DBL"
    isAtp     -> "ATP"
    isWta     -> "WTA"
    else      -> null
}

// ── Top Pick Carousel Card ─────────────────────────────────────────────────────

@Composable
private fun TopPickCard(enriched: EnrichedAiPrediction, onClick: () -> Unit = {}) {
    val match = enriched.dto
    val p1Wins = match.p1Prob >= match.p2Prob
    val p1Pct = (match.p1Prob * 100).toInt()
    val p2Pct = (match.p2Prob * 100).toInt()
    val conf = confStyle(match.confidence)
    val animP1 by animateFloatAsState(targetValue = match.p1Prob, animationSpec = tween(900), label = "fav")
    val animConf by animateFloatAsState(targetValue = match.confidence, animationSpec = tween(1000), label = "conf")

    val catLabel = enriched.category.shortLabel()
    val tourLabel = enriched.tourLabel()
    val purpleAlpha = AuraPurple

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(230.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = AuraDeep)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Subtle shimmer dot top-right
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .offset(x = 140.dp, y = (-20).dp)
                    .background(
                        Brush.radialGradient(
                            listOf(AuraPurple.copy(alpha = 0.25f), Color.Transparent)
                        ),
                        CircleShape
                    )
            )

            Column(modifier = Modifier.padding(14.dp, 14.dp, 14.dp, 14.dp)) {

                // Top row: badges | confidence chip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tourLabel?.let {
                            Surface(shape = RoundedCornerShape(6.dp), color = Color.White.copy(alpha = 0.12f)) {
                                Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                        catLabel?.let {
                            Surface(shape = RoundedCornerShape(6.dp), color = AuraPurple.copy(alpha = 0.35f)) {
                                Text(it, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 9.sp, fontWeight = FontWeight.Black, color = AuraLime)
                            }
                        }
                    }
                    Surface(shape = RoundedCornerShape(20.dp), color = conf.color.copy(alpha = 0.22f)) {
                        Text(conf.label, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontSize = 8.sp, fontWeight = FontWeight.Black, color = conf.color)
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Face-off + confidence arc
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    // P1
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            match.p1Fullname.split(" ").last(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (p1Wins) FontWeight.Black else FontWeight.Normal,
                            color = if (p1Wins) Color.White else Color.White.copy(alpha = 0.4f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "$p1Pct%",
                            fontSize = 14.sp, fontWeight = FontWeight.Black,
                            color = if (p1Wins) AuraLime else Color.White.copy(alpha = 0.35f)
                        )
                    }

                    // Confidence arc
                    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val stroke = 5f
                            val inset = stroke / 2
                            val rect = androidx.compose.ui.geometry.Rect(
                                offset = Offset(inset, inset),
                                size = Size(size.width - stroke, size.height - stroke)
                            )
                            drawArc(color = Color.White.copy(alpha = 0.12f), startAngle = 135f, sweepAngle = 270f,
                                useCenter = false, topLeft = rect.topLeft, size = rect.size,
                                style = Stroke(width = stroke, cap = StrokeCap.Round))
                            drawArc(color = conf.color, startAngle = 135f, sweepAngle = 270f * animConf,
                                useCenter = false, topLeft = rect.topLeft, size = rect.size,
                                style = Stroke(width = stroke, cap = StrokeCap.Round))
                        }
                        Text("${(match.confidence * 100).toInt()}%",
                            fontSize = 10.sp, fontWeight = FontWeight.Black, color = conf.color)
                    }

                    // P2
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                        Text(
                            match.p2Fullname.split(" ").last(),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = if (!p1Wins) FontWeight.Black else FontWeight.Normal,
                            color = if (!p1Wins) Color.White else Color.White.copy(alpha = 0.4f),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End
                        )
                        Text(
                            "$p2Pct%",
                            fontSize = 14.sp, fontWeight = FontWeight.Black,
                            color = if (!p1Wins) AuraLime else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.End
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Probability bar
                Box(modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animP1).fillMaxHeight()
                            .background(
                                Brush.horizontalGradient(listOf(AuraLime, AuraPurple)),
                                RoundedCornerShape(3.dp)
                            )
                    )
                }
            }
        }
    }
}

// ── Compact row for "Alle" section ────────────────────────────────────────────

@Composable
private fun CompactPickRow(enriched: EnrichedAiPrediction) {
    val match = enriched.dto
    val p1Wins = match.p1Prob >= match.p2Prob
    val p1Pct = (match.p1Prob * 100).toInt()
    val p2Pct = (match.p2Prob * 100).toInt()
    val conf = confStyle(match.confidence)
    val catLabel = enriched.category.shortLabel()
    val tourLabel = enriched.tourLabel()
    val animP1 by animateFloatAsState(targetValue = match.p1Prob, animationSpec = tween(700), label = "cp1")

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Player names
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    match.p1Fullname.split(" ").last(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (p1Wins) FontWeight.Black else FontWeight.Normal,
                    color = if (p1Wins) AuraDeep else AuraDeep.copy(alpha = 0.5f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
                Text("vs", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    match.p2Fullname.split(" ").last(),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = if (!p1Wins) FontWeight.Black else FontWeight.Normal,
                    color = if (!p1Wins) AuraDeep else AuraDeep.copy(alpha = 0.5f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false)
                )
            }

            // Right badges
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                tourLabel?.let {
                    Surface(shape = RoundedCornerShape(5.dp), color = AuraDeep.copy(alpha = 0.07f)) {
                        Text(it, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraDeep.copy(alpha = 0.6f))
                    }
                }
                catLabel?.let {
                    Surface(shape = RoundedCornerShape(5.dp), color = AuraPurple.copy(alpha = 0.08f)) {
                        Text(it, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            fontSize = 8.sp, fontWeight = FontWeight.Black, color = AuraPurple.copy(alpha = 0.8f))
                    }
                }
                Surface(shape = RoundedCornerShape(20.dp), color = conf.color.copy(alpha = 0.1f)) {
                    Text(
                        when {
                            match.confidence >= 0.65f -> "HOCH"
                            match.confidence >= 0.50f -> "MITTEL"
                            else -> "NIEDRIG"
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 8.sp, fontWeight = FontWeight.Black, color = conf.color
                    )
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        // Probability split bar
        Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
            .background(Color(0xFFEF4444).copy(alpha = 0.18f))
        ) {
            Box(modifier = Modifier.fillMaxWidth(animP1).fillMaxHeight().background(AuraPurple, RoundedCornerShape(2.dp)))
        }

        Spacer(Modifier.height(3.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$p1Pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AuraPurple)
            Text("$p2Pct%", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444).copy(alpha = 0.7f))
        }
    }
}
