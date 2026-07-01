package com.lenz.tennisapp.ui.screens.settings

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import com.lenz.tennisapp.ui.components.GreenHeader
import com.lenz.tennisapp.ui.theme.AuraDeep
import com.lenz.tennisapp.ui.theme.AuraPurple
import com.lenz.tennisapp.ui.theme.AuraLime
import com.lenz.tennisapp.worker.RankingsAndEloSyncWorker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    showHeader: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        if (showHeader) {
            GreenHeader(title = "Einstellungen", subtitle = "API-Keys & Konfiguration")
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { isRefreshing = false },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ── Group 1: APIs ──────────────────────────────────────────
                item {
                    CollapsibleSettingsCard(
                        title = "API Konfiguration",
                        subtitle = "api-tennis.com & the-odds-api.com",
                        icon = Icons.Default.VpnKey,
                        initialExpanded = false
                    ) {
                        ApiSettingsContent(state = state, viewModel = viewModel)
                    }
                }

                // ── Group 2: Data & Sync ─────────────────────────────────────
                item {
                    CollapsibleSettingsCard(
                        title = "Daten & Synchronisierung",
                        subtitle = "Rankings, Elo-Scores & Quoten",
                        icon = Icons.Default.Sync,
                        initialExpanded = false
                    ) {
                        DataSyncContent()
                    }
                }

                // ── Group 3: Appearance ──────────────────────────────────────
                item {
                    CollapsibleSettingsCard(
                        title = "Darstellung",
                        subtitle = "Farben, Verläufe & Tab-Bar",
                        icon = Icons.Default.Palette,
                        initialExpanded = false
                    ) {
                        AppearanceSettingsContent(state = state, viewModel = viewModel)
                    }
                }

                // ── Group 4: Roadmap & Info ──────────────────────────────────
                item {
                    CollapsibleSettingsCard(
                        title = "Über & Roadmap",
                        subtitle = "App Version & Kommende Features",
                        icon = Icons.Default.Info,
                        initialExpanded = false
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            UpcomingFeaturesContent()
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            InfoContent()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollapsibleSettingsCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    initialExpanded: Boolean = false,
    content: @Composable () -> Unit
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(AuraPurple.copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(icon, null, tint = AuraPurple, modifier = Modifier.size(22.dp))
                    }
                    Column {
                        Text(
                            title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = AuraDeep
                        )
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = AuraDeep.copy(alpha = 0.5f)
                        )
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = AuraDeep.copy(alpha = 0.3f)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    content()
                }
            }
        }
    }
}

@Composable
private fun ApiSettingsContent(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Tennis API Section
        ApiKeySection(
            title = "Tennis API",
            subtitle = "api-tennis.com",
            currentKey = state.tennisKey,
            isExpired = state.tennisKeyExpired,
            statusText = if (state.tennisKeyExpired) "Fehlerhaft/Abgelaufen" else "Aktiv",
            isTesting = state.isTestingTennis,
            testResult = state.tennisTestResult,
            signupUrl = "https://api-tennis.com",
            onSave = viewModel::saveTennisKey,
            onTest = { viewModel.testTennisKey(it) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // The Odds API Section
        ApiKeySection(
            title = "The Odds API",
            subtitle = "api.the-odds-api.com · ${state.oddsQuotaRemaining?.let { "$it Requests übrig" } ?: "Quota unbekannt"}",
            currentKey = state.oddsApiKey,
            isExpired = false,
            statusText = "Aktiv",
            isTesting = false,
            testResult = null,
            signupUrl = "https://the-odds-api.com",
            onSave = viewModel::saveOddsApiKey,
            onTest = {}
        )
    }
}

@Composable
private fun ApiKeySection(
    title: String,
    subtitle: String,
    currentKey: String,
    isExpired: Boolean,
    statusText: String,
    isTesting: Boolean,
    testResult: String?,
    signupUrl: String,
    onSave: (String) -> Unit,
    onTest: (String) -> Unit
) {
    var editedKey by remember(currentKey) { mutableStateOf(currentKey) }
    var showKey by remember { mutableStateOf(false) }
    var showSavedMessage by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatusChip(statusText, !isExpired && currentKey.isNotEmpty(), isExpired)
        }

        OutlinedTextField(
            value = editedKey,
            onValueChange = { editedKey = it },
            label = { Text("API Schlüssel") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showKey = !showKey }) {
                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        if (showSavedMessage) {
            Text(
                "✅ Gespeichert! Bitte starte die App neu, um die Änderungen zu übernehmen.",
                style = MaterialTheme.typography.labelSmall,
                color = AuraPurple,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        testResult?.let {
            val isError = it.startsWith("Fehler")
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) MaterialTheme.colorScheme.error else AuraPurple,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { 
                    onSave(editedKey)
                    showSavedMessage = true
                    android.widget.Toast.makeText(context, "Key gespeichert", android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Speichern", fontSize = 13.sp)
            }
            
            Button(
                onClick = { 
                    showSavedMessage = false
                    onTest(editedKey) 
                },
                modifier = Modifier.weight(1f),
                enabled = !isTesting,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                if (isTesting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Testen", fontSize = 13.sp)
                }
            }
            
            IconButton(
                onClick = { uriHandler.openUri(signupUrl) },
                modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, null, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun StatusChip(text: String, isActive: Boolean, isExpired: Boolean) {
    val color = when { 
        isExpired -> Color.Red
        isActive -> AuraPurple
        else -> Color.Gray 
    }
    Surface(
        shape = RoundedCornerShape(8.dp), 
        color = color.copy(alpha = 0.1f), 
        border = BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = color, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
    }
}

@Composable
private fun DataSyncContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        RankingsSyncItem()
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        OddsSyncItem()
    }
}

@Composable
private fun AppearanceSettingsContent(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    val presetColors = listOf(
        0xFFBBDEFB to "Blau",
        0xFFC8E6C9 to "Grün",
        0xFFF8BBD0 to "Rosa",
        0xFFD1C4E9 to "Lila",
        0xFFFFF9C4 to "Gelb"
    )

    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        // Tab Bar Gradient Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tab Bar Gradient", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Deaktiveren für komplette Transparenz", style = MaterialTheme.typography.bodySmall, color = AuraDeep.copy(alpha = 0.5f))
            }
            Switch(
                checked = state.showTabGradient,
                onCheckedChange = { viewModel.setShowTabGradient(it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AuraPurple)
            )
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        // Dynamic Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dynamischer Farbverlauf", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("Wechselnde Aura-Farben", style = MaterialTheme.typography.bodySmall, color = AuraDeep.copy(alpha = 0.5f))
            }
            Switch(
                checked = state.bgGradientDynamic,
                onCheckedChange = { viewModel.updateBgGradientSettings(state.bgGradientHeight, state.bgGradientColor, it) },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = AuraPurple)
            )
        }

        // Height Slider
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Gradient Ausdehnung", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${(state.bgGradientHeight * 100).toInt()}%", style = MaterialTheme.typography.labelSmall, color = AuraPurple, fontWeight = FontWeight.Bold)
            }
            Slider(
                value = state.bgGradientHeight,
                onValueChange = { viewModel.updateBgGradientSettings(it, state.bgGradientColor, state.bgGradientDynamic) },
                valueRange = 0.1f..2.0f,
                colors = SliderDefaults.colors(thumbColor = AuraPurple, activeTrackColor = AuraPurple)
            )
        }

        // Color Presets
        if (!state.bgGradientDynamic) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Primärfarbe", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    presetColors.forEach { (hex, _) ->
                        val isSelected = state.bgGradientColor == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) AuraPurple else AuraDeep.copy(alpha = 0.1f),
                                    shape = CircleShape
                                )
                                .clickable { viewModel.updateBgGradientSettings(state.bgGradientHeight, hex, state.bgGradientDynamic) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RankingsSyncItem() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkLiveData("${RankingsAndEloSyncWorker.WORK_NAME}_manual")
        .observeAsState()

    val syncState = workInfos?.firstOrNull()?.state
    val isSyncing = syncState == androidx.work.WorkInfo.State.ENQUEUED ||
            syncState == androidx.work.WorkInfo.State.RUNNING

    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(syncState) {
        if (syncState == androidx.work.WorkInfo.State.SUCCEEDED) {
            showSuccess = true
            kotlinx.coroutines.delay(3000L)
            showSuccess = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Rankings & Elo-Scores", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("Täglicher Sync der ATP/WTA Rankings & Elo Scores (um 3:00 Uhr).", 
            style = MaterialTheme.typography.bodySmall, color = AuraDeep.copy(alpha = 0.5f))
        
        Button(
            onClick = {
                workManager.enqueueUniqueWork(
                    "${RankingsAndEloSyncWorker.WORK_NAME}_manual", 
                    androidx.work.ExistingWorkPolicy.REPLACE, 
                    androidx.work.OneTimeWorkRequestBuilder<RankingsAndEloSyncWorker>().build()
                )
            }, 
            enabled = !isSyncing, 
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showSuccess) Color(0xFF4CAF50) else AuraDeep
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Synchronisiere...")
            } else if (showSuccess) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Erfolgreich!")
            } else {
                Text("Jetzt synchronisieren")
            }
        }
    }
}

@Composable
private fun OddsSyncItem() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkLiveData("${com.lenz.tennisapp.worker.OddsSyncWorker.WORK_NAME}_manual")
        .observeAsState()

    val syncState = workInfos?.firstOrNull()?.state
    val isSyncing = syncState == androidx.work.WorkInfo.State.ENQUEUED ||
            syncState == androidx.work.WorkInfo.State.RUNNING

    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(syncState) {
        if (syncState == androidx.work.WorkInfo.State.SUCCEEDED) {
            showSuccess = true
            kotlinx.coroutines.delay(3000L)
            showSuccess = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Wettquoten", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        Text("Aktualisiert Quoten für alle offenen Spiele des heutigen Tages.",
            style = MaterialTheme.typography.bodySmall, color = AuraDeep.copy(alpha = 0.5f))

        Button(
            onClick = {
                workManager.enqueueUniqueWork(
                    "${com.lenz.tennisapp.worker.OddsSyncWorker.WORK_NAME}_manual",
                    androidx.work.ExistingWorkPolicy.REPLACE,
                    androidx.work.OneTimeWorkRequestBuilder<com.lenz.tennisapp.worker.OddsSyncWorker>().build()
                )
            },
            enabled = !isSyncing,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (showSuccess) Color(0xFF4CAF50) else AuraDeep
            )
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                Spacer(Modifier.width(8.dp))
                Text("Synchronisiere...")
            } else if (showSuccess) {
                Icon(Icons.Default.Check, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Erfolgreich!")
            } else {
                Text("Jetzt synchronisieren")
            }
        }
    }
}

@Composable
private fun UpcomingFeaturesContent() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Roadmap", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        
        val features = listOf(
            "✓" to ("Ranking Integration" to "ATP/WTA Ranking direkt beim Spielernamen"),
            "✓" to ("Aura Design" to "Modernes, dynamisches UI & Verläufe"),
            "🚀" to ("KI mit Google Gemini" to "Fortgeschrittener Prognose-Algorithmus"),
            "📅" to ("Match-Archiv" to "Historische Spiele mit Datumswahl"),
            "🏆" to ("Vorjahressieger" to "Sync historischer Turniergewinner"),
            "💡" to ("KI-Tipps & Analyse" to "Vertiefte Experten-Einblicke pro Match")
        )

        features.forEach { (icon, data) ->
            val (title, desc) = data
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(icon, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                Column {
                    Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AuraDeep)
                    Text(desc, style = MaterialTheme.typography.labelSmall, color = AuraDeep.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun InfoContent() {
    Column {
        Text("Tennis Today v4.7.2", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = AuraDeep)
        Text("Daten von api-tennis.com & tennisabstract.com", style = MaterialTheme.typography.labelSmall, color = AuraDeep.copy(alpha = 0.5f))
    }
}
