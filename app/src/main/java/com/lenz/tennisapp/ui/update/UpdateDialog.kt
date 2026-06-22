package com.lenz.tennisapp.ui.update

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SystemUpdate
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
        shape = RoundedCornerShape(28.dp),
        tonalElevation = 6.dp,
        icon = {
            Icon(
                Icons.Rounded.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                "Update verfügbar",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 340.dp)) {
                AssistChipRow(versionName = info.versionName, sizeBytes = info.apkSizeBytes)
                if (info.releaseNotes.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Neuerungen",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        info.releaseNotes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            FilledTonalButton(
                onClick = { viewModel.startUpdate() },
                enabled = !downloading
            ) {
                if (downloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Lädt…")
                } else {
                    Text("Herunterladen")
                }
            }
        },
        dismissButton = {
            if (!downloading) {
                TextButton(onClick = { viewModel.dismiss() }) { Text("Später") }
            }
        }
    )
}

@Composable
private fun AssistChipRow(versionName: String, sizeBytes: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text("Version $versionName") }
        )
        if (sizeBytes > 0) {
            AssistChip(
                onClick = {},
                enabled = false,
                label = { Text("${"%.1f".format(sizeBytes / 1_000_000.0)} MB") }
            )
        }
    }
}
