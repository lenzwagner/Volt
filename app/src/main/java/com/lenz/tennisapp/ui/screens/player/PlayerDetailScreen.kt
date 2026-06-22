package com.lenz.tennisapp.ui.screens.player

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lenz.tennisapp.TennisApplication
import com.lenz.tennisapp.ui.components.PlayerAvatarWithRanking
import com.lenz.tennisapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerDetailScreen(
    playerKey: String,
    playerName: String,
    onBack: () -> Unit,
    onPlayerClick: (String, String) -> Unit = { _, _ -> },
    viewModel: PlayerDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val eloScores by viewModel.eloScores.collectAsStateWithLifecycle()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Details", "Spiele", "Statistiken")

    LaunchedEffect(playerKey) {
        viewModel.loadPlayerData(playerKey, playerName)
    }

    Scaffold(
        topBar = {
            PlayerHeroHeader(
                playerName = playerName,
                playerImageUrl = state.playerImageUrl,
                ranking = state.ranking,
                notificationsEnabled = state.notificationsEnabled,
                isFavorite = state.isFavorite,
                onBack = onBack,
                onNotificationToggle = viewModel::toggleNotifications,
                onFavoriteToggle = viewModel::toggleFavorite
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .background(Color.White)
        ) {
            // TabRow in Aura Style
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = Color.White,
                contentColor = AuraPurple,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = AuraPurple
                    )
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleSmall,
                                color = if (selectedTabIndex == index) AuraPurple else Color.Gray,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Black else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            PullToRefreshBox(
                isRefreshing = state.isLoading,
                onRefresh = { viewModel.loadPlayerData(playerKey, playerName) },
                modifier = Modifier.fillMaxSize()
            ) {
                when (selectedTabIndex) {
                    0 -> DetailsTab(state, eloScores)
                    1 -> MatchesTab(state, onPlayerClick)
                    2 -> StatsTab(state)
                }
            }
        }
    }
}

@Composable
private fun DetailsTab(state: PlayerDetailUiState, eloScores: Map<String, Any>?) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Current Form
        item {
            CardSection(title = "Aktuelle Form", hasInfo = true) {
                FormRow(state.recentMatches)
            }
        }

        // 2. Rankings
        item {
            CardSection(title = "Ranglisten") {
                RankingsContent(state)
            }
        }

        // 2b. Elo Rankings
        if (eloScores != null) {
            item {
                CardSection(title = "Elo Rating (TennisAbstract)") {
                    EloRankingsContent(eloScores)
                }
            }
        }

        // 3. Grand Slam Record
        item {
            CardSection(title = "Grand-Slam-Rekord", hasInfo = true) {
                GrandSlamRecordTable(state.grandSlamRecord)
            }
        }

        // 4. Profile
        item {
            CardSection(title = "Profil") {
                ProfileContent(state.profile)
            }
        }

        // 5. Prize Money
        item {
            CardSection(title = "Preisgeld") {
                PrizeMoneyContent(state.prizeMoneyFormatted)
            }
        }
    }
}

@Composable
private fun MatchesTab(state: PlayerDetailUiState, onPlayerClick: (String, String) -> Unit) {
    var filterIndex by remember { mutableIntStateOf(0) }
    val filters = listOf("Alle", "Einzel", "Doppel")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            filters.forEachIndexed { index, label ->
                FilterChip(
                    selected = filterIndex == index,
                    onClick = { filterIndex = index },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AuraPurple,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        val groupedMatches = remember(state.recentMatches) {
            state.recentMatches.groupBy { it.tournament }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            groupedMatches.forEach { (tournament, matches) ->
                item {
                    TournamentHeader(tournament, matches.firstOrNull()?.surface ?: "Hartplatz")
                }
                items(matches.size) { idx ->
                    val match = matches[idx]
                    MatchRow(match, state.playerName, onPlayerClick)
                    if (idx < matches.size - 1) {
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = Color.LightGray.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TournamentHeader(name: String, surface: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(AuraPurple.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
        )
        Spacer(Modifier.width(8.dp))
        Column {
            Text(name, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(surface, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        }
    }
}

@Composable
private fun StatsTab(state: PlayerDetailUiState) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = true, onClick = {}, label = { Text("Alle Turniere") })
                FilterChip(selected = true, onClick = {}, label = { Text("2024") })
            }
        }

        item {
            CardSection(title = "Leistung") {
                state.stats.take(2).forEach { stat ->
                    StatRow(stat.name, stat.value)
                }
            }
        }

        item {
            CardSection(title = "Aufschlag") {
                state.stats.drop(1).take(5).forEach { stat ->
                    StatRow(stat.name, stat.value)
                }
            }
        }
        
        item {
            CardSection(title = "Druckmesswerte") {
                state.stats.takeLast(2).forEach { stat ->
                    StatRow(stat.name, stat.value)
                }
            }
        }

        item {
            Surface(
                color = Color.White,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Info, null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Text(
                        "Statistiken basierend auf verfügbaren Daten für Spiele auf Challenger-Level und höher.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

// --- Sub-Components ---

@Composable
private fun CardSection(title: String, hasInfo: Boolean = false, content: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = AuraDeep
            )
            if (hasInfo) {
                Icon(Icons.Default.Info, null, tint = AuraPurple, modifier = Modifier.size(18.dp))
            }
        }
        Surface(
            color = Color.White.copy(alpha = 0.6f),
            shape = RoundedCornerShape(
                topStart = 40.dp, 
                topEnd = 12.dp, 
                bottomStart = 12.dp, 
                bottomEnd = 40.dp
            ),
            shadowElevation = 0.dp,
            border = BorderStroke(1.dp, Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun FormRow(matches: List<RecentMatch>) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        matches.take(8).reversed().forEach { match ->
            val isWin = match.result.startsWith("W")
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(AuraPurple.copy(alpha = 0.1f))
                )
                Spacer(Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isWin) 32.dp else 16.dp)
                        .background(if (isWin) AuraLime else Color.LightGray.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun RankingsContent(state: PlayerDetailUiState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(AuraPurple.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(AuraDeep, RoundedCornerShape(8.dp))
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(state.isTour, color = AuraLime, fontWeight = FontWeight.Black, fontSize = 12.sp)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("UTR", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Karriere-bestwert", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text("${state.careerHigh ?: "—"}.", fontWeight = FontWeight.Black, color = AuraDeep)
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Aktueller Rang", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = AuraPurple, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    "${state.ranking ?: "—"}. (${state.rankingPoints ?: 0} PTS)",
                    color = AuraPurple,
                    fontWeight = FontWeight.Black
                )
            }
        }
    }
}

@Composable
private fun EloRankingsContent(eloScores: Map<String, Any>) {
    val overall = (eloScores["overall"] as? Number)?.toInt() ?: 0
    val hard = (eloScores["hard"] as? Number)?.toInt() ?: 0
    val clay = (eloScores["clay"] as? Number)?.toInt() ?: 0
    val grass = (eloScores["grass"] as? Number)?.toInt() ?: 0

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileRow("Gesamt", overall.toString())
        ProfileRow("Hartplatz", hard.toString())
        ProfileRow("Sandplatz", clay.toString())
        ProfileRow("Rasen", grass.toString())
    }
}

@Composable
private fun GrandSlamRecordTable(records: List<GrandSlamYear>) {
    if (records.isEmpty()) {
        Text("Keine Daten verfügbar", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        return
    }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
            Spacer(Modifier.weight(1.5f))
            records.forEach { record ->
                Text(record.year, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 10.sp, color = AuraDeep, fontWeight = FontWeight.Bold)
            }
        }
        
        val slams = listOf(
            "Australian Open" to { r: GrandSlamYear -> r.aus },
            "French Open" to { r: GrandSlamYear -> r.fre },
            "Wimbledon" to { r: GrandSlamYear -> r.wim },
            "US Open" to { r: GrandSlamYear -> r.usa }
        )
        
        slams.forEach { (name, selector) ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, modifier = Modifier.weight(1.5f), fontSize = 11.sp, color = Color.Gray)
                records.forEach { record ->
                    val res = selector(record)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(2.dp)
                            .background(
                                if (res == "-") Color(0xFFF5F5F5) else AuraPurple.copy(alpha = 0.1f), 
                                RoundedCornerShape(8.dp)
                            )
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(res, fontSize = 10.sp, fontWeight = FontWeight.Black, color = if (res == "-") Color.LightGray else AuraPurple)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileContent(profile: PlayerProfile) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileRow("Vollständiger Name", profile.fullName)
        ProfileRow("Land", profile.country)
        ProfileRow("Geburtsort", profile.birthPlace)
        ProfileRow("Alter", profile.age)
        ProfileRow("Größe", profile.height)
        ProfileRow("Spielhand", profile.plays)
    }
}

@Composable
private fun PrizeMoneyContent(formatted: String?) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ProfileRow("Dieses Jahr", formatted ?: "—")
        ProfileRow("Gesamtkarriere", "—")
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AuraDeep)
    }
}

@Composable
private fun MatchRow(
    match: RecentMatch, 
    playerName: String,
    onPlayerClick: (String, String) -> Unit
) {
    val isWin = match.result.startsWith("W")
    val score = match.result.substringAfter(" ")
    val scoreParts = score.split(",")
    
    Surface(
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.width(64.dp)) {
                Text(match.date.split("-").drop(1).joinToString("."), style = MaterialTheme.typography.labelSmall, color = Color.Gray, fontWeight = FontWeight.Bold)
                Text("FT", style = MaterialTheme.typography.labelSmall, color = AuraPurple, fontWeight = FontWeight.Black)
            }

            Column(modifier = Modifier.weight(1f)) {
                PlayerMatchLine(
                    name = match.opponent, 
                    won = isWin.not(), 
                    scoreParts = scoreParts, 
                    isOpponent = true,
                    onClick = { onPlayerClick(match.opponentKey, match.opponent) }
                )
                Spacer(Modifier.height(6.dp))
                PlayerMatchLine(
                    name = playerName,
                    won = isWin, 
                    scoreParts = scoreParts, 
                    isOpponent = false
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isWin) AuraLime else Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(if (isWin) "W" else "L", color = if (isWin) AuraDeep else Color.Red, fontWeight = FontWeight.Black, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PlayerMatchLine(
    name: String, 
    won: Boolean, 
    scoreParts: List<String>, 
    isOpponent: Boolean,
    onClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier
    ) {
        Box(Modifier.size(14.dp, 10.dp).background(Color(0xFFF0F0F0), RoundedCornerShape(2.dp)))
        Spacer(Modifier.width(8.dp))
        Text(
            name.split(" ").last(), 
            style = MaterialTheme.typography.bodySmall, 
            fontWeight = if (won) FontWeight.Black else FontWeight.Medium,
            color = if (won) AuraDeep else Color.Gray,
            modifier = Modifier.weight(1f)
        )
        scoreParts.forEach { part ->
            val scoreString = if (part.contains("-")) {
                val p = part.split("-")
                if (isOpponent) p.firstOrNull()?.trim() ?: "" else p.lastOrNull()?.trim() ?: ""
            } else ""
            
            ScoreDigit(
                score = scoreString,
                won = won
            )
        }
    }
}

@Composable
private fun ScoreDigit(score: String, won: Boolean) {
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
        modifier = Modifier.width(24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = if (hasDot) "$games." else games,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = if (won) FontWeight.Black else FontWeight.Normal,
            color = if (won) AuraPurple else Color.LightGray,
            textAlign = TextAlign.Center
        )
        if (tbPoints != null) {
            Text(
                text = tbPoints,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = if (won) AuraPurple.copy(alpha = 0.7f) else Color.LightGray,
            )
        }
    }
}

@Composable
private fun StatRow(name: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(name, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontWeight = FontWeight.Medium)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, color = AuraDeep)
    }
}

@Composable
private fun PlayerHeroHeader(
    playerName: String,
    playerImageUrl: String?,
    ranking: Int?,
    notificationsEnabled: Boolean,
    isFavorite: Boolean,
    onBack: () -> Unit,
    onNotificationToggle: () -> Unit,
    onFavoriteToggle: () -> Unit
) {
    val court = TennisApplication.sessionCourt

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        color = AuraPurple
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Large Organic Image Background
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-20).dp)
                    .size(240.dp)
                    .clip(CircleShape)
                    .alpha(0.3f)
            ) {
                AsyncImage(
                    model = court.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Radial Glow
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                            center = Offset(400f, 400f),
                            radius = 1000f
                        )
                    )
            )

            // Large Rank Watermark
            ranking?.let {
                Text(
                    text = "#$it",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 30.dp, y = 10.dp)
                        .rotate(-15f)
                        .alpha(0.12f),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 180.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.statusBars)
            ) {
                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.background(AuraDeep.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    Row(
                        modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape).padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNotificationToggle) { 
                            Icon(
                                imageVector = if (notificationsEnabled) Icons.Default.NotificationsActive else Icons.Default.NotificationsNone, 
                                contentDescription = null, 
                                tint = if (notificationsEnabled) AuraLime else Color.White 
                            ) 
                        }
                        IconButton(onClick = onFavoriteToggle) { 
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Star else Icons.Default.StarBorder, 
                                contentDescription = null, 
                                tint = if (isFavorite) AuraLime else Color.White 
                            ) 
                        }
                    }
                }

                // Player Info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Styled Avatar Container
                    Box(contentAlignment = Alignment.Center) {
                        // Shadow/Glow behind avatar
                        Box(modifier = Modifier.size(105.dp).clip(CircleShape).background(AuraLime.copy(alpha = 0.4f)))
                        
                        PlayerAvatarWithRanking(
                            player = com.lenz.tennisapp.domain.model.Player(
                                key = "",
                                name = playerName,
                                logoUrl = playerImageUrl,
                                ranking = ranking
                            ),
                            size = 100.dp,
                            rankingFontSize = 12.sp,
                            badgeSize = 34.dp
                        )
                    }

                    Column(
                        modifier = Modifier.padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp)
                    ) {
                        Surface(
                            color = AuraLime,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "PRO PLAYER", 
                                color = AuraDeep, 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                        
                        Text(
                            playerName.uppercase(),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontStyle = FontStyle.Normal,
                                letterSpacing = (-0.5).sp,
                                lineHeight = 30.sp
                            )
                        )
                        
                        Text(
                            "GERMANY • ATP TOUR", 
                            color = AuraDeep, 
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
