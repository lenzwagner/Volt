package com.lenz.tennisapp.ui.screens.settings

import androidx.compose.foundation.border
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.lenz.tennisapp.ui.components.GreenHeader
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.work.WorkManager
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.lenz.tennisapp.worker.RankingsAndEloSyncWorker
import com.lenz.tennisapp.ui.theme.AuraPurple
import com.lenz.tennisapp.ui.theme.AuraDeep
import java.text.SimpleDateFormat
import java.util.Date
import kotlinx.coroutines.launch
import androidx.compose.foundation.horizontalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    showHeader: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var isRefreshing by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
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
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 160.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SectionLabel("Netzwerk & APIs") }

                item {
                    ApiSettingsBox(state = state, viewModel = viewModel)
                }

                item { SectionLabel("UI Anpassungen") }

                item { ExpandableUISettingsCard() }

                item { SectionLabel("Daten") }

                item { RankingsSyncCard() }

                item { OddsSyncCard() }

                item { SectionLabel("Upcoming Features") }

                item { UpcomingFeaturesCard() }

                item { SectionLabel("Info") }

                item { InfoCard() }
            }
        }
    }
}

@Composable
private fun ApiSettingsBox(
    state: SettingsUiState,
    viewModel: SettingsViewModel
) {
    var isExpanded by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.VpnKey, null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Column {
                        Text("API Konfiguration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Verwalte und teste deine API-Schlüssel", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
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

                    // Odds API Section
                    ApiKeySection(
                        title = "The Odds API",
                        subtitle = "the-odds-api.com",
                        currentKey = state.oddsKey,
                        isExpired = state.oddsKeyExpired,
                        statusText = if (state.oddsKeyExpired) "Limit erreicht" else "${state.oddsRequestsRemaining} Anfragen übrig",
                        isTesting = state.isTestingOdds,
                        testResult = state.oddsTestResult,
                        signupUrl = "https://the-odds-api.com",
                        onSave = viewModel::saveOddsKey,
                        onTest = { viewModel.testOddsKey(it) }
                    )
                }
            }
        }
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
private fun ExpandableUISettingsCard() {
    var expandedMenu by remember { mutableStateOf(false) }
    var expandedGameView by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // App-Menüs Section
        UIAdaptationBox(
            title = "App-Menüs",
            subtitle = "Home, Live, Heute, Prognosen",
            isExpanded = expandedMenu,
            onExpandChange = { 
                expandedMenu = it
                if (it) expandedGameView = false
            },
            content = { UISettingsPanel(sectionType = "menu") }
        )

        // Spiel Ansicht Section
        UIAdaptationBox(
            title = "Spiel Ansicht",
            subtitle = "Match Details & Spieler Profile",
            isExpanded = expandedGameView,
            onExpandChange = { 
                expandedGameView = it
                if (it) expandedMenu = false
            },
            content = { UISettingsPanel(sectionType = "game") }
        )
    }
}

@Composable
private fun UIAdaptationBox(
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    content: @Composable () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!isExpanded) }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(
                    if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(16.dp))
                    content()
                }
            }
        }
    }
}

@Composable
private fun UISettingsPanel(sectionType: String) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("TennisAppSettings", Context.MODE_PRIVATE) }
    var blurRadius by remember { mutableStateOf(sharedPrefs.getFloat("blur_radius", 20f)) }
    var gradientIntensity by remember { mutableStateOf(sharedPrefs.getFloat("gradient_intensity", 0.6f)) }
    var gradientHeight by remember { mutableStateOf(sharedPrefs.getFloat("gradient_height", 0.25f)) }

    Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
        SliderWithDescription(
            title = "Header-Unschärfe",
            description = "Stärke des Weichzeichners auf dem Hintergrundbild.",
            value = blurRadius,
            onValueChange = { blurRadius = it },
            valueRange = 0f..40f,
            steps = 7,
            minLabel = "Klar",
            maxLabel = "Verschwommen"
        )
        SliderWithDescription(
            title = "Overlay-Dunkelheit",
            description = "Deckkraft des dunklen Filters für bessere Lesbarkeit.",
            value = gradientIntensity,
            onValueChange = { gradientIntensity = it },
            valueRange = 0.2f..0.8f,
            steps = 5,
            minLabel = "Hell",
            maxLabel = "Dunkel"
        )
        SliderWithDescription(
            title = "Overlay-Abdeckung",
            description = "Wie weit der Filter von oben in das Bild hineinragt.",
            value = gradientHeight,
            onValueChange = { gradientHeight = it },
            valueRange = 0.05f..0.5f,
            steps = 9,
            minLabel = "Gering",
            maxLabel = "Mittel"
        )
        
        Button(
            onClick = {
                sharedPrefs.edit().apply {
                    putFloat("blur_radius", blurRadius)
                    putFloat("gradient_intensity", gradientIntensity)
                    putFloat("gradient_height", gradientHeight)
                    apply()
                }
                android.widget.Toast.makeText(context, "Gespeichert! Neustart...", android.widget.Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ Process.killProcess(Process.myPid()) }, 500)
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = AuraDeep,
                contentColor = Color.White
            )
        ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Speichern & Neustarten", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SliderWithDescription(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    minLabel: String,
    maxLabel: String
) {
    Column {
        Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(minLabel, style = MaterialTheme.typography.labelSmall)
            Slider(
                value = value, 
                onValueChange = onValueChange, 
                valueRange = valueRange, 
                steps = steps, 
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = AuraPurple,
                    activeTrackColor = AuraPurple
                )
            )
            Text(maxLabel, style = MaterialTheme.typography.labelSmall)
        }
        Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = AuraPurple,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun RankingsSyncCard() {
    val context = LocalContext.current
    val workManager = remember { WorkManager.getInstance(context) }
    val workInfos by workManager
        .getWorkInfosForUniqueWorkLiveData("${RankingsAndEloSyncWorker.WORK_NAME}_manual")
        .observeAsState()

    val syncState = workInfos?.firstOrNull()?.state
    val isSyncing = syncState == androidx.work.WorkInfo.State.ENQUEUED ||
            syncState == androidx.work.WorkInfo.State.RUNNING

    var showSuccess by remember { mutableStateOf(false) }

    // Show success for 3 seconds after completion
    LaunchedEffect(syncState) {
        if (syncState == androidx.work.WorkInfo.State.SUCCEEDED) {
            showSuccess = true
            kotlinx.coroutines.delay(3000L)
            showSuccess = false
        }
    }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Rankings und Elo-Scores", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Täglicher automatischer Sync der ATP/WTA Rankings & Elo Scores (um 3:00 Uhr).", 
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            
            Button(
                onClick = {
                    workManager.enqueueUniqueWork(
                        "${RankingsAndEloSyncWorker.WORK_NAME}_manual", 
                        androidx.work.ExistingWorkPolicy.REPLACE, 
                        androidx.work.OneTimeWorkRequestBuilder<RankingsAndEloSyncWorker>().build()
                    )
                }, 
                enabled = !isSyncing, 
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
}

@Composable
private fun OddsSyncCard() {
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

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Tipico Wettquoten", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text("Täglicher automatischer Sync der Wettquoten (um 7:00 Uhr). Hier manuell für alle offenen Spiele aktualisieren.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(
                onClick = {
                    workManager.enqueueUniqueWork(
                        "${com.lenz.tennisapp.worker.OddsSyncWorker.WORK_NAME}_manual",
                        androidx.work.ExistingWorkPolicy.REPLACE,
                        androidx.work.OneTimeWorkRequestBuilder<com.lenz.tennisapp.worker.OddsSyncWorker>().build()
                    )
                },
                enabled = !isSyncing,
                modifier = Modifier.fillMaxWidth().height(48.dp),
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
}

@Composable
private fun UpcomingFeaturesCard() {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 32.dp)),
        colors = CardDefaults.elevatedCardColors(
            containerColor = AuraPurple.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(
            topStart = 32.dp, 
            topEnd = 16.dp, 
            bottomStart = 16.dp, 
            bottomEnd = 32.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Kommende Features", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            
            val features = listOf(
                "✓" to ("Ranking Integration" to "ATP/WTA Ranking direkt beim Spielernamen"),
                "✓" to ("Profil-Update" to "Überarbeitete Player Page mit Statistiken"),
                "🚀" to ("KI mit Google Gemini" to "Fortgeschrittener Prognose-Algorithmus"),
                "📅" to ("Match-Archiv" to "Historische Spiele mit Datumswahl"),
                "👤" to ("Spieler-Historie" to "Direkte Links zu vergangenen Matches"),
                "🧹" to ("Juniors-Filter" to "Ausblenden von Jugend-Turnieren"),
                "🏆" to ("Vorjahressieger" to "Sync historischer Turniergewinner"),
                "💡" to ("KI-Tipps & Analyse" to "Vertiefte Experten-Einblicke pro Match")
            )

            features.forEach { (icon, data) ->
                val (title, desc) = data
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(icon, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.width(20.dp))
                    Column {
                        Text(title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        Text(desc, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tennis Today v1.0.0", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            Text("Daten von api-tennis.com & tennisabstract.com", style = MaterialTheme.typography.bodySmall)
        }
    }
}
