package com.lenz.tennisapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.lenz.tennisapp.domain.model.*
import com.lenz.tennisapp.ui.theme.*

/**
 * Custom modifier for a "physical" bouncy click effect.
 */
@Composable
fun Modifier.bouncyClickable(
    enabled: Boolean = true,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bouncy_scale"
    )

    return this
        .scale(scale)
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Disable default ripple for physical feel
            enabled = enabled,
            onClick = onClick
        )
}

@Composable
fun PlayerAvatarWithRanking(
    player: Player,
    size: androidx.compose.ui.unit.Dp = 24.dp,
    rankingFontSize: androidx.compose.ui.unit.TextUnit = 8.sp,
    badgeSize: androidx.compose.ui.unit.Dp = 12.dp
) {
    Box(contentAlignment = Alignment.BottomEnd) {
        if (!player.logoUrl.isNullOrBlank()) {
            AsyncImage(
                model = player.logoUrl,
                contentDescription = player.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.1f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(AuraPurple.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    player.name.take(1),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = (size.value * 0.5f).sp),
                    color = AuraDeep,
                    fontWeight = FontWeight.Black
                )
            }
        }

        player.ranking?.let { rank ->
            Surface(
                modifier = Modifier
                    .size(badgeSize)
                    .offset(x = (badgeSize.value * 0.2f).dp, y = (badgeSize.value * 0.2f).dp),
                shape = CircleShape,
                color = AuraDeep,
                border = androidx.compose.foundation.BorderStroke(1.dp, AuraLime)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        rank.toString(),
                        style = androidx.compose.ui.text.TextStyle(
                            color = AuraLime,
                            fontSize = rankingFontSize,
                            fontWeight = FontWeight.Black,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun MatchCard(
    match: TennisMatch,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    userPrediction: UserPrediction? = null,
    onPredict: ((winnerKey: String, winnerName: String) -> Unit)? = null
) {
    val canPredict = onPredict != null && match.status == MatchStatus.NOT_STARTED

    val isLive = match.status == MatchStatus.LIVE
    
    ElevatedCard(
        modifier = modifier
            .padding(vertical = 4.dp)
            .then(
                if (isLive) Modifier.shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(24.dp),
                    spotColor = AuraLime,
                    ambientColor = AuraLime
                ) else Modifier
            )
            .border(1.dp, Color.White, RoundedCornerShape(24.dp))
            .bouncyClickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = Color.White.copy(alpha = 0.7f)
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Time / Status
                Column(
                    modifier = Modifier.width(60.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    when (match.status) {
                        MatchStatus.LIVE     -> LiveIndicator()
                        MatchStatus.FINISHED -> StatusBadge("FT", AuraDeep, AuraLime.copy(alpha = 0.2f))
                        MatchStatus.CANCELLED -> StatusBadge("CANC", Color.Red, Color.Red.copy(alpha = 0.1f))
                        else -> Text(
                            match.time.take(5),
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp),
                            color = AuraDeep,
                            fontWeight = FontWeight.Black
                        )
                    }
                    RoundLabel(match.round)
                }

                // Players
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val homeWon = match.status == MatchStatus.FINISHED &&
                            match.score?.let { score ->
                                val parts = score.split(",")
                                var hS = 0; var aS = 0
                                parts.forEach { s ->
                                    val p = s.split("-")
                                    if (p.size == 2) {
                                        val s1 = p[0].trim().toIntOrNull() ?: 0
                                        val s2 = p[1].trim().toIntOrNull() ?: 0
                                        if (s1 > s2) hS++ else if (s2 > s1) aS++
                                    }
                                }
                                hS > aS
                            } == true
                    val awayWon = match.status == MatchStatus.FINISHED && !homeWon &&
                            match.score?.let { it.contains("-") } == true

                    PlayerMatchRow(
                        player = match.homePlayer,
                        isWinner = homeWon,
                        isServing = match.isHomeServing == true && match.status == MatchStatus.LIVE
                    )
                    PlayerMatchRow(
                        player = match.awayPlayer,
                        isWinner = awayWon,
                        isServing = match.isHomeServing == false && match.status == MatchStatus.LIVE
                    )
                }

                // Score
                if (match.score != null && match.score != "-") {
                    SetScoreGrid(
                        score     = match.score,
                        gameScore = if (match.status == MatchStatus.LIVE) match.gameScore else null,
                        isLive    = match.status == MatchStatus.LIVE,
                        fontSize  = 15.sp,
                        gameSize  = 12.sp,
                        modifier  = Modifier.padding(start = 12.dp)
                    )
                }

                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = AuraPurple.copy(alpha = 0.4f),
                    modifier = Modifier.size(24.dp).padding(start = 4.dp)
                )
            }

            if (canPredict || userPrediction != null) {
                PredictionStrip(
                    match = match,
                    prediction = userPrediction,
                    onPredict = onPredict
                )
            }
        }
    }
}

@Composable
private fun PredictionStrip(
    match: TennisMatch,
    prediction: UserPrediction?,
    onPredict: ((String, String) -> Unit)?
) {
    val homeKey  = match.homePlayer.key
    val homeName = match.homePlayer.name.split(" ").last().uppercase()
    val awayKey  = match.awayPlayer.key
    val awayName = match.awayPlayer.name.split(" ").last().uppercase()

    val noPick = prediction == null

    Surface(
        color = if (noPick) AuraPurple.copy(alpha = 0.08f) else Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (noPick) {
                Text(
                    "PICK",
                    style = MaterialTheme.typography.labelSmall,
                    color = AuraPurple,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    modifier = Modifier.width(36.dp)
                )
            }
            PickButton(
                name      = homeName,
                isPicked  = prediction?.predictedWinnerKey == homeKey,
                isCorrect = if (prediction?.predictedWinnerKey == homeKey) prediction?.isCorrect else null,
                isWinner  = prediction?.actualWinnerKey == homeKey,
                canPick   = noPick && onPredict != null,
                onClick   = { onPredict?.invoke(homeKey, match.homePlayer.name) },
                modifier  = Modifier.weight(1f)
            )
            PickButton(
                name      = awayName,
                isPicked  = prediction?.predictedWinnerKey == awayKey,
                isCorrect = if (prediction?.predictedWinnerKey == awayKey) prediction?.isCorrect else null,
                isWinner  = prediction?.actualWinnerKey == awayKey,
                canPick   = noPick && onPredict != null,
                onClick   = { onPredict?.invoke(awayKey, match.awayPlayer.name) },
                modifier  = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PickButton(
    name: String,
    isPicked: Boolean,
    isCorrect: Boolean?,
    isWinner: Boolean,
    canPick: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val containerColor = when {
        isPicked && isCorrect == true  -> AuraLime
        isPicked && isCorrect == false -> Color(0xFFFFEBEE)
        isPicked                       -> AuraPurple
        else                           -> Color.White
    }
    val contentColor = when {
        isPicked && isCorrect == true  -> AuraDeep
        isPicked && isCorrect == false -> Color.Red
        isPicked                       -> Color.White
        else                           -> AuraDeep.copy(alpha = 0.6f)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        border = if (!isPicked) BorderStroke(1.dp, AuraDeep.copy(alpha = 0.05f)) else null,
        modifier = modifier.bouncyClickable(enabled = canPick, onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PlayerMatchRow(player: Player, isWinner: Boolean, isServing: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        PlayerAvatarWithRanking(
            player = player,
            size = 36.dp,
            rankingFontSize = 7.sp,
            badgeSize = 14.dp
        )

        Text(
            text = player.name.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                letterSpacing = 0.5.sp
            ),
            fontWeight = if (isWinner) FontWeight.Black else FontWeight.Bold,
            color = if (isWinner) AuraDeep else AuraDeep.copy(alpha = 0.5f),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (isServing) {
            Surface(
                color = AuraLime,
                shape = CircleShape,
                modifier = Modifier.size(10.dp).border(1.dp, AuraDeep.copy(alpha = 0.2f), CircleShape)
            ) {}
        }
    }
}

@Composable
private fun RoundLabel(round: String?) {
    if (round == null) return
    Text(
        text = round.uppercase(),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = AuraPurple.copy(alpha = 0.6f),
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp
    )
}

@Composable
private fun StatusBadge(text: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = textColor,
            fontWeight = FontWeight.Black,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

@Composable
private fun LiveIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "live_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live_scale"
    )
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = Color.Red,
        modifier = Modifier.scale(scale)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Surface(
                color = Color.White,
                shape = CircleShape,
                modifier = Modifier.size(6.dp)
            ) {}
            Spacer(Modifier.width(4.dp))
            Text(
                "LIVE",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = Color.White,
                fontWeight = FontWeight.Black,
            )
        }
    }
}
