package com.lenz.tennisapp.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lenz.tennisapp.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TipsCarouselMaterial(
    tips: List<TipItem>,
    threshold: Float,
    count: Int,
    onThresholdChange: (Float) -> Unit,
    onCountChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onTipClick: (matchId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }

    // Entrance Animation for the whole carousel
    val entranceAlpha = remember { Animatable(0f) }
    val entranceScale = remember { Animatable(0.9f) }
    
    LaunchedEffect(Unit) {
        launch { entranceAlpha.animateTo(1f, tween(600)) }
        launch { entranceScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp)
            .scale(entranceScale.value)
            .alpha(entranceAlpha.value)
            .clip(RoundedCornerShape(32.dp))
            .background(AuraDeep)
            .padding(vertical = 16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isExpanded = !isExpanded }
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "AI TOP PICKS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = AuraLime,
                    letterSpacing = 1.sp
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = "Filter",
                    tint = AuraLime,
                    modifier = Modifier.size(16.dp)
                )
            }

            IconButton(
                onClick = onDismiss, 
                modifier = Modifier.size(32.dp).background(Color.White.copy(alpha = 0.1f), CircleShape)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        // Inline Selection Area
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(spring(Spring.DampingRatioMediumBouncy)) + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(20.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                FilterRow(
                    label = "CONFIDENCE", 
                    options = listOf(0.6f, 0.7f, 0.8f, 0.9f),
                    selectedValue = threshold
                ) { valStr ->
                    val value = valStr.replace("%", "").toFloat() / 100f
                    onThresholdChange(value)
                }
                FilterRow(
                    label = "MAX TIPS", 
                    options = listOf(3, 5, 10),
                    selectedValue = count
                ) { onCountChange(it.toInt()) }
            }
        }

        if (tips.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Analyzing court data...",
                    color = Color.White.copy(alpha = 0.3f),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            val pagerState = rememberPagerState(pageCount = { tips.size })

            Box(modifier = Modifier.fillMaxWidth()) {
                // Background Watermark Text that moves with pager
                Text(
                    text = "WINNER",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .offset(x = (pagerState.currentPageOffsetFraction * -100).dp)
                        .alpha(0.05f)
                        .rotate(-5f),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = Color.White,
                    softWrap = false
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentPadding = PaddingValues(horizontal = 56.dp),
                    pageSpacing = 12.dp
                ) { index ->
                    val tip = tips[index]
                    val pageOffset = abs((pagerState.currentPage + pagerState.currentPageOffsetFraction) - index)
                    
                    // Physical scale effect
                    val scale = 1f - (pageOffset * 0.25f).coerceIn(0f, 0.25f)
                    val isActive = pageOffset < 0.5f

                    TipAuraItem(
                        tip = tip,
                        isActive = isActive,
                        onClick = { onTipClick(tip.match.id) },
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(260.dp)
                            .scale(scale)
                    )
                }
            }
        }
    }
}

@Composable
private fun TipAuraItem(
    tip: TipItem,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.bouncyClickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = if (isActive) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isActive) AuraPurple.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
        )
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            AnimatedContent(
                targetState = isActive,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith
                    fadeOut(animationSpec = tween(400))
                },
                label = "tip_content"
            ) { active ->
                if (active) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Surface(
                            color = AuraLime,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                "${(tip.aiProb * 100).roundToInt()}% CHANCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = AuraDeep,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }

                        Text(
                            text = tip.match.tournament.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )

                        Text(
                            text = tip.winnerName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 24.sp,
                                lineHeight = 26.sp
                            ),
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )

                        val opponent = if (tip.match.homePlayer.name == tip.winnerName) {
                            tip.match.awayPlayer.name
                        } else {
                            tip.match.homePlayer.name
                        }
                        Text(
                            text = "VS ${opponent.split(" ").last().uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AuraPurple,
                            fontWeight = FontWeight.Black
                        )
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "${(tip.aiProb * 100).roundToInt()}%",
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Black,
                            color = Color.White.copy(alpha = 0.15f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(label: String, options: List<Any>, selectedValue: Any, onSelect: (String) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label, 
            style = MaterialTheme.typography.labelSmall, 
            color = Color.White.copy(alpha = 0.4f), 
            fontWeight = FontWeight.Black, 
            modifier = Modifier.width(90.dp),
            letterSpacing = 1.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { opt ->
                val display = if (opt is Float) "${(opt * 100).toInt()}%" else opt.toString()
                val isSelected = opt == selectedValue
                Surface(
                    onClick = { onSelect(display) },
                    color = if (isSelected) AuraLime else Color.White.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = display, 
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), 
                        style = MaterialTheme.typography.labelSmall, 
                        color = if (isSelected) AuraDeep else AuraLime,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}
