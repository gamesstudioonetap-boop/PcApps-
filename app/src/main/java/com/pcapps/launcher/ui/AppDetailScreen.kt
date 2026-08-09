package com.pcapps.launcher.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pcapps.launcher.PickedFile
import com.pcapps.launcher.model.EnvironmentConfig
import com.pcapps.launcher.model.RuntimeKind

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDetailScreen(
    picked: PickedFile,
    environments: List<EnvironmentConfig>,
    onCreateEnvironment: (String) -> EnvironmentConfig,
    onBack: () -> Unit
) {
    var selectedEnvironment by remember { mutableStateOf(environments.firstOrNull()) }
    val supported = picked.detection.runtimeKind != RuntimeKind.UNSUPPORTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(picked.fileName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (!supported) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Warning, contentDescription = null,
                            tint = MaterialTheme.colorScheme.error)
                        Column {
                            Text("Unsupported file", style = MaterialTheme.typography.titleMedium)
                            Text(picked.detection.message, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            DetailRow("Architecture", picked.detection.arch.name)
            DetailRow("Compatibility layer", picked.detection.runtimeKind.name)
            DetailRow("Detection notes", picked.detection.message)
            DetailRow("File", picked.fileName)

            Divider()

            Text("Environment", style = MaterialTheme.typography.titleMedium)
            if (environments.isEmpty()) {
                Text(
                    "No environments yet. Create one to install this app into.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Button(onClick = {
                    selectedEnvironment = onCreateEnvironment(picked.fileName.substringBeforeLast('.'))
                }) {
                    Text("Create environment for this app")
                }
            } else {
                environments.forEach { env ->
                    ElevatedCard(
                        onClick = { selectedEnvironment = env },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(env.name)
                            if (selectedEnvironment?.id == env.id) {
                                Text("Selected", color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { /* Wired to RuntimeLauncher in a later stage */ },
                enabled = supported && selectedEnvironment != null,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (supported) "Launch" else "Cannot Launch — Unsupported")
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value)
    }
}
