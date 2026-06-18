package com.lenz.tennisapp.ui.screens.match

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
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
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.ui.components.*
import com.lenz.tennisapp.ui.theme.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MatchDetailScreen(
    matchId: String,
    onBack: () -> Unit,
    onPlayerClick: (playerKey: String, playerName: String) -> Unit = { _, _ -> },
    onTournamentClick: (leagueId: String, name: String) -> Unit = { _, _ -> },
    viewModel: MatchDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        when (val s = state) {
            is MatchDetailUiState.Error -> {
                ErrorView(message = s.message, onBack = onBack, onRetry = viewModel::loadDetail)
            }
            is MatchDetailUiState.Loaded -> {
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::loadDetail,
                    modifier = Modifier.fillMaxSize()
                ) {
                    MatchDetailContent(
                        detail         = s.detail,
                        userPrediction = s.userPrediction,
                        onPredict      = viewModel::predict,
                        onBack         = onBack,
                        onRefresh      = viewModel::loadDetail,
                        onPlayerClick  = onPlayerClick,
                        onTournamentClick = onTournamentClick
                    )
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AuraPurple)
                }
            }
        }
    }
}

@Composable
private fun MatchDetailContent(
    detail: MatchDetail,
    userPrediction: UserPrediction? = null,
    onPredict: (winnerKey: String, winnerName: String) -> Unit = { _, _ -> },
    onBack: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPlayerClick: (playerKey: String, playerName: String) -> Unit = { _, _ -> },
    onTournamentClick: (leagueId: String, name: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Hero Header (Image + Player Avatars)
        item {
            MatchHeroHeader(
                match = detail.match,
                onBack = onBack,
                onRefresh = onRefresh,
                onPlayerClick = onPlayerClick
            )
        }

        // 1. Tournament Breadcrumb Banner
        item {
            TournamentBreadcrumbBanner(
                tournament = detail.match.tournament,
                category = detail.match.tournamentCategory,
                round = detail.match.round,
                onClick = { onTournamentClick(detail.match.leagueId, detail.match.tournament) },
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // 2. Odds Card (Spielende)
        if (detail.odds.isNotEmpty()) {
            item {
                OddsSummaryCard(
                    odds = detail.odds,
                    modifier = Modifier.padding(horizontal = 12.dp).animateItem()
                )
            }
        }

        // 3. Match Summary (FT - Chart - Score)
        item {
            MatchSummaryCard(
                match = detail.match,
                modifier = Modifier.padding(horizontal = 12.dp).animateItem()
            )
        }

        // 4. Set Breakdown (Expandable Sections)
        val sets = detail.match.score?.split(",") ?: emptyList()
        sets.reversed().forEachIndexed { index, score ->
            item {
                val setNum = sets.size - index
                SetCollapsibleSection(
                    title = "$setNum. Satz",
                    match = detail.match,
                    setScore = score,
                    isInitiallyExpanded = index == 0,
                    modifier = Modifier.padding(horizontal = 12.dp).animateItem()
                )
            }
        }

        // 5. Who will win? (User Pick section)
        item {
            WhoWillWinCard(
                prediction = detail.prediction,
                p1 = detail.match.homePlayer,
                p2 = detail.match.awayPlayer,
                userPrediction = userPrediction,
                onPredict = onPredict,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // 6. Rankings Comparison (WTA/ATP + ELO)
        item {
            RankingComparisonCard(
                p1 = detail.match.homePlayer,
                p2 = detail.match.awayPlayer,
                p1Elo = detail.player1Elo,
                p2Elo = detail.player2Elo,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        // 7. Match Info Card (Stadium, City, Surface)
        item {
            MatchInfoCard(
                match = detail.match,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

// ─── Sub-Components ──────────────────────────────────────────────────────────

@Composable
private fun TournamentBreadcrumbBanner(
    tournament: String,
    category: TournamentCategory,
    round: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth().clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(AuraLime, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("🎾", fontSize = 12.sp)
            }
            Text(
                text = "Tennis, ${category.displayName}, $tournament${round?.let { ", $it" } ?: ""}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = AuraDeep,
                modifier = Modifier.weight(1f),
                maxLines = 1
            )
            Icon(Icons.Default.ChevronRight, null, tint = AuraPurple, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun OddsSummaryCard(
    odds: List<BookmakerOdds>,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("QUOTEN", style = MaterialTheme.typography.labelSmall, color = AuraPurple, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
            Spacer(Modifier.height(12.dp))
            
            odds.take(2).forEach { o ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        modifier = Modifier.width(64.dp).height(30.dp),
                        color = AuraDeep,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(o.bookmakerName.take(3).uppercase(), color = AuraLime, fontSize = 10.sp, fontWeight = FontWeight.Black)
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("1", color = AuraPurple, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text(String.format(Locale.US, "%.2f", o.homeOdds), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraDeep)
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.weight(1f),
                        color = Color.White,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text("2", color = AuraPurple, fontSize = 13.sp, fontWeight = FontWeight.Black)
                            Spacer(Modifier.weight(1f))
                            Text(String.format(Locale.US, "%.2f", o.awayOdds), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = AuraDeep)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchSummaryCard(
    match: TennisMatch,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val statusText = when (match.status) {
                    MatchStatus.FINISHED -> "GAME OVER"
                    MatchStatus.LIVE -> "LIVE NOW"
                    else -> "UPCOMING"
                }
                Surface(color = AuraDeep, shape = RoundedCornerShape(6.dp)) {
                    Text(
                        statusText, 
                        style = MaterialTheme.typography.labelSmall, 
                        color = AuraLime, 
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Info, null, tint = AuraPurple.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Match progress visual (Dynamic Waveform style)
            val infiniteTransition = rememberInfiniteTransition(label = "waveform")
            Row(
                modifier = Modifier.fillMaxWidth().height(60.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(24) { i ->
                    val isHome = i % 3 == 0
                    val animHeight by infiniteTransition.animateFloat(
                        initialValue = if (isHome) 20f else 10f,
                        targetValue = if (isHome) 45f else 25f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000 + (i * 50), easing = LinearOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "bar_height"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(animHeight.dp)
                            .background(
                                if (isHome) AuraPurple else AuraLime, 
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
            }
            
            Spacer(Modifier.height(20.dp))
            
            // Large Result Boxes
            val scores = match.score?.split(",") ?: emptyList()
            var p1Sets = 0
            var p2Sets = 0
            scores.forEach { s ->
                val p = s.split("-")
                if (p.size == 2) {
                    val s1 = p[0].trim().toIntOrNull() ?: 0
                    val s2 = p[1].trim().toIntOrNull() ?: 0
                    if (s1 > s2) p1Sets++ else if (s2 > s1) p2Sets++
                }
            }
            
            Box(contentAlignment = Alignment.Center) {
                // Background Watermark for Score
                Text(
                    text = "$p1Sets-$p2Sets",
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = AuraPurple.copy(alpha = 0.05f),
                    modifier = Modifier.offset(y = (-10).dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    BigScoreBox(p1Sets.toString(), isActive = p1Sets >= p2Sets)
                    BigScoreBox(p2Sets.toString(), isActive = p2Sets > p1Sets)
                }
            }
        }
    }
}

@Composable
private fun BigScoreBox(value: String, isActive: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.1f else 1.0f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow),
        label = "score_box_scale"
    )

    Surface(
        modifier = Modifier.size(64.dp, 72.dp).scale(scale),
        color = if (isActive) AuraLime else Color.White.copy(alpha = 0.4f),
        shape = RoundedCornerShape(
            topStart = 20.dp, 
            topEnd = 8.dp, 
            bottomStart = 8.dp, 
            bottomEnd = 20.dp
        ),
        border = if (!isActive) BorderStroke(1.dp, Color.White) else null
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(value, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = if (isActive) AuraDeep else Color.LightGray.copy(alpha = 0.5f))
        }
    }
}

@Composable
private fun SetCollapsibleSection(
    title: String,
    match: TennisMatch,
    setScore: String,
    isInitiallyExpanded: Boolean,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(isInitiallyExpanded) }
    
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().clickable { isExpanded = !isExpanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AuraDeep, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, null, tint = AuraPurple)
            }
            
            if (isExpanded) {
                Spacer(Modifier.height(20.dp))
                val games = listOf("6", "5", "4", "3", "2", "1", "0")
                games.forEach { g ->
                    GameDetailRow(
                        gameNum = g,
                        p1Name = match.homePlayer.name,
                        p2Name = match.awayPlayer.name,
                        isBreak = g.toInt() % 2 != 0
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun GameDetailRow(gameNum: String, p1Name: String, p2Name: String, isBreak: Boolean) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(p1Name.split(" ").last().uppercase(), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, modifier = Modifier.width(90.dp), color = AuraDeep)
            Text(gameNum, fontWeight = FontWeight.Black, modifier = Modifier.width(28.dp), color = AuraPurple)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("15", "30", "40", "A").forEach { pts ->
                    Text(pts, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (pts == "A") AuraLime else Color.LightGray)
                }
            }
        }
        if (isBreak) {
            Surface(color = AuraLime, shape = RoundedCornerShape(4.dp), modifier = Modifier.padding(start = 90.dp, top = 4.dp)) {
                Text("BREAK", style = MaterialTheme.typography.labelSmall, color = AuraDeep, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 6.dp)) {
            Text(p2Name.split(" ").last().uppercase(), style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(90.dp), color = Color.Gray)
            Text("1", fontWeight = FontWeight.Medium, modifier = Modifier.width(28.dp), color = Color.Gray)
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf("0", "15", "30", "30").forEach { pts ->
                    Text(pts, fontSize = 11.sp, color = Color.LightGray)
                }
            }
            Box(modifier = Modifier.size(10.dp).background(AuraPurple, CircleShape))
        }
    }
}

@Composable
private fun WhoWillWinCard(
    prediction: MatchPrediction,
    p1: Player,
    p2: Player,
    userPrediction: UserPrediction?,
    onPredict: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("VOTING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AuraPurple, letterSpacing = 1.sp)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Info, null, tint = AuraPurple.copy(alpha = 0.3f), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(20.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PredictionPickItem(p1, prediction.player1WinPercent, isSelected = userPrediction?.predictedWinnerKey == p1.key, onClick = { onPredict(p1.key, p1.name) }, modifier = Modifier.weight(1f))
                PredictionPickItem(p2, prediction.player2WinPercent, isSelected = userPrediction?.predictedWinnerKey == p2.key, onClick = { onPredict(p2.key, p2.name) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PredictionPickItem(player: Player, percent: Int, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = if (isSelected) AuraPurple else Color.White,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = player.logoUrl, contentDescription = null, modifier = Modifier.size(36.dp).clip(CircleShape))
            Text("$percent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = if (isSelected) Color.White else AuraDeep)
        }
    }
}

@Composable
private fun RankingComparisonCard(
    p1: Player,
    p2: Player,
    p1Elo: PlayerEloProfile?,
    p2Elo: PlayerEloProfile?,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("RANKINGS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = AuraPurple, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, letterSpacing = 1.sp)
            Spacer(Modifier.height(24.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(model = p1.logoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape))
                
                Column(modifier = Modifier.weight(1f).padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    RankingRowComp("TOUR", p1.ranking?.toString() ?: "-", p2.ranking?.toString() ?: "-")
                    RankingRowComp("ELO", p1Elo?.eloOverall?.toString() ?: "-", p2Elo?.eloOverall?.toString() ?: "-")
                }
                
                AsyncImage(model = p2.logoUrl, contentDescription = null, modifier = Modifier.size(50.dp).clip(CircleShape))
            }
        }
    }
}

@Composable
private fun RankingRowComp(label: String, v1: String, v2: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(v1, color = AuraPurple, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.width(44.dp))
        Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Surface(color = AuraDeep, shape = RoundedCornerShape(4.dp)) {
                Text(label, color = AuraLime, fontSize = 9.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
            }
        }
        Text(v2, color = Color.Gray, fontWeight = FontWeight.Black, fontSize = 14.sp, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
    }
}

@Composable
private fun MatchInfoCard(match: TennisMatch, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, AuraPurple.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth().clickable { }, verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(20.dp).background(AuraLime, CircleShape))
                Spacer(Modifier.width(12.dp))
                Text("Tennis, ${match.tournamentCategory.displayName}, ${match.tournament}", fontSize = 12.sp, color = AuraDeep, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = AuraPurple, modifier = Modifier.size(18.dp))
            }
            InfoLine("📅", "${match.date} • ${match.time.take(5)}")
            InfoLine("🏟️", "Arthur Ashe Stadium")
            InfoLine("📍", "New York, USA")
            InfoLine("🎾", match.surface.displayName + " im Freien")
        }
    }
}

@Composable
private fun InfoLine(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(icon, fontSize = 18.sp)
        Text(text, style = MaterialTheme.typography.bodySmall, color = AuraDeep, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MatchHeroHeader(
    match: TennisMatch,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPlayerClick: (String, String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(260.dp),
        color = AuraPurple
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Organic Background
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = (-30).dp)
                    .size(300.dp, 180.dp)
                    .clip(RoundedCornerShape(bottomStart = 100.dp, bottomEnd = 100.dp))
                    .alpha(0.3f)
            ) {
                val court = TennisApplication.sessionCourt
                AsyncImage(model = court.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }

            // Watermark
            Text(
                text = "MATCH",
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset(x = 30.dp, y = (-20).dp)
                    .rotate(-10f)
                    .alpha(0.1f),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 150.sp, fontWeight = FontWeight.Black),
                color = Color.White
            )

            Column(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.background(AuraDeep.copy(alpha = 0.2f), CircleShape)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Color.White)
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = onRefresh, modifier = Modifier.background(Color.White.copy(alpha = 0.15f), CircleShape)) {
                        Icon(Icons.Default.Refresh, "Aktualisieren", tint = Color.White)
                    }
                }

                // Center Content
                Row(
                    modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PlayerHeroAvatar(
                        player = match.homePlayer,
                        isServing = match.isHomeServing == true,
                        onClick = { onPlayerClick(match.homePlayer.key, match.homePlayer.name) }
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (match.status == MatchStatus.LIVE || match.status == MatchStatus.FINISHED) {
                            val scoreParts = match.score?.split(",") ?: emptyList()
                            var p1S = 0; var p2S = 0
                            scoreParts.forEach { s ->
                                val p = s.split("-")
                                if (p.size == 2) {
                                    val s1 = p[0].trim().toIntOrNull() ?: 0
                                    val s2 = p[1].trim().toIntOrNull() ?: 0
                                    if (s1 > s2) p1S++ else if (s2 > s1) p2S++
                                }
                            }
                            Text("$p1S : $p2S", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = AuraLime)
                        } else {
                            Text("VS", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.3f))
                        }
                    }

                    PlayerHeroAvatar(
                        player = match.awayPlayer,
                        isServing = match.isHomeServing == false,
                        onClick = { onPlayerClick(match.awayPlayer.key, match.awayPlayer.name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerHeroAvatar(player: Player, isServing: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { onClick() }) {
        Box(contentAlignment = Alignment.BottomEnd) {
            // Layered overlapping effect
            Box(
                modifier = Modifier
                    .size(94.dp)
                    .offset(x = 4.dp, y = 4.dp)
                    .clip(CircleShape)
                    .background(AuraLime.copy(alpha = 0.2f))
            )
            Surface(
                modifier = Modifier.size(84.dp), 
                shape = CircleShape, 
                color = Color.White.copy(alpha = 0.1f), 
                border = BorderStroke(2.dp, Color.White.copy(alpha = 0.5f))
            ) {
                AsyncImage(model = player.logoUrl, contentDescription = player.name, modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
            }
            if (isServing) {
                Surface(color = AuraLime, shape = CircleShape, modifier = Modifier.size(24.dp).border(2.dp, AuraDeep, CircleShape)) {
                    Box(contentAlignment = Alignment.Center) { Text("🎾", fontSize = 10.sp) }
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(player.name.split(" ").last().uppercase(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = Color.White)
    }
}

@Composable
private fun ErrorView(message: String, onBack: () -> Unit, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Retry") }
            TextButton(onClick = onBack) { Text("Back") }
        }
    }
}
