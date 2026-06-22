package com.lenz.tennisapp.ui.screens.match

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsTennis
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.Canvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.ui.components.H2HCard
import com.lenz.tennisapp.ui.theme.*

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

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF7F7F7))) {
        when (val s = state) {
            is MatchDetailUiState.Error -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(s.message, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = viewModel::loadDetail) { Text("Retry") }
                    TextButton(onClick = onBack) { Text("Back") }
                }
            }
            is MatchDetailUiState.Loaded -> {
                val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
                val listState = rememberLazyListState()
                
                // Show sticky header when we scrolled past the main header (item 1)
                val showStickyHeader by remember {
                    derivedStateOf { listState.firstVisibleItemIndex >= 2 }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = viewModel::refresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        MatchDetailContent(
                            detail = s.detail,
                            listState = listState,
                            onBack = onBack,
                            onRefresh = viewModel::refresh,
                            onPlayerClick = onPlayerClick,
                            onTournamentClick = onTournamentClick
                        )
                    }

                    // Sticky Banner
                    AnimatedVisibility(
                        visible = showStickyHeader,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically(),
                        modifier = Modifier.align(Alignment.TopCenter).zIndex(2f)
                    ) {
                        StickyMatchBanner(
                            match = s.detail.match,
                            onBack = onBack
                        )
                    }
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
private fun StickyMatchBanner(
    match: TennisMatch,
    onBack: () -> Unit
) {
    val court = com.lenz.tennisapp.TennisApplication.sessionCourt
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = court.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().blur(10.dp)
        )
        Box(modifier = Modifier.matchParentSize().background(Color(0xFF0A0A14).copy(alpha = 0.65f)))
        Column(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars).fillMaxWidth())
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Color.White)
                }
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        match.homePlayer.name.split(" ").last().uppercase(),
                        fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White
                    )
                    Text(
                        " vs ",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.45f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Text(
                        match.awayPlayer.name.split(" ").last().uppercase(),
                        fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
private fun MatchDetailContent(
    detail: MatchDetail,
    listState: LazyListState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPlayerClick: (String, String) -> Unit,
    onTournamentClick: (String, String) -> Unit
) {
    val match = detail.match

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp)
    ) {
        // ── Top bar ──────────────────────────────────────────────────────────
        item {
            MatchHeader(
                match = match,
                onBack = onBack,
                onRefresh = onRefresh,
                onPlayerClick = onPlayerClick
            )
        }

        // ── Section gap ──────────────────────────────────────────────────────
        item { Spacer(Modifier.height(12.dp)) }

        // ── Aktuelle Form ────────────────────────────────────────────────────
        item {
            SectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                FormSection(
                    p1 = match.homePlayer,
                    p2 = match.awayPlayer,
                    p1Matches = detail.homeRecentMatches,
                    p2Matches = detail.awayRecentMatches
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Ranglisten ───────────────────────────────────────────────────────
        item {
            SectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                RankingSection(
                    p1 = match.homePlayer,
                    p2 = match.awayPlayer,
                    p1Elo = detail.player1Elo,
                    p2Elo = detail.player2Elo
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── AI Prediction (only when external prediction exists) ─────────────
        detail.prediction?.let { pred ->
            item {
                SectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                    AIPredictionSection(
                        prediction = pred,
                        p1Name = match.homePlayer.name,
                        p2Name = match.awayPlayer.name
                    )
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }

        // ── Wettquoten ───────────────────────────────────────────────────────
        item {
            SectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                OddsSection(
                    odds = detail.odds,
                    p1 = match.homePlayer,
                    p2 = match.awayPlayer
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── H2H ─────────────────────────────────────────────────────────────
        item {
            H2HCard(h2h = detail.h2h, modifier = Modifier.padding(horizontal = 12.dp))
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Preisgeld ────────────────────────────────────────────────────────
        item {
            SectionCard(modifier = Modifier.padding(horizontal = 12.dp)) {
                PreisgeldSection(p1 = match.homePlayer, p2 = match.awayPlayer)
            }
        }
    }
}

// ─── Round translation ────────────────────────────────────────────────────────

private fun translateRound(round: String): String {
    val r = round.lowercase().trim()
    return when {
        r.contains("final") && r.contains("quarter") -> "Viertelfinale"
        r.contains("final") && r.contains("semi")    -> "Halbfinale"
        r.contains("final")                          -> "Finale"
        r.contains("round of 64")  || r == "r64"    -> "Runde der letzten 64"
        r.contains("round of 32")  || r == "r32"    -> "Runde der letzten 32"
        r.contains("round of 16")  || r == "r16"    -> "Achtelfinale"
        r.contains("round of 128") || r == "r128"   -> "Runde der letzten 128"
        r.contains("1st round") || r.contains("first round") || r == "r1" -> "1. Runde"
        r.contains("2nd round") || r.contains("second round") || r == "r2" -> "2. Runde"
        r.contains("3rd round") || r.contains("third round")  || r == "r3" -> "3. Runde"
        r.contains("4th round") || r.contains("fourth round") || r == "r4" -> "4. Runde"
        r.contains("qualifying") -> "Qualifikation"
        r.contains("group") -> "Gruppenphase"
        else -> round
    }
}

// ─── Match Header (TopBar + ScoreHeader as one seamless background) ──────────

@Composable
private fun MatchHeader(
    match: TennisMatch,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onPlayerClick: (String, String) -> Unit
) {
    val court = com.lenz.tennisapp.TennisApplication.sessionCourt
    Box(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model = court.imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().blur(10.dp)
        )
        Box(modifier = Modifier.matchParentSize().background(Color(0xFF0A0A14).copy(alpha = 0.62f)))
        Column(modifier = Modifier.fillMaxWidth()) {
            TopBarContent(
                tournament = match.tournament,
                category = match.tournamentCategory,
                round = match.round,
                onBack = onBack,
                onRefresh = onRefresh
            )
            ScoreHeaderContent(match = match, onPlayerClick = onPlayerClick)
        }
    }
}

@Composable
private fun TopBarContent(
    tournament: String,
    category: TournamentCategory,
    round: String?,
    onBack: () -> Unit,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Zurück", tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = tournament,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            round?.let {
                Text(
                    text = translateRound(it),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }
        }
        IconButton(onClick = onRefresh) {
            Icon(Icons.Default.Refresh, "Aktualisieren", tint = Color.White.copy(alpha = 0.6f))
        }
    }
}

// ─── Score Header ─────────────────────────────────────────────────────────────

@Composable
private fun ScoreHeaderContent(match: TennisMatch, onPlayerClick: (String, String) -> Unit) {
    val hasScoreData = !match.score.isNullOrBlank() && match.score != "-"
    val isInPlay = match.status == MatchStatus.LIVE ||
        (hasScoreData && match.status != MatchStatus.FINISHED)
    val scoreParts = match.score?.takeIf { it.isNotBlank() && it != "-" }?.split(",") ?: emptyList()

    data class SetInfo(val s1: Int, val s2: Int, val complete: Boolean)
    val sets = scoreParts.map { s ->
        val p = s.split("-")
        val v1 = parseSetScore(p.getOrNull(0) ?: "0")
        val v2 = parseSetScore(p.getOrNull(1) ?: "0")
        SetInfo(v1, v2, isSetComplete(v1, v2))
    }

    val p1Sets = sets.count { it.complete && it.s1 > it.s2 }
    val p2Sets = sets.count { it.complete && it.s2 > it.s1 }

    val gameParts = match.gameScore?.takeIf { it.isNotBlank() && it != "-" }?.split("-")
    val g1 = gameParts?.getOrNull(0)?.trim() ?: ""
    val g2 = gameParts?.getOrNull(1)?.trim() ?: ""

    val statusLabel = when (match.status) {
        MatchStatus.LIVE -> "LIVE"
        MatchStatus.FINISHED -> "Beendet"
        else -> match.time.take(5)
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Surface(
                color = Color.White.copy(alpha = 0.12f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    statusLabel,
                    color = if (match.status == MatchStatus.LIVE) Color.Red else Color.White,
                    fontWeight = FontWeight.Black, fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        PlayerScoreRow(
            player = match.homePlayer,
            isServing = match.isHomeServing == true && isInPlay,
            sets = sets.map { it.s1 },
            opponentSets = sets.map { it.s2 },
            setsComplete = sets.map { it.complete },
            totalSetsWon = p1Sets,
            gamePoints = g1,
            hasScore = hasScoreData,
            isFinished = match.status == MatchStatus.FINISHED,
            onClick = { onPlayerClick(match.homePlayer.key, match.homePlayer.name) }
        )

        Spacer(Modifier.height(12.dp))

        PlayerScoreRow(
            player = match.awayPlayer,
            isServing = match.isHomeServing == false && isInPlay,
            sets = sets.map { it.s2 },
            opponentSets = sets.map { it.s1 },
            setsComplete = sets.map { it.complete },
            totalSetsWon = p2Sets,
            gamePoints = g2,
            hasScore = hasScoreData,
            isFinished = match.status == MatchStatus.FINISHED,
            onClick = { onPlayerClick(match.awayPlayer.key, match.awayPlayer.name) }
        )
    }
}

@Composable
private fun PlayerScoreRow(
    player: Player,
    isServing: Boolean,
    sets: List<Int>,         // this player's score per set
    opponentSets: List<Int>, // opponent's score per set
    setsComplete: List<Boolean>,
    totalSetsWon: Int,
    gamePoints: String,
    hasScore: Boolean,
    isFinished: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp)) {
            if (!player.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = player.logoUrl, contentDescription = player.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .border(2.dp, if (isServing) AuraLime else Color.Transparent, CircleShape)
                )
            } else {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .border(2.dp, if (isServing) AuraLime else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(player.name.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // Serving indicator
        if (isServing) {
            Surface(color = AuraLime, shape = CircleShape, modifier = Modifier.size(22.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.SportsTennis, contentDescription = "Serving", tint = AuraDeep, modifier = Modifier.size(14.dp))
                }
            }
            Spacer(Modifier.width(6.dp))
        } else {
            Spacer(Modifier.width(28.dp))
        }

        // Player name + ranking
        Column(modifier = Modifier.weight(1f)) {
            Text(
                player.name.split(" ").last().uppercase(),
                fontSize = 14.sp, fontWeight = FontWeight.Black, color = Color.White, maxLines = 1
            )
            player.ranking?.let {
                Text("#$it", fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Medium)
            }
        }

        // Score area: set scores then separator then game points
        if (hasScore) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                sets.forEachIndexed { i, myScore ->
                    val oppScore = opponentSets.getOrElse(i) { 0 }
                    val complete = setsComplete.getOrElse(i) { false }
                    val iWon = complete && myScore > oppScore
                    val oppWon = complete && oppScore > myScore

                    if (complete) {
                        if (iWon) {
                            Text(myScore.toString(), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                        } else if (oppWon) {
                            Surface(
                                color = Color(0xFFCC2200),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.defaultMinSize(minWidth = 24.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)) {
                                    Text(myScore.toString(), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                                }
                            }
                        } else {
                            Text(myScore.toString(), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.4f))
                        }
                    } else {
                        Text(myScore.toString(), fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                // Separator + game points
                if (gamePoints.isNotBlank()) {
                    Text("|", fontSize = 17.sp, color = Color.White.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 4.dp))
                    Text(gamePoints, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // Sets won count (bold, right-most)
            Spacer(Modifier.width(12.dp))
            Text(
                totalSetsWon.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = if (totalSetsWon > 0) Color.White else Color.White.copy(alpha = 0.2f)
            )
        } else {
            // No score yet — VS
            Text("VS", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White.copy(alpha = 0.2f))
        }
    }
}

private fun parseSetScore(s: String): Int = when {
    "(" in s -> s.substring(0, s.indexOf("(")).trim().toIntOrNull() ?: 0
    "." in s -> s.substring(0, s.indexOf(".")).trim().toIntOrNull() ?: 0
    else -> s.trim().toIntOrNull() ?: 0
}

private fun isSetComplete(s1: Int, s2: Int): Boolean {
    if (s1 == 7 && s2 == 6) return true
    if (s2 == 7 && s1 == 6) return true
    if (s1 >= 6 && s1 - s2 >= 2) return true
    if (s2 >= 6 && s2 - s1 >= 2) return true
    return false
}


// ─── AI Prediction ───────────────────────────────────────────────────────────

@Composable
private fun AIPredictionSection(
    prediction: MatchPrediction,
    p1Name: String,
    p2Name: String
) {
    SectionTitle("AI PREDICTION")

    val p1Prob = prediction.player1WinProbability
        .coerceIn(0f, 1f)
        .let { it / (it + prediction.player2WinProbability.coerceIn(0f, 1f)).coerceAtLeast(0.001f) }
    val p1Pct = (p1Prob * 100).toInt()
    val p2Pct = 100 - p1Pct

    val animatedP1 by animateFloatAsState(targetValue = p1Prob, animationSpec = tween(900), label = "ai_prob")

    Spacer(Modifier.height(12.dp))

    // Probability bar
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp))) {
        val totalWidth = maxWidth
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.width(totalWidth * animatedP1).fillMaxHeight()
                    .background(AuraPurple),
                contentAlignment = Alignment.CenterStart
            ) {
                if (animatedP1 > 0.15f) Text(
                    "$p1Pct%",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color.White,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(AuraDeep.copy(alpha = 0.12f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (1f - animatedP1 > 0.15f) Text(
                    "$p2Pct%",
                    fontSize = 13.sp, fontWeight = FontWeight.Black, color = AuraDeep.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 10.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(p1Name.split(" ").last(), fontSize = 10.sp, color = AuraPurple, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(p2Name.split(" ").last(), fontSize = 10.sp, color = AuraDeep.copy(alpha = 0.5f), fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }

    Spacer(Modifier.height(12.dp))

    // Confidence badge + explanation
    val (confidenceLabel, confidenceColor, confidenceDesc) = when (prediction.confidence) {
        PredictionConfidence.HIGH   -> Triple("HOHE KONFIDENZ",   AuraPurple, "Modell sehr sicher in seiner Prognose")
        PredictionConfidence.MEDIUM -> Triple("MITTLERE KONFIDENZ", Color(0xFFF59E0B), "Modell mäßig sicher in seiner Prognose")
        PredictionConfidence.LOW    -> Triple("NIEDRIGE KONFIDENZ", Color(0xFFEF4444), "Modell unsicher — Tipp mit Vorsicht")
    }

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .background(confidenceColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(confidenceLabel, fontSize = 10.sp, fontWeight = FontWeight.Black, color = confidenceColor)
        }
        Text(confidenceDesc, fontSize = 10.sp, color = AuraDeep.copy(alpha = 0.5f))
    }
}

// ─── Wettquoten ───────────────────────────────────────────────────────────────

@Composable
private fun OddsSection(
    odds: List<BookmakerOdds>,
    p1: Player,
    p2: Player
) {
    val p1Name = p1.name.split(" ").last().uppercase()
    val p2Name = p2.name.split(" ").last().uppercase()

    SectionTitle("WETTQUOTEN")

    if (odds.isEmpty()) {
        Spacer(Modifier.height(12.dp))
        Text(
            "Keine Quoten verfügbar",
            fontSize = 12.sp,
            color = AuraDeep.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        return
    }

    Spacer(Modifier.height(14.dp))

    // Column headers
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            p1Name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = AuraPurple,
            modifier = Modifier.weight(1f)
        )
        Text(
            "Anbieter",
            fontSize = 10.sp,
            color = AuraDeep.copy(alpha = 0.35f),
            modifier = Modifier.width(80.dp),
            textAlign = TextAlign.Center
        )
        Text(
            p2Name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = AuraDeep.copy(alpha = 0.6f),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }

    Spacer(Modifier.height(10.dp))
    HorizontalDivider(color = Color(0xFFEEEEEE))
    Spacer(Modifier.height(10.dp))

    odds.take(5).forEachIndexed { index, o ->
        val p1Favored = o.homeOdds <= o.awayOdds
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // P1 quote
            Surface(
                color = if (p1Favored) AuraPurple.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    String.format("%.2f", o.homeOdds),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (p1Favored) AuraPurple else AuraDeep.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Bookmaker
            Text(
                o.bookmakerName.take(10),
                fontSize = 9.sp,
                color = AuraDeep.copy(alpha = 0.35f),
                modifier = Modifier.width(80.dp),
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            // P2 quote
            Surface(
                color = if (!p1Favored) AuraPurple.copy(alpha = 0.1f) else Color(0xFFF5F5F5),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    String.format("%.2f", o.awayOdds),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = if (!p1Favored) AuraPurple else AuraDeep.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
        }

        if (index < odds.size - 1 && index < 4) {
            Spacer(Modifier.height(6.dp))
        }
    }

    // No-vig probability bar (average across all bookmakers)
    val avgP1Raw = odds.map { 1.0 / it.homeOdds }.average()
    val avgP2Raw = odds.map { 1.0 / it.awayOdds }.average()
    val overround = avgP1Raw + avgP2Raw
    val p1Prob = (avgP1Raw / overround).toFloat()
    val p2Prob = (avgP2Raw / overround).toFloat()

    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = Color(0xFFEEEEEE))
    Spacer(Modifier.height(12.dp))

    Text(
        "Bereinigte Gewinnwahrscheinlichkeit",
        fontSize = 10.sp,
        color = AuraDeep.copy(alpha = 0.4f),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center
    )
    Spacer(Modifier.height(8.dp))

    // Probability bar
    val animatedP1 by animateFloatAsState(targetValue = p1Prob, animationSpec = tween(800), label = "prob")
    BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(28.dp).clip(RoundedCornerShape(8.dp))) {
        val totalWidth = maxWidth
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.width(totalWidth * animatedP1).fillMaxHeight()
                    .background(AuraPurple),
                contentAlignment = Alignment.CenterStart
            ) {
                if (animatedP1 > 0.15f) Text(
                    "${(p1Prob * 100).toInt()}%",
                    fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.White,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Box(
                modifier = Modifier.weight(1f).fillMaxHeight()
                    .background(AuraDeep.copy(alpha = 0.15f)),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (p2Prob > 0.15f) Text(
                    "${(p2Prob * 100).toInt()}%",
                    fontSize = 11.sp, fontWeight = FontWeight.Black, color = AuraDeep.copy(alpha = 0.6f),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(4.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(p1Name, fontSize = 10.sp, color = AuraPurple, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text(p2Name, fontSize = 10.sp, color = AuraDeep.copy(alpha = 0.5f), fontWeight = FontWeight.Bold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

// ─── Section Card wrapper ─────────────────────────────────────────────────────

@Composable
private fun SectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.White,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 0.dp,
        border = BorderStroke(1.dp, Color(0xFFEEEEEE))
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        fontSize = 12.sp,
        fontWeight = FontWeight.Black,
        color = AuraDeep,
        letterSpacing = 0.5.sp
    )
}

// ─── Aktuelle Form ────────────────────────────────────────────────────────────

@Composable
private fun FormSection(
    p1: Player,
    p2: Player,
    p1Matches: List<TennisMatch>,
    p2Matches: List<TennisMatch>
) {
    SectionTitle("AKTUELLE FORM")
    Spacer(Modifier.height(12.dp))

    val p1Form = p1Matches.filter { it.status == MatchStatus.FINISHED }.take(5).map { it.winnerKey == p1.key }.reversed()
    val p2Form = p2Matches.filter { it.status == MatchStatus.FINISHED }.take(5).map { it.winnerKey == p2.key }.reversed()

    if (p1Form.isEmpty() && p2Form.isEmpty()) {
        Text(
            "Keine Spiele in der Vergangenheit",
            fontSize = 12.sp,
            color = AuraDeep.copy(alpha = 0.35f),
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = TextAlign.Center
        )
    } else {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Y-axis labels
            Column(
                modifier = Modifier.width(16.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                Text("G", fontSize = 9.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(24.dp))
                Text("V", fontSize = 9.sp, color = Color(0xFFFF9800), fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(6.dp))
            FormGraph(form = p1Form, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            FormGraph(form = p2Form, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun FormGraph(form: List<Boolean>, modifier: Modifier = Modifier) {
    if (form.isEmpty()) {
        Box(modifier = modifier.height(56.dp))
        return
    }
    val winColor = Color(0xFF4CAF50)
    val lossColor = Color(0xFFFF9800)
    val lineColor = Color(0xFFCCCCCC)
    val dotRadius = 7.dp
    val graphHeight = 56.dp

    Canvas(modifier = modifier.height(graphHeight)) {
        val n = form.size
        val w = size.width
        val h = size.height
        val topY = dotRadius.toPx() + 2f
        val botY = h - dotRadius.toPx() - 2f

        fun xOf(i: Int) = if (n == 1) w / 2f else i * (w - dotRadius.toPx() * 2) / (n - 1) + dotRadius.toPx()
        fun yOf(won: Boolean) = if (won) topY else botY

        // Lines between dots
        for (i in 0 until n - 1) {
            drawLine(
                color = lineColor,
                start = Offset(xOf(i), yOf(form[i])),
                end = Offset(xOf(i + 1), yOf(form[i + 1])),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
        // Dots
        form.forEachIndexed { i, won ->
            drawCircle(
                color = if (won) winColor else lossColor,
                radius = dotRadius.toPx(),
                center = Offset(xOf(i), yOf(won))
            )
        }
    }
}

// ─── Ranglisten ───────────────────────────────────────────────────────────────

@Composable
private fun RankingSection(
    p1: Player,
    p2: Player,
    p1Elo: PlayerEloProfile?,
    p2Elo: PlayerEloProfile?
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("ATP", "ELO")

    SectionTitle("RANGLISTEN")
    Spacer(Modifier.height(12.dp))

    // Slider toggle — same design as TimeRangeFilter
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)), RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        val itemWidth = maxWidth / tabs.size
        val indicatorOffset by animateDpAsState(
            targetValue = itemWidth * selectedTab,
            animationSpec = spring(stiffness = Spring.StiffnessLow),
            label = "rankTab"
        )
        Box(
            modifier = Modifier
                .offset(x = indicatorOffset)
                .width(itemWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .background(AuraPurple, RoundedCornerShape(16.dp))
        )
        Row(modifier = Modifier.fillMaxSize()) {
            tabs.forEachIndexed { i, label ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { selectedTab = i },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedTab == i) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.offset(y = (-1).dp)
                    )
                }
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    if (selectedTab == 0) {
        RankingBarRow(
            label = "Aktueller Rang",
            v1 = p1.ranking?.let { "#$it" } ?: "—",
            v2 = p2.ranking?.let { "#$it" } ?: "—",
            // Lower rank number = better, so invert for bar
            p1Better = (p1.ranking ?: Int.MAX_VALUE) < (p2.ranking ?: Int.MAX_VALUE),
            ratio = rankingRatio(p1.ranking, p2.ranking, invert = true)
        )
        Spacer(Modifier.height(12.dp))
        RankingBarRow(
            label = "Ranking-Punkte",
            v1 = p1.atpPoints?.let { formatPoints(it) } ?: "—",
            v2 = p2.atpPoints?.let { formatPoints(it) } ?: "—",
            p1Better = (p1.atpPoints ?: 0) >= (p2.atpPoints ?: 0),
            ratio = simpleRatio(p1.atpPoints?.toDouble(), p2.atpPoints?.toDouble())
        )
        Spacer(Modifier.height(12.dp))
        RankingBarRow(
            label = "Karriere-Bestwert",
            v1 = p1.careerHighRanking?.let { "#$it" } ?: "—",
            v2 = p2.careerHighRanking?.let { "#$it" } ?: "—",
            p1Better = (p1.careerHighRanking ?: Int.MAX_VALUE) < (p2.careerHighRanking ?: Int.MAX_VALUE),
            ratio = rankingRatio(p1.careerHighRanking, p2.careerHighRanking, invert = true)
        )
        if (p1.prizeMoneyYtd != null || p2.prizeMoneyYtd != null) {
            Spacer(Modifier.height(12.dp))
            RankingBarRow(
                label = "Preisgeld 2026",
                v1 = p1.prizeMoneyYtd?.let { formatPrizeMoney(it) } ?: "—",
                v2 = p2.prizeMoneyYtd?.let { formatPrizeMoney(it) } ?: "—",
                p1Better = (p1.prizeMoneyYtd ?: 0) > (p2.prizeMoneyYtd ?: 0),
                ratio = run {
                    val a = (p1.prizeMoneyYtd ?: 0).toDouble()
                    val b = (p2.prizeMoneyYtd ?: 0).toDouble()
                    if (a + b == 0.0) 0.5f else (a / (a + b)).toFloat()
                }
            )
        }
    } else {
        EloBarRow("Gesamt", p1Elo?.eloOverall, p2Elo?.eloOverall)
        Spacer(Modifier.height(12.dp))
        EloBarRow("Sand", p1Elo?.eloClay, p2Elo?.eloClay)
        Spacer(Modifier.height(12.dp))
        EloBarRow("Rasen", p1Elo?.eloGrass, p2Elo?.eloGrass)
        Spacer(Modifier.height(12.dp))
        EloBarRow("Hartplatz", p1Elo?.eloHard, p2Elo?.eloHard)
    }
}

private fun simpleRatio(a: Double?, b: Double?): Float {
    val total = (a ?: 0.0) + (b ?: 0.0)
    return if (total == 0.0) 0.5f else ((a ?: 0.0) / total).toFloat()
}

private fun rankingRatio(r1: Int?, r2: Int?, invert: Boolean): Float {
    if (r1 == null && r2 == null) return 0.5f
    if (r1 == null) return if (invert) 0f else 1f
    if (r2 == null) return if (invert) 1f else 0f
    // For ranking: lower = better; so effective value = 1/rank
    val e1 = 1.0 / r1
    val e2 = 1.0 / r2
    return simpleRatio(e1, e2)
}

private fun formatPoints(pts: Int): String = pts.toString()

@Composable
private fun EloBarRow(label: String, v1: Int?, v2: Int?) {
    // Amplify the difference: ±400 ELO = full bar swing
    val ratio = if (v1 != null && v2 != null) {
        (0.5f + (v1 - v2) / 800f).coerceIn(0.05f, 0.95f)
    } else 0.5f
    RankingBarRow(
        label = "ELO $label",
        v1 = v1?.toString() ?: "—",
        v2 = v2?.toString() ?: "—",
        p1Better = (v1 ?: 0) >= (v2 ?: 0),
        ratio = ratio
    )
}

@Composable
private fun RankingBarRow(
    label: String,
    v1: String,
    v2: String,
    p1Better: Boolean,
    ratio: Float
) {
    val p1Color = AuraPurple
    val p2Color = Color(0xFF9E9E9E)

    val animRatio by animateFloatAsState(
        targetValue = ratio.coerceIn(0.05f, 0.95f),
        animationSpec = tween(600),
        label = "barRatio"
    )

    Column {
        // Values + label
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                v1,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (p1Better) p1Color else AuraDeep.copy(alpha = 0.5f),
                modifier = Modifier.width(52.dp)
            )
            Text(
                label,
                fontSize = 10.sp,
                color = AuraDeep.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            Text(
                v2,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                color = if (!p1Better) p1Color else AuraDeep.copy(alpha = 0.5f),
                modifier = Modifier.width(52.dp),
                textAlign = TextAlign.End
            )
        }
        Spacer(Modifier.height(5.dp))
        // Bar
        Row(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .weight(animRatio)
                    .fillMaxHeight()
                    .background(p1Color)
            )
            Box(
                modifier = Modifier
                    .weight(1f - animRatio)
                    .fillMaxHeight()
                    .background(p2Color.copy(alpha = 0.3f))
            )
        }
    }
}

@Composable
private fun ProfilLine(label: String, value: String) {
    Column {
        Text(label, fontSize = 9.sp, color = AuraDeep.copy(alpha = 0.4f))
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = AuraDeep)
    }
}

@Composable
private fun ProfilHeader(player: Player, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!player.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = player.logoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(28.dp).clip(CircleShape)
            )
        } else {
            Box(
                modifier = Modifier.size(28.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(player.name.take(1), fontSize = 12.sp, fontWeight = FontWeight.Black, color = color)
            }
        }
        Text(
            player.name.split(" ").last(),
            fontSize = 12.sp,
            fontWeight = FontWeight.Black,
            color = color,
            maxLines = 1
        )
    }
}

// ─── Preisgeld ────────────────────────────────────────────────────────────────

@Composable
private fun formatPrizeMoney(usd: Int): String {
    return when {
        usd >= 1_000_000 -> "$%.2fM".format(usd / 1_000_000.0).trimEnd('0').trimEnd('.')
        usd >= 1_000 -> "$%.0fk".format(usd / 1_000.0)
        else -> "$$usd"
    }
}

@Composable
private fun PreisgeldSection(p1: Player, p2: Player) {
    SectionTitle("PREISGELD 2026")
    Spacer(Modifier.height(16.dp))

    Row(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfilHeader(player = p1, color = AuraPurple)
            ProfilLine("Diese Saison", p1.prizeMoneyYtd?.let { formatPrizeMoney(it) } ?: "—")
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProfilHeader(player = p2, color = AuraDeep)
            ProfilLine("Diese Saison", p2.prizeMoneyYtd?.let { formatPrizeMoney(it) } ?: "—")
        }
    }
}
