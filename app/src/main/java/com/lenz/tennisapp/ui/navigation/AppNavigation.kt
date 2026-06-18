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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.*
import androidx.navigation.compose.*
import com.lenz.tennisapp.TennisApplication
import com.lenz.tennisapp.ui.components.GreenHeader
import com.lenz.tennisapp.ui.screens.home.HomeScreen
import com.lenz.tennisapp.ui.screens.match.MatchDetailScreen
import com.lenz.tennisapp.ui.screens.player.PlayerDetailScreen
import com.lenz.tennisapp.ui.screens.tournament.TournamentDetailScreen
import com.lenz.tennisapp.ui.screens.predictions.PredictionsScreen
import com.lenz.tennisapp.ui.screens.settings.SettingsScreen
import com.lenz.tennisapp.ui.theme.*

sealed class Screen(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
    val tabIndex: Int
) {
    object Home        : Screen("home",        "Heute",     Icons.Outlined.SportsTennis, Icons.Filled.SportsTennis, 0)
    object Predictions : Screen("predictions", "Prognosen", Icons.Outlined.Lightbulb,    Icons.Filled.Lightbulb, 1)
    object Settings    : Screen("settings",    "Mehr",      Icons.Outlined.Settings,     Icons.Filled.Settings, 2)
}

val bottomNavItems = listOf(Screen.Home, Screen.Predictions, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(initialRoute: String? = null, initialMatchId: String? = null) {
    AppMainContent(initialRoute, initialMatchId)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppMainContent(initialRoute: String? = null, initialMatchId: String? = null) {
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

    val pagerState = rememberPagerState(initialPage = 0) { 3 }

    // Snappier sync from NavController to Pager
    LaunchedEffect(currentTabIndex) {
        if (currentTabIndex != null && currentTabIndex != pagerState.currentPage) {
            pagerState.animateScrollToPage(
                page = currentTabIndex,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
            )
        }
    }

    // NEW: Smooth sync from Pager swipe to NavController
    // This only triggers when the page has fully settled, preventing "stuck" states
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

    val showBottomBar = currentTabIndex != null

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = Color.White,
        bottomBar = {
            if (showBottomBar) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    // Soft transparent gradient overlay behind the navigation bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0f),
                                        Color.White.copy(alpha = 0.8f),
                                        Color.White
                                    )
                                )
                            )
                    )

                    Box(
                        modifier = Modifier
                            .padding(bottom = 24.dp)
                    ) {
                        FloatingAuraNavigationBar(
                            selectedTabIndex = pagerState.currentPage,
                            onTabSelected = { index ->
                                if (pagerState.currentPage != index) {
                                    val targetRoute = bottomNavItems[index].route
                                    navController.navigate(targetRoute) {
                                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                    // Pager sync will happen via the LaunchedEffect(currentTabIndex)
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
                .padding(bottom = if (showBottomBar) 0.dp else innerPadding.calculateBottomPadding())
        ) {
            if (showBottomBar) {
                GreenHeader(
                    title    = when (pagerState.currentPage) {
                        0    -> "Tennis Today"
                        1    -> "AI Predictor"
                        2    -> "Settings"
                        else -> "Tennis"
                    },
                    subtitle = when (pagerState.currentPage) {
                        0    -> "Matches & News"
                        1    -> "Hit rate statistics"
                        2    -> "System config"
                        else -> null
                    },
                    court = TennisApplication.sessionCourt,
                    onCourtClick = { TennisApplication.rotateCourt() }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                userScrollEnabled = showBottomBar,
                beyondViewportPageCount = 1,
                contentPadding = PaddingValues(bottom = if (showBottomBar) 110.dp else 0.dp)
            ) { page ->
                when (page) {
                    0 -> HomeScreen(
                        onMatchClick = { navController.navigate("match/$it") },
                        onTournamentClick = { leagueId, name ->
                            navController.navigate("tournament/$leagueId/$name")
                        },
                        showHeader = false
                    )
                    1 -> PredictionsScreen(showHeader = false)
                    2 -> SettingsScreen(showHeader = false)
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier,
            enterTransition = { fadeIn(tween(200)) },
            exitTransition = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(200)) },
            popExitTransition = { fadeOut(tween(180)) }
        ) {
            composable(Screen.Home.route) {}
            composable(Screen.Predictions.route) {}
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
    }
}

@Composable
fun FloatingAuraNavigationBar(
    selectedTabIndex: Int,
    onTabSelected: (Int) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(320.dp)
            .height(76.dp),
        color = AuraDeep.copy(alpha = 0.96f),
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
                    targetValue = if (isSelected) 1.2f else 1.0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium // Snappier stiffness
                    ),
                    label = "scale"
                )

                val tint by animateColorAsState(
                    targetValue = if (isSelected) AuraLime else Color.White.copy(alpha = 0.3f),
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
                                .background(AuraPurple.copy(alpha = 0.15f))
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isSelected) screen.iconSelected else screen.icon,
                            contentDescription = screen.label,
                            tint = tint,
                            modifier = Modifier
                                .size(26.dp)
                                .scale(scale)
                        )
                        
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
