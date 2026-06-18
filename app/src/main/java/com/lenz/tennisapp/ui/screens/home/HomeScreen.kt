package com.lenz.tennisapp.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lenz.tennisapp.domain.model.MatchStatus
import com.lenz.tennisapp.domain.model.Surface as CourtSurface
import com.lenz.tennisapp.domain.model.Tournament
import com.lenz.tennisapp.domain.model.TournamentCategory
import com.lenz.tennisapp.domain.model.UserPrediction
import com.lenz.tennisapp.ui.components.GreenHeader
import com.lenz.tennisapp.ui.components.MatchCard
import com.lenz.tennisapp.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onMatchClick: (String) -> Unit,
    onTournamentClick: (String, String) -> Unit = { _, _ -> },
    showHeader: Boolean = true,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val todayState   by viewModel.uiState.collectAsStateWithLifecycle()
    val liveState    by viewModel.liveUiState.collectAsStateWithLifecycle()
    val liveOnly     by viewModel.liveOnly.collectAsStateWithLifecycle()
    val liveCount    by viewModel.liveMatchCount.collectAsStateWithLifecycle()

    val state = if (liveOnly) liveState else todayState
    val filter = state.liveFilter

    var showFilterSheet by remember { mutableStateOf(false) }

    val filteredTournaments = remember(state.tournaments, filter) {
        state.tournaments
            .filter { filter.matches(it) }
            .sortedWith(compareBy({ it.category.sortOrder }, { it.name }))
    }

    val activeFilterCount = remember(filter) {
        filter.tours.size + filter.categories.size + filter.matchTypes.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F1A0F))
    ) {
        if (showHeader) {
            GreenHeader(
                title    = "Tennis Today",
                subtitle = state.selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d. MMMM", Locale.GERMAN))
            )
        }

        // ── Compact action bar ────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Filter button
            FilterButton(
                activeCount = activeFilterCount,
                onClick = { showFilterSheet = true }
            )

            // Live toggle button
            LiveButton(
                liveCount = liveCount,
                isActive  = liveOnly,
                onClick   = viewModel::toggleLiveOnly
            )

            Spacer(modifier = Modifier.weight(1f))

            // Date navigation (only when not in live mode)
            if (!liveOnly) {
                DateNavigator(
                    date       = state.selectedDate,
                    onPrevious = { viewModel.selectDate(state.selectedDate.minusDays(1)) },
                    onNext     = { viewModel.selectDate(state.selectedDate.plusDays(1)) },
                    onToday    = { viewModel.selectDate(LocalDate.now()) }
                )
            }
        }

        // ── Content ───────────────────────────────────────────────────────────
        if (state.isLoading && filteredTournaments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TennisGreenBright, modifier = Modifier.size(36.dp))
            }
        } else if (filteredTournaments.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (liveOnly) "Keine Live-Spiele" else "Keine Spiele gefunden",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredTournaments, key = { it.id }) { tournament ->
                    TournamentCard(
                        tournament   = tournament,
                        predictions  = state.predictions,
                        onMatchClick = onMatchClick,
                        onPredict    = { matchId, matchDate, tName, hKey, hName, aKey, aName, wKey, wName ->
                            viewModel.predict(matchId, matchDate, tName, hKey, hName, aKey, aName, wKey, wName)
                        }
                    )
                }
            }
        }
    }

    // ── Filter bottom sheet ───────────────────────────────────────────────────
    if (showFilterSheet) {
        FilterBottomSheet(
            filter           = filter,
            onDismiss        = { showFilterSheet = false },
            onTourToggle     = viewModel::toggleTourFilter,
            onCategoryToggle = viewModel::toggleCategoryFilter,
            onMatchTypeToggle= viewModel::toggleMatchTypeFilter,
            onClear          = { viewModel.clearLiveFilters(); showFilterSheet = false }
        )
    }
}

// ─── Action bar buttons ───────────────────────────────────────────────────────

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    Surface(
        shape  = RoundedCornerShape(20.dp),
        color  = if (activeCount > 0) TennisGreen else Color(0xFF1E2E1E),
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(Icons.Default.Tune, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Text(
                if (activeCount > 0) "$activeCount Filter" else "Filter",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun LiveButton(liveCount: Int, isActive: Boolean, onClick: () -> Unit) {
    val bgColor by animateColorAsState(
        targetValue = if (isActive) Color.Red else Color(0xFF1E2E1E),
        animationSpec = tween(200),
        label = "live_bg"
    )

    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = bgColor,
        modifier = Modifier
            .height(36.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Pulsing dot when active
            if (isActive) {
                val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 1f, targetValue = 0.3f,
                    animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
                    label = "pulse_alpha"
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = alpha))
                )
            } else {
                Icon(
                    Icons.Default.FiberManualRecord,
                    null,
                    tint = Color.Red,
                    modifier = Modifier.size(10.dp)
                )
            }
            Text("Live", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (liveCount > 0) {
                Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.25f)) {
                    Text(
                        liveCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

// ─── Filter bottom sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterBottomSheet(
    filter: LiveFilterState,
    onDismiss: () -> Unit,
    onTourToggle: (TourType) -> Unit,
    onCategoryToggle: (FilterCategory) -> Unit,
    onMatchTypeToggle: (MatchType) -> Unit,
    onClear: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest  = onDismiss,
        sheetState        = sheetState,
        containerColor    = Color.Transparent,
        contentColor      = Color.White,
        dragHandle        = null,
        windowInsets      = WindowInsets(0)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
        ) {
            // Gradient background: transparent at top, dark green filling in
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f   to Color.Transparent,
                            0.18f to Color(0xFF0D1F0D),
                            1f   to Color(0xFF0A1A0A)
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 32.dp, start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filter",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    if (filter.isActive) {
                        TextButton(onClick = onClear) {
                            Text("Zurücksetzen", color = TennisGreenBright, fontSize = 13.sp)
                        }
                    }
                }

                // Tour section
                FilterSection(label = "Tour") {
                    TourType.entries.filter { it != TourType.JUNIORS }.forEach { tour ->
                        FilterPill(
                            label    = tour.label,
                            selected = tour in filter.tours,
                            color    = TennisGreen,
                            onClick  = { onTourToggle(tour) }
                        )
                    }
                }

                // Category section
                FilterSection(label = "Kategorie") {
                    FilterCategory.entries.forEach { cat ->
                        FilterPill(
                            label    = cat.label,
                            selected = cat in filter.categories,
                            color    = categoryColor(cat),
                            onClick  = { onCategoryToggle(cat) }
                        )
                    }
                }

                // Match type section
                FilterSection(label = "Spieltyp") {
                    MatchType.entries.forEach { mt ->
                        FilterPill(
                            label    = mt.label,
                            selected = mt in filter.matchTypes,
                            color    = AuraPurple,
                            onClick  = { onMatchTypeToggle(mt) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterSection(label: String, content: @Composable RowScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            label.uppercase(),
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { content() }
    }
}

@Composable
private fun FilterPill(label: String, selected: Boolean, color: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(
        targetValue = if (selected) color else Color.White.copy(alpha = 0.07f),
        animationSpec = tween(150), label = "pill_bg"
    )
    Surface(
        shape    = RoundedCornerShape(20.dp),
        color    = bg,
        modifier = Modifier
            .height(34.dp)
            .border(
                1.dp,
                if (selected) Color.Transparent else Color.White.copy(alpha = 0.15f),
                RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─── Tournament card ──────────────────────────────────────────────────────────

@Composable
private fun TournamentCard(
    tournament: Tournament,
    predictions: Map<String, UserPrediction>,
    onMatchClick: (String) -> Unit,
    onPredict: (String, String, String, String, String, String, String, String, String) -> Unit
) {
    var expanded by remember { mutableStateOf(true) }

    val liveCount = tournament.matches.count { it.status == MatchStatus.LIVE }

    val sortedMatches = remember(tournament.matches) {
        val (live, rest) = tournament.matches.partition { it.status == MatchStatus.LIVE }
        val (upcoming, finished) = rest.partition {
            it.status == MatchStatus.NOT_STARTED || it.status == MatchStatus.POSTPONED
        }
        live + upcoming.sortedBy { it.time } + finished.sortedByDescending { it.time }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A2A1A),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, TennisGreen.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(36.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(TennisGreenBright)
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        tournament.name.uppercase(),
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp),
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            tournament.category.displayName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = categoryColor(tournament.category),
                            fontWeight = FontWeight.Bold
                        )
                        Text("·", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                        Text(
                            "${categoryPoints(tournament.category, tournament.name)} Pts",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                        Text("·", color = Color.White.copy(alpha = 0.3f), fontSize = 10.sp)
                        SurfaceDot(tournament.surface)
                        Text(
                            surfaceName(tournament.surface),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }

                if (liveCount > 0) {
                    Surface(shape = RoundedCornerShape(6.dp), color = Color.Red) {
                        Text(
                            "$liveCount LIVE",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter   = expandVertically() + fadeIn(),
                exit    = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    HorizontalDivider(color = TennisGreen.copy(alpha = 0.2f), modifier = Modifier.padding(bottom = 4.dp))
                    sortedMatches.forEach { match ->
                        MatchCard(
                            match = match,
                            onClick = { onMatchClick(match.id) },
                            userPrediction = predictions[match.id],
                            onPredict = if (match.status == MatchStatus.NOT_STARTED) {
                                { wKey, wName ->
                                    onPredict(
                                        match.id, match.date, match.tournament,
                                        match.homePlayer.key, match.homePlayer.name,
                                        match.awayPlayer.key, match.awayPlayer.name,
                                        wKey, wName
                                    )
                                }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

@Composable
private fun SurfaceDot(surface: CourtSurface) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(surfaceColor(surface))
    )
}

private fun surfaceColor(surface: CourtSurface) = when (surface) {
    CourtSurface.CLAY        -> ClayColor
    CourtSurface.GRASS       -> GrassColor
    CourtSurface.HARD        -> HardColor
    CourtSurface.INDOOR_HARD -> IndoorColor
    CourtSurface.UNKNOWN     -> Color.Gray
}

private fun surfaceName(surface: CourtSurface) = when (surface) {
    CourtSurface.CLAY        -> "Sand"
    CourtSurface.GRASS       -> "Rasen"
    CourtSurface.HARD        -> "Hard"
    CourtSurface.INDOOR_HARD -> "Indoor"
    CourtSurface.UNKNOWN     -> "—"
}

private fun categoryPoints(cat: TournamentCategory, tournamentName: String = "") = when (cat) {
    TournamentCategory.GRAND_SLAM                                    -> "2000"
    TournamentCategory.ATP_MASTERS_1000, TournamentCategory.WTA_1000 -> "1000"
    TournamentCategory.ATP_500,          TournamentCategory.WTA_500  -> "500"
    TournamentCategory.ATP_250,          TournamentCategory.WTA_250  -> "250"
    TournamentCategory.CHALLENGER -> {
        // ATP Challengers: extract point level from name (e.g. "Bordeaux 175" → "175")
        val match = Regex("(175|125|100|75)").find(tournamentName)
        match?.value ?: "Chal."
    }
    TournamentCategory.ITF -> {
        // ITF events: extract M/W prefix (e.g. "M25 Klosters" → "25", "W75 Blois" → "75")
        val match = Regex("^[MWmw](\\d+)").find(tournamentName.trim())
        match?.groupValues?.get(1) ?: "ITF"
    }
    TournamentCategory.OTHER -> "—"
}

private fun categoryColor(cat: TournamentCategory) = when (cat) {
    TournamentCategory.GRAND_SLAM                                      -> SlamGold
    TournamentCategory.ATP_MASTERS_1000, TournamentCategory.WTA_1000   -> Masters1000Color
    TournamentCategory.ATP_500,          TournamentCategory.WTA_500    -> Atp500Color
    TournamentCategory.ATP_250,          TournamentCategory.WTA_250    -> Atp250Color
    TournamentCategory.CHALLENGER                                      -> ChallengerColor
    else                                                               -> Color.Gray
}

private fun categoryColor(cat: FilterCategory): Color = when (cat) {
    FilterCategory.GRAND_SLAM -> SlamGold
    FilterCategory.MASTERS    -> Masters1000Color
    FilterCategory.FIVE00     -> Atp500Color
    FilterCategory.TWO50      -> Atp250Color
    FilterCategory.CHALLENGER -> ChallengerColor
    FilterCategory.ITF        -> Color.Gray
}

@Composable
private fun DateNavigator(
    date: LocalDate,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val isToday = date == LocalDate.now()
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (!isToday) {
            TextButton(onClick = onToday) {
                Text("Heute", color = AuraLime, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black)
            }
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ChevronLeft, null, tint = Color.White.copy(alpha = 0.7f))
        }
        IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.ChevronRight, null, tint = Color.White.copy(alpha = 0.7f))
        }
    }
}
