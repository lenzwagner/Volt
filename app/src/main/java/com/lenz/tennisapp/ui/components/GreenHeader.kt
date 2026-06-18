package com.lenz.tennisapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.lenz.tennisapp.TennisApplication
import com.lenz.tennisapp.ui.theme.CourtType
import com.lenz.tennisapp.ui.theme.AuraPurple
import com.lenz.tennisapp.ui.theme.AuraLime
import com.lenz.tennisapp.ui.theme.AuraDeep

/**
 * Ultra-expressive "Aura" Header.
 * Inspired by the organic shapes and vibrant contrast of the Serafina design.
 */
@Composable
fun GreenHeader(
    title: String,
    subtitle: String? = null,
    court: CourtType = TennisApplication.sessionCourt,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    onCourtClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .then(if (onCourtClick != null) Modifier.clickable { onCourtClick() } else Modifier),
        color = AuraPurple
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // 1. Decorative "Aura" background elements
            // Large blurred circle in bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 40.dp, y = 40.dp)
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(AuraLime.copy(alpha = 0.3f), Color.Transparent)
                        )
                    )
            )

            // 2. The "Serafina" Cloud Image Container
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = (-30).dp)
                    .size(width = 280.dp, height = 140.dp)
                    .clip(RoundedCornerShape(
                        topStart = 70.dp, 
                        topEnd = 70.dp, 
                        bottomStart = 20.dp, 
                        bottomEnd = 20.dp
                    ))
            ) {
                AsyncImage(
                    model = court.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Frosted glass / Color overlay
                Box(modifier = Modifier.fillMaxSize().background(AuraPurple.copy(alpha = 0.2f)))
            }

            // 3. Floating Action Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (navigationIcon != null) {
                    navigationIcon()
                } else {
                    // Modern squircle chip
                    Surface(
                        color = AuraDeep,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("🎾", fontSize = 14.sp)
                        }
                    }
                }
                
                if (actions != null) {
                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions
                    )
                }
            }

            // 4. Expressionistic Typography (Bottom)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-8).dp)
            ) {
                if (subtitle != null) {
                    Text(
                        text = subtitle.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Black,
                            color = AuraDeep,
                            letterSpacing = 3.sp
                        )
                    )
                }
                
                Text(
                    text = title,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = AuraLime,
                        fontStyle = FontStyle.Normal,
                        letterSpacing = (-2).sp
                    )
                )
            }
        }
    }
}
