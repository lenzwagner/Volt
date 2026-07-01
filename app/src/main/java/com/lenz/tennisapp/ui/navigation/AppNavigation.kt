package com.lenz.tennisapp.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.*
import androidx.navigation.compose.*
import com.lenz.tennisapp.TennisApplication
import com.lenz.tennisapp.ui.components.GreenHeader
import com.lenz.tennisapp.ui.screens.home.HomeScreen
import com.lenz.tennisapp.ui.screens.home.HomeViewModel
import com.lenz.tennisapp.ui.screens.match.MatchDetailScreen
import com.lenz.tennisapp.ui.screens.player.PlayerDetailScreen
import com.lenz.tennisapp.ui.screens.tournament.TournamentDetailScreen
import com.lenz.tennisapp.ui.screens.airecommendations.AiRecommendationsScreen
import com.lenz.tennisapp.ui.screens.predictions.PredictionsScreen
import com.lenz.tennisapp.ui.screens.settings.SettingsScreen
import com.lenz.tennisapp.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val tabIndex: Int
) {
    object Home            : Screen("home",            "Heute",       Icons.Outlined.SportsTennis,  Icons.Filled.SportsTennis, 0)
    object Predictions     : Screen("predictions",     "Vorhersagen", Icons.Outlined.Lightbulb,     Icons.Filled.Lightbulb,    1)
    object AiRecommend     : Screen("ai_recommend",    "KI-Tipps",    Icons.Outlined.AutoAwesome,   Icons.Filled.AutoAwesome,  2)
    object Settings        : Screen("settings",        "Mehr",        Icons.Outlined.Settings,      Icons.Filled.Settings,     3)
}

val bottomNavItems = listOf(Screen.Home, Screen.Predictions, Screen.AiRecommend, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(initialRoute: String? = null, initialMatchId: String? = null) {
    AppMainContent(initialRoute, initialMatchId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppMainContent(initialRoute: String? = null, initialMatchId: String? = null) {
    val homeViewModel: HomeViewModel = hiltViewModel()
    val settingsViewModel: com.lenz.tennisapp.ui.screens.settings.SettingsViewModel = hiltViewModel()
    
    val isDataReady by homeViewModel.isDataReady.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()

    val liveCount by homeViewModel.liveCount.collectAsStateWithLifecycle()

    val backgroundBrush = remember(settingsState.bgGradientHeight, settingsState.bgGradientColor) {
        val accentColor = Color(settingsState.bgGradientColor)
        val colorStart = (1f - (settingsState.bgGradientHeight * 0.5f)).coerceIn(0f, 0.95f)
        
        Brush.verticalGradient(
            0.0f to Color.White,
            colorStart to Color.White,
            1.0f to accentColor
        )
    }

    var splashVisible by remember { mutableStateOf(true) }

    // Logic to hide splash when data is ready
    LaunchedEffect(isDataReady) {
        if (isDataReady) {
            delay(400) // Minimum display time
            splashVisible = false
        }
    }

    // Status bar style adjustment
    val view = androidx.compose.ui.platform.LocalView.current
    val window = (view.context as? android.app.Activity)?.window
    if (window != null && !view.isInEditMode) {
        SideEffect {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !splashVisible
            
            // Initial/Default navigation bar setup
            if (splashVisible) {
                window.navigationBarColor = settingsState.tabBarColor.toInt()
                controller.isAppearanceLightNavigationBars = false
            } else {
                window.navigationBarColor = android.graphics.Color.WHITE
                controller.isAppearanceLightNavigationBars = true
            }
        }
    }

    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStack?.destination?.route

    // Handle initial navigation from notification
    LaunchedEffect(initialRoute, initialMatchId) {
        if (initialRoute == "match_detail" && initialMatchId != null) {
            navController.navigate("match/$initialMatchId") {
                launchSingleTop = true
            }
        } else if (initialRoute == "settings") {
            navController.navigate(Screen.Settings.route) {
                launchSingleTop = true
            }
        }
    }

    val currentTabIndex = remember(currentRoute) {
        bottomNavItems.find { it.route == currentRoute }?.tabIndex
    }

    val pagerState = rememberPagerState(initialPage = 0) { bottomNavItems.size }

    // Snappier sync from NavController to Pager
    LaunchedEffect(currentTabIndex) {
        if (currentTabIndex != null && currentTabIndex != pagerState.currentPage) {
            pagerState.scrollToPage(currentTabIndex)
        }
    }

    // NEW: Smooth sync from Pager swipe to NavController
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { settledPage ->
            val targetRoute = bottomNavItems[settledPage].route
            if (currentRoute != targetRoute && currentTabIndex != null) {
                navController.navigate(targetRoute) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
    }

    val showBottomBar = currentTabIndex != null && !splashVisible

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .background(
                            brush = Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(bottom = 24.dp)
                    ) {
                        FloatingAuraNavigationBar(
                            selectedTabIndex = pagerState.currentPage,
                            liveCount = if (pagerState.currentPage != 0) liveCount else 0,
                            tabBarColor = Color(settingsState.tabBarColor),
                            onTabSelected = { index ->
                                if (pagerState.currentPage != index) {
                                    val targetRoute = bottomNavItems[index].route
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
                .padding(bottom = if (showBottomBar) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            if (showBottomBar) {
                GreenHeader(
                    title    = when (pagerState.currentPage) {
                        0 -> "Tennis Today"
                        1 -> "Vorhersagen"
                        2 -> "KI-Empfehlungen"
                        3 -> "Settings"
                        else -> "Tennis"
                    },
                    subtitle = when (pagerState.currentPage) {
                        0 -> "Matches"
                        1 -> "Deine Trefferquote"
                        2 -> "Tägliche KI-Prognosen"
                        3 -> "System config"
                        else -> null
                    },
                    court = TennisApplication.sessionCourt,
                    onCourtClick = { TennisApplication.rotateCourt() }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = true,
                beyondViewportPageCount = 1,
                contentPadding = PaddingValues(bottom = 0.dp)
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onMatchClick = { navController.navigate("match/$it") },
                        onTournamentClick = { leagueId, name -> 
                            navController.navigate("tournament/$leagueId/$name")
                        },
                        showHeader = false
                    )
                    1 -> PredictionsScreen(
                        showHeader = false,
                        onMatchClick = { navController.navigate("match/$it") }
                    )
                    2 -> AiRecommendationsScreen(
                        onMatchClick = { navController.navigate("match/$it") }
                    )
                    3 -> SettingsScreen(showHeader = false)
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(150)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(150)) }
        ) {
            composable(Screen.Home.route) {}
            composable(Screen.Predictions.route) {}
            composable(Screen.AiRecommend.route) {}
            composable(Screen.Settings.route) {}

            composable(
                route = "match/{matchId}",
                arguments = listOf(navArgument("matchId") { type = NavType.StringType })
            ) { backStack ->
                val matchId = backStack.arguments?.getString("matchId") ?: return@composable
                MatchDetailScreen(
                    matchId = matchId,
                    onBack = { navController.popBackStack() },
                    onPlayerClick = { playerKey, playerName ->
                        navController.navigate("player/$playerKey/$playerName")
                    },
                    onTournamentClick = { leagueId, name ->
                        navController.navigate("tournament/$leagueId/$name")
                    }
                )
            }

            composable(
                route = "tournament/{leagueId}/{name}",
                arguments = listOf(
                    navArgument("leagueId") { type = NavType.StringType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStack ->
                val leagueId = backStack.arguments?.getString("leagueId") ?: return@composable
                val name = backStack.arguments?.getString("name") ?: return@composable
                TournamentDetailScreen(
                    leagueId = leagueId,
                    tournamentName = name,
                    onBack = { navController.popBackStack() },
                    onMatchClick = { mid ->
                        navController.navigate("match/$mid")
                    }
                )
            }

            composable(
                route = "player/{playerKey}/{playerName}",
                arguments = listOf(
                    navArgument("playerKey") { type = NavType.StringType },
                    navArgument("playerName") { type = NavType.StringType }
                )
            ) { backStack ->
                val playerKey = backStack.arguments?.getString("playerKey") ?: return@composable
                val playerName = backStack.arguments?.getString("playerName") ?: return@composable
                PlayerDetailScreen(
                    playerKey = playerKey,
                    playerName = playerName,
                    onBack = { navController.popBackStack() },
                    onPlayerClick = { newPlayerKey, newPlayerName ->
                        navController.navigate("player/$newPlayerKey/${newPlayerName.trim()}")
                    }
                )
            }
        }

        AnimatedVisibility(
            visible = splashVisible,
            enter = EnterTransition.None,
            exit = fadeOut(tween(400))
        ) {
            SplashOverlay()
        }
    }
}

@Composable
private fun SplashOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AuraDeep),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            val transition = rememberInfiniteTransition("splashPulse")
            val scale by transition.animateFloat(
                initialValue = 0.9f,
                targetValue = 1.1f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.9f at 0 with CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                        1.1f at 600 with CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                        0.9f at 1200
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "splashScale"
            )

            val glowAlpha by transition.animateFloat(
                initialValue = 0.12f,
                targetValue = 0.38f,
                animationSpec = infiniteRepeatable(
                    animation = keyframes {
                        durationMillis = 1200
                        0.12f at 0 with CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                        0.38f at 600 with CubicBezierEasing(0.4f, 0f, 0.6f, 1f)
                        0.12f at 1200
                    },
                    repeatMode = RepeatMode.Restart
                ),
                label = "splashGlow"
            )

            Box(contentAlignment = Alignment.Center) {
                // Background glow
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .graphicsLayer { alpha = glowAlpha }
                        .clip(CircleShape)
                        .background(AuraLime.copy(alpha = 0.4f))
                )

                // Main icon box
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                        }
                        .clip(CircleShape)
                        .background(AuraLime),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SportsTennis,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = AuraDeep
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Tennis Today",
                    color = AuraLime,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "WHERE AI MEETS THE BASELINE",
                    color = Color.White.copy(alpha = 0.4f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Placeholder for LivePulsingDot if needed
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(AuraLime)
                )
                Text(
                    "Lade Spieltag …",
                    color = Color.White.copy(alpha = 0.55f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun FloatingAuraNavigationBar(
    selectedTabIndex: Int,
    liveCount: Int = 0,
    tabBarColor: Color = AuraPurple,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(380.dp)
            .height(76.dp),
        color = tabBarColor,
        shape = CircleShape,
        shadowElevation = 16.dp,
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            bottomNavItems.forEach { screen ->
                val isSelected = selectedTabIndex == screen.tabIndex
                
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.25f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessHigh
                    ),
                    label = "scale"
                )

                val tint by animateColorAsState(
                    targetValue = if (isSelected) AuraLime else Color.White.copy(alpha = 0.5f),
                    animationSpec = tween(250), // Faster transition
                    label = "tint"
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabSelected(screen.tabIndex)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Modern background glow for active tab
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.18f))
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BadgedBox(
                            badge = {
                                if (screen == Screen.Home && liveCount > 0) {
                                    Badge(
                                        containerColor = Color.Red,
                                        contentColor = Color.White
                                    ) {
                                        Text(liveCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isSelected) screen.iconSelected else screen.icon,
                                contentDescription = screen.label,
                                tint = tint,
                                modifier = Modifier
                                    .size(26.dp)
                                    .scale(scale)
                            )
                        }
                        
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(tween(200)) + expandVertically(tween(250)),
                            exit = fadeOut(tween(150)) + shrinkVertically(tween(200))
                        ) {
                            Text(
                                text = screen.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                ),
                                color = AuraLime,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
