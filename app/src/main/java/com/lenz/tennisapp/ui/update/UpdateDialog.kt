package com.lenz.tennisapp.ui.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Drop-in update gate. Checks GitHub for a newer release once per process and,
 * when found, shows a dialog offering to download + install it.
 */
@Composable
fun UpdateGate(viewModel: UpdateViewModel = hiltViewModel()) {
    LaunchedEffect(Unit) { viewModel.checkOnce() }

    val update by viewModel.available.collectAsState()
    val downloading by viewModel.downloading.collectAsState()

    val info = update ?: return

    AlertDialog(
        onDismissRequest = { if (!downloading) viewModel.dismiss() },
        title = { Text("Update verfügbar", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.heightIn(max = 320.dp)) {
                Text("Version ${info.versionName}", fontWeight = FontWeight.SemiBold)
                if (info.apkSizeBytes > 0) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "${"%.1f".format(info.apkSizeBytes / 1_000_000.0)} MB",
                        fontSize = 12.sp
                    )
                }
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        info.releaseNotes,
                        fontSize = 13.sp,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { viewModel.startUpdate() },
                enabled = !downloading
            ) {
                Text(if (downloading) "Lädt…" else "Herunterladen")
            }
        },
        dismissButton = {
            if (!downloading) {
                TextButton(onClick = { viewModel.dismiss() }) { Text("Später") }
            }
        }
    )
}
