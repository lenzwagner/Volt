package com.lenz.tennisapp.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.ui.theme.*
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMatchClick: (String) -> Unit,
    onTournamentClick: (String, String) -> Unit = { _, _ -> },
    showHeader: Boolean = true,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.todayUiState.collectAsStateWithLifecycle()
    val tourFilter by viewModel.tourFilter.collectAsStateWithLifecycle()
    val formatFilter by viewModel.formatFilter.collectAsStateWithLifecycle()
    val categoryFilter by viewModel.categoryFilter.collectAsStateWithLifecycle()
    val liveFilter by viewModel.liveFilter.collectAsStateWithLifecycle()
    val finishedFilter by viewModel.finishedFilter.collectAsStateWithLifecycle()
    val liveCount by viewModel.liveCount.collectAsStateWithLifecycle()

    var showFilters by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    val activeFilterCount = listOf(
        tourFilter != TourFilter.ALL,
        formatFilter != FormatFilter.ALL,
        categoryFilter != CategoryFilter.ALL
    ).count { it }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        val expandedMap = remember { mutableStateMapOf<String, Boolean>() }
        val tournaments = state.tournaments ?: emptyList()

        CompactFilterRow(
            liveFilterActive = liveFilter,
            finishedFilterActive = finishedFilter,
            liveCount = liveCount,
            activeFilterCount = activeFilterCount,
            onToggleLive = viewModel::toggleLiveFilter,
            onToggleFinished = viewModel::toggleFinishedFilter,
            onOpenFilters = { showFilters = true },
            onCollapseAll = {
                val allCollapsed = tournaments.all { expandedMap[it.id] == false }
                val newState = allCollapsed
                tournaments.forEach { expandedMap[it.id] = newState }
            }
        )

        if (state.isLoading && !state.isRefreshing) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AuraPurple)
            }
        } else {
            if (tournaments.isEmpty() && !state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Matches gefunden", color = Color.Gray)
                }
            } else {
                // Ensure new tournaments are expanded by default
                LaunchedEffect(tournaments) {
                    tournaments.forEach { 
                        if (it.id !in expandedMap) {
                            expandedMap[it.id] = true
                        }
                    }
                }

                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = viewModel::refresh,
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        tournaments.forEach { tournament ->
                            item(key = "tour_${tournament.id}") {
                                val expanded = expandedMap[tournament.id] ?: true
                                TournamentBanner(
                                    tournament = tournament,
                                    expanded = expanded,
                                    onClick = { expandedMap[tournament.id] = !expanded }
                                )
                            }

                            if (expandedMap[tournament.id] != false) {
                                items(tournament.matches, key = { "match_${it.id}" }) { match ->
                                    MatchRow(
                                        match = match,
                                        onClick = { onMatchClick(match.id) },
                                        modifier = Modifier
                                            .animateItem(
                                                fadeInSpec = tween(durationMillis = 250),
                                                fadeOutSpec = tween(durationMillis = 200),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessLow,
                                                    visibilityThreshold = IntOffset.VisibilityThreshold
                                                )
                                            )
                                            .padding(vertical = 1.dp)
                                    )
                                }
                                item { Spacer(Modifier.height(6.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFilters) {
        val view = androidx.compose.ui.platform.LocalView.current
        val window = (view.context as? android.app.Activity)?.window
        DisposableEffect(Unit) {
            window?.let { w ->
                val controller = androidx.core.view.WindowCompat.getInsetsController(w, view)
                w.navigationBarColor = android.graphics.Color.parseColor("#1D1B20")
                controller.isAppearanceLightNavigationBars = false
            }
            onDispose {
                window?.let { w ->
                    val controller = androidx.core.view.WindowCompat.getInsetsController(w, view)
                    w.navigationBarColor = android.graphics.Color.WHITE
                    controller.isAppearanceLightNavigationBars = true
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
                    .fillMaxHeight(0.6f)
                    .background(
                        color = AuraDeep,
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
                    )
                    .windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp, 4.dp)
                            .background(Color.White.copy(alpha = 0.3f), CircleShape)
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Filter",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { showFilters = false }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Anwenden",
                                tint = AuraLime,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                    ) {
                        Spacer(Modifier.height(24.dp))
                        
                        FilterSection(
                            title = "Tour",
                            options = TourFilter.entries,
                            selected = tourFilter,
                            onSelect = viewModel::setTourFilter
                        )
                        Spacer(Modifier.height(20.dp))
                        FilterSection(
                            title = "Format",
                            options = FormatFilter.entries,
                            selected = formatFilter,
                            onSelect = viewModel::setFormatFilter
                        )
                        Spacer(Modifier.height(20.dp))
                        FilterSection(
                            title = "Kategorie",
                            options = CategoryFilter.entries,
                            selected = categoryFilter,
                            onSelect = viewModel::setCategoryFilter
                        )
                    }
                }
            }
        }
    }

    // Applying milky blur when filters are shown
    if (showFilters) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .blur(16.dp)
                .background(Color.White.copy(alpha = 0.4f))
        )
    }
}

@Composable
fun CompactFilterRow(
    liveFilterActive: Boolean,
    finishedFilterActive: Boolean,
    liveCount: Int,
    activeFilterCount: Int,
    onToggleLive: () -> Unit,
    onToggleFinished: () -> Unit,
    onOpenFilters: () -> Unit,
    onCollapseAll: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Filter Button
        Surface(
            onClick = onOpenFilters,
            color = AuraDeep,
            shape = CircleShape,
            modifier = Modifier.height(40.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (activeFilterCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            activeFilterCount.toString(),
                            color = AuraDeep,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Icon(Icons.Outlined.FilterList, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Text("Filter", color = Color.White, style = MaterialTheme.typography.labelLarge)
            }
        }

        // Collapse All Button
        Surface(
            onClick = onCollapseAll,
            color = Color(0xFFF5F5F5),
            shape = CircleShape,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.UnfoldLess,
                    contentDescription = "Alle einklappen",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Live Toggle Button
        Surface(
            onClick = onToggleLive,
            color = if (liveFilterActive) Color(0xFFFFEBEE) else Color(0xFFF5F5F5),
            shape = CircleShape,
            modifier = Modifier.height(40.dp),
            border = if (liveFilterActive) BorderStroke(1.dp, Color.Red.copy(alpha = 0.5f)) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color.Red, CircleShape)
                )
                Text(
                    "Live",
                    color = Color.Red,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (liveFilterActive) FontWeight.Bold else FontWeight.Normal
                )
                if (liveCount > 0) {
                    Surface(
                        color = if (liveFilterActive) Color.Red else Color.Gray.copy(alpha = 0.2f),
                        shape = CircleShape
                    ) {
                        Text(
                            text = liveCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (liveFilterActive) Color.White else Color.DarkGray,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
        
        // Full-Time Toggle Button
        Surface(
            onClick = onToggleFinished,
            color = if (finishedFilterActive) Color(0xFFE8F5E9) else Color(0xFFF5F5F5),
            shape = CircleShape,
            modifier = Modifier.height(40.dp),
            border = if (finishedFilterActive) BorderStroke(1.dp, Color(0xFF4CAF50).copy(alpha = 0.5f)) else null
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            if (finishedFilterActive) Color(0xFF4CAF50) else Color.Gray,
                            CircleShape
                        )
                )
                Text(
                    "FT",
                    color = if (finishedFilterActive) Color(0xFF2E7D32) else Color.DarkGray,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (finishedFilterActive) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Date indicator (optional placeholder for "Heute")
        Text(
            "Heute",
            color = AuraPurple,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> FilterSection(
    title: String,
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, color = Color.White.copy(alpha = 0.6f), style = MaterialTheme.typography.labelMedium)
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                val isSelected = option == selected
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.05f else 1f,
                    animationSpec = spring(stiffness = Spring.StiffnessMedium),
                    label = "chipScale"
                )

                Surface(
                    onClick = { onSelect(option) },
                    color = if (isSelected) AuraLime else Color.White.copy(alpha = 0.1f),
                    shape = CircleShape,
                    modifier = Modifier.scale(scale)
                ) {
                    Text(
                        text = option.toString(),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        color = if (isSelected) AuraDeep else Color.White,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Black else FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun TournamentBanner(
    tournament: Tournament,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )
    val liveCount = tournament.matches.count { it.status == MatchStatus.LIVE }

    val surfaceColor = when (tournament.surface) {
        Surface.CLAY -> ClayColor
        Surface.GRASS -> GrassColor
        Surface.HARD -> HardColor
        Surface.INDOOR_HARD -> IndoorColor
        Surface.UNKNOWN -> Color.Gray
    }

    val surfaceName = when (tournament.surface) {
        Surface.CLAY -> "Sand"
        Surface.GRASS -> "Rasen"
        Surface.HARD -> "Hartplatz"
        Surface.INDOOR_HARD -> "Hartplatz (H)"
        Surface.UNKNOWN -> "Unbekannt"
    }

    val points = tournament.category.points.takeIf { it > 0 }?.let { "$it Pts" } ?: ""

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = AuraDeep,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Surface Color Accent Line
            Box(
                Modifier
                    .size(3.dp, 42.dp)
                    .background(surfaceColor, RoundedCornerShape(2.dp))
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                // City / short name (top)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = (tournament.location ?: tournament.name.replace(Regex("""\s*\(.*?\)"""), "").trim()).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (liveCount > 0) {
                        Spacer(Modifier.width(8.dp))
                        Box(modifier = Modifier.size(6.dp).background(Color.Red, CircleShape))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = "$liveCount LIVE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Category label + Singles/Doubles badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = tournament.category.displayName.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.55f),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (tournament.type != null) {
                        Spacer(Modifier.width(6.dp))
                        val isDoubles = tournament.type == "Doubles"
                        Box(
                            modifier = Modifier
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = if (isDoubles) "D" else "S",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.9f),
                                fontWeight = FontWeight.Bold,
                                fontSize = 9.sp
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                tint = Color.White.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun MatchRow(
    match: TennisMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isFinished = match.status == MatchStatus.FINISHED
    val isLive = match.status == MatchStatus.LIVE
    val isTbd = match.status == MatchStatus.TBD

    val cardAlpha = if (isFinished) 0.55f else 1f

    val isUpcoming = !isLive && !isFinished && !isTbd

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .graphicsLayer { alpha = cardAlpha },
        shape = RoundedCornerShape(10.dp),
        color = when {
            isLive -> Color(0xFFFFF3F3)
            isFinished -> Color(0xFFF2F2F2)
            else -> Color(0xFFF9F9F9)
        },
        border = BorderStroke(
            width = if (isLive) 1.5.dp else 1.dp,
            color = if (isLive) Color.Red.copy(alpha = 0.4f) else Color.LightGray.copy(alpha = 0.3f)
        )
    ) {
        if (isUpcoming) {
            Row(
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    Text(
                        text = match.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }
                // Players
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PlayerRow(player = match.homePlayer, isWinner = false, isServing = false, score = null)
                    PlayerRow(player = match.awayPlayer, isWinner = false, isServing = false, score = null)
                }
                // Dash instead of score
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("-", style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
                    Text("-", style = MaterialTheme.typography.titleMedium, color = Color.LightGray)
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        } else {
            // Live / Finished layout matching reference design
            val sets = match.score?.split(",")?.map { it.trim() } ?: emptyList()
            // Per-set scores for each player: sets[i] = "6-4" → home=6, away=4
            val homeSets = sets.map { it.split("-").getOrNull(0) ?: "" }
            val awaySets = sets.map { it.split("-").getOrNull(1) ?: "" }
            val gameScoreParts = match.gameScore?.split("-")
            val homeGame = gameScoreParts?.getOrNull(0) ?: ""
            val awayGame = gameScoreParts?.getOrNull(1) ?: ""

            Row(
                modifier = Modifier.padding(start = 10.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: status badge + round
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(56.dp)
                ) {
                    if (isLive) {
                        Surface(
                            color = Color.Red,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                LivePulsingDot()
                                Text("LIVE", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Text("FT", style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                    }
                }

                // Players
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center // Center the two rows together
                ) {
                    PlayerRow(
                        player = match.homePlayer,
                        isWinner = match.winnerKey == match.homePlayer.key,
                        isServing = match.isHomeServing == true && isLive,
                        score = null
                    )
                    Spacer(Modifier.height(8.dp))
                    PlayerRow(
                        player = match.awayPlayer,
                        isWinner = match.winnerKey == match.awayPlayer.key,
                        isServing = match.isHomeServing == false && isLive,
                        score = null
                    )
                }

                // Score columns
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Set scores
                    homeSets.forEachIndexed { i, homeScore ->
                        val awayScore = awaySets.getOrNull(i) ?: ""
                        
                        fun parseGames(s: String): Int {
                            return when {
                                "(" in s -> s.substring(0, s.indexOf("(")).trim().toIntOrNull() ?: 0
                                "." in s -> s.substring(0, s.indexOf(".")).trim().toIntOrNull() ?: 0
                                else -> s.trim().toIntOrNull() ?: 0
                            }
                        }

                        val h = parseGames(homeScore)
                        val a = parseGames(awayScore)
                        val homeWonSet = (h >= 6 && h - a >= 2) || (h == 7 && a == 6) || (h >= 10 && h - a >= 2)
                        val awayWonSet = (a >= 6 && a - h >= 2) || (a == 7 && h == 6) || (a >= 10 && a - h >= 2)
                        val setDone = homeWonSet || awayWonSet

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            ScoreText(homeScore, homeWonSet, setDone)
                            Spacer(Modifier.height(8.dp))
                            ScoreText(awayScore, awayWonSet, setDone)
                        }
                    }
                    // Current game score (live only)
                    if (isLive && match.gameScore != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                                Text(homeGame, style = MaterialTheme.typography.titleMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.height(32.dp), contentAlignment = Alignment.Center) {
                                Text(awayGame, style = MaterialTheme.typography.titleMedium, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }

        }
    }
}

@Composable
private fun ScoreText(
    score: String,
    isWinner: Boolean,
    setDone: Boolean,
    color: Color? = null
) {
    val games = when {
        "(" in score -> score.substring(0, score.indexOf("("))
        "." in score -> score.substring(0, score.indexOf("."))
        else -> score
    }
    val tbPoints = when {
        "(" in score -> score.substring(score.indexOf("(") + 1, score.indexOf(")"))
        "." in score -> score.substring(score.indexOf(".") + 1)
        else -> null
    }
    val hasDot = "." in score

    Row(
        modifier = Modifier
            .height(32.dp)
            .widthIn(min = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (hasDot) "$games." else games,
            style = MaterialTheme.typography.titleMedium.copy(
                lineHeight = 32.sp,
                platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                    includeFontPadding = false
                )
            ),
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = color ?: when {
                !setDone -> Color.Gray
                isWinner -> Color.Black
                else -> Color.Gray
            },
            textAlign = TextAlign.Center
        )
        if (tbPoints != null) {
            Text(
                text = tbPoints,
                fontSize = 10.sp, 
                fontWeight = FontWeight.Bold,
                color = color?.copy(alpha = 0.7f) ?: if (isWinner) Color.Black.copy(alpha = 0.6f) else Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.align(Alignment.Top)
            )
        }
    }
}

@Composable
fun PlayerRow(
    player: Player,
    isWinner: Boolean,
    isServing: Boolean,
    score: String?
) {
    Row(
        modifier = Modifier.height(32.dp), // Match ScoreText height
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Player image with live-ranking badge (lime circle) on the avatar
        Box(contentAlignment = Alignment.BottomCenter) {
            com.lenz.tennisapp.ui.components.PlayerAvatarWithRanking(
                player = player,
                size = 28.dp,
                rankingFontSize = 8.sp,
                badgeSize = 13.dp
            )
            // Small badge at the bottom of avatar
            Surface(
                color = AuraLime,
                shape = CircleShape,
                modifier = Modifier.size(10.dp).offset(y = 3.dp),
                border = BorderStroke(1.dp, AuraDeep)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("-", color = AuraDeep, fontSize = 7.sp, fontWeight = FontWeight.Black, modifier = Modifier.offset(y = (-1).dp))
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        if (isServing) {
            Surface(
                color = AuraLime,
                shape = CircleShape,
                modifier = Modifier.size(18.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SportsTennis,
                        contentDescription = "Serving",
                        tint = AuraDeep,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
        }

        // Name
        Text(
            text = player.name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isWinner) FontWeight.Bold else FontWeight.Normal,
            color = if (isWinner) Color.Black else Color.DarkGray,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LivePulsingDot() {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse)
    )
    Box(
        Modifier
            .size(8.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Red, CircleShape)
    )
}
