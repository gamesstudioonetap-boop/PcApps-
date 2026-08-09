package com.pcapps.launcher

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pcapps.launcher.manager.*
import com.pcapps.launcher.model.AppProfile
import com.pcapps.launcher.ui.AppDetailScreen
import com.pcapps.launcher.ui.PCAppsTheme

class MainActivity : ComponentActivity() {

    private lateinit var storageAccess: StorageAccessManager
    private lateinit var environmentManager: EnvironmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        storageAccess = StorageAccessManager(this)
        environmentManager = EnvironmentManager(this)

        setContent {
            PCAppsTheme {
                var pickedFile by remember { mutableStateOf<PickedFile?>(null) }

                val filePicker = storageAccess.registerFilePicker { uri ->
                    val name = storageAccess.displayNameFor(uri)
                    val detection = AppDetector.detect(contentResolver, uri, name)
                    pickedFile = PickedFile(uri, name, detection)
                }

                Surface(modifier = Modifier.fillMaxSize()) {
                    if (pickedFile == null) {
                        HomeScreen(
                            onOpenPcApp = {
                                filePicker.launch(
                                    arrayOf(
                                        "application/x-msdownload",   // .exe
                                        "application/x-msi",          // .msi
                                        "application/octet-stream"    // generic/Linux binaries
                                    )
                                )
                            }
                        )
                    } else {
                        AppDetailScreen(
                            picked = pickedFile!!,
                            environments = environmentManager.listEnvironments(),
                            onCreateEnvironment = { name ->
                                environmentManager.createEnvironment(name, pickedFile!!.detection.runtimeKind)
                            },
                            onBack = { pickedFile = null }
                        )
                    }
                }
            }
        }
    }
}

data class PickedFile(
    val uri: Uri,
    val fileName: String,
    val detection: DetectionResult
)

@Composable
fun HomeScreen(onOpenPcApp: () -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("PC Apps Launcher") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(20.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            HomeActionCard(
                icon = Icons.Filled.FolderOpen,
                title = "Open PC App",
                subtitle = "Pick a .exe, .msi, or Linux binary",
                onClick = onOpenPcApp
            )
            HomeActionCard(
                icon = Icons.Filled.History,
                title = "Recent Applications",
                subtitle = "Apps you've launched before",
                onClick = { /* TODO stage 4: recent list backed by profile store */ }
            )
            HomeActionCard(
                icon = Icons.Filled.PlayArrow,
                title = "Running Applications",
                subtitle = "Currently active sessions",
                onClick = { /* TODO stage 6: wired to RuntimeLauncher process list */ }
            )
            HomeActionCard(
                icon = Icons.Filled.Folder,
                title = "Environments",
                subtitle = "Manage isolated app containers",
                onClick = { /* TODO: environment list/manage screen */ }
            )
            HomeActionCard(
                icon = Icons.Filled.Settings,
                title = "Settings",
                subtitle = "Graphics, performance, storage grants",
                onClick = { /* TODO: settings screen */ }
            )
        }
    }
}

@Composable
private fun HomeActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
