package dev.deathlegion.dazki.manager

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Single screen manager UI. Status pill at the top, start and stop
 * buttons, an ADB instructions block the user can copy, and the list
 * of allowed apps at the bottom.
 *
 * The status pill polls the server every 2 seconds. Cheap binder
 * ping; no point wiring up a push for it.
 */
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = applicationContext as DazkiManagerApp
        val repository = app.repository

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ManagerScreen(repository)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManagerScreen(repo: DazkiManagerRepository) {
    val context = LocalContext.current
    var running by remember { mutableStateOf(false) }
    var serverUid by remember { mutableStateOf(-1) }
    var starterScript by remember { mutableStateOf("") }
    var showInstructions by remember { mutableStateOf(false) }
    val allowedApps = remember { mutableStateListOf<Pair<Int, String>>() }
    var lastRefresh by remember { mutableStateOf("--") }
    val refreshHandler = remember { Handler(Looper.getMainLooper()) }

    fun refresh() {
        running = repo.isServiceRunning()
        serverUid = if (running) repo.serverUid() else -1
        allowedApps.clear()
        allowedApps.addAll(repo.allowedApps)
        lastRefresh = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())

        if (running) {
            // Re-register so the server knows our uid is the manager.
            repo.registerWithServer()
        }
    }

    DisposableEffect(Unit) {
        val poller = object : Runnable {
            override fun run() {
                refresh()
                refreshHandler.postDelayed(this, 2000)
            }
        }
        refreshHandler.post(poller)
        onDispose {
            refreshHandler.removeCallbacks(poller)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("dazki") },
                actions = {
                    IconButton(onClick = { refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(running, serverUid, lastRefresh)

            ActionButtons(
                running = running,
                onStart = {
                    starterScript = repo.buildStarterScript()
                    showInstructions = true
                },
                onStop = {
                    val ok = repo.shutdown()
                    toast(context, if (ok) "Stop request sent" else "Stop failed; the server may not be running")
                    refresh()
                },
                onRefresh = { refresh() }
            )

            AndroidVersionNotes()

            AllowedAppsCard(
                apps = allowedApps,
                onRevoke = { uid, pkg ->
                    repo.revoke(uid, pkg)
                    refresh()
                }
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "dazki by death legion team",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }

    if (showInstructions) {
        ModalBottomSheet(
            onDismissRequest = { showInstructions = false },
            sheetState = rememberModalBottomSheetState(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Start the service", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Pick one of two ways. ADB is recommended. Use root only if your device is rooted.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))

                Text("Option A: ADB", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "ADB starts the server as shell uid (2000). That gives it access to most system APIs through the shell role, but not to settings protected by WRITE_SECURE_SETTINGS or to apps targeting the latest Android that hide their data from shell.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                Text("1. Connect your phone to a PC with ADB enabled.")
                Text("2. Run these commands in a terminal:")
                Spacer(Modifier.height(8.dp))
                CodeBlock(
                    text = "adb push dazki-starter.sh /data/local/tmp/\nadb shell sh /data/local/tmp/dazki-starter.sh &",
                    onCopy = { copy(context, it) }
                )

                Spacer(Modifier.height(20.dp))
                Text("Option B: root (su)", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Root starts the server as uid 0. More APIs work, but the server can do anything on the device. Only use this on devices you control.",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                CodeBlock(
                    text = "su -c \"sh /data/local/tmp/dazki-starter.sh\" &",
                    onCopy = { copy(context, it) }
                )

                Spacer(Modifier.height(20.dp))
                Text("Full script", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "The manager already filled in the paths. Copy this script to a file named dazki-starter.sh on your PC:",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                CodeBlock(
                    text = starterScript,
                    onCopy = { copy(context, it) }
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun StatusCard(running: Boolean, serverUid: Int, lastRefresh: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (running) Color(0xFF1B5E20) else Color(0xFF37474F)
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = if (running) Color(0xFF76FF03) else Color(0xFFB0BEC5),
                            shape = CircleShape,
                        )
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    text = if (running) "Service running" else "Service stopped",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (running)
                    "Server uid: ${describeUid(serverUid)}"
                else
                    "Press Start for instructions on how to launch the service.",
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Last checked: $lastRefresh",
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

private fun describeUid(uid: Int): String {
    val role = when (uid) {
        0 -> "root"
        1000 -> "system"
        2000 -> "shell"
        else -> "uid $uid"
    }
    return "$uid ($role)"
}

@Composable
private fun ActionButtons(
    running: Boolean,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onStart,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
        ) {
            Text("Start")
        }
        OutlinedButton(
            onClick = onStop,
            modifier = Modifier.weight(1f),
            enabled = running,
        ) {
            Text("Stop")
        }
        OutlinedButton(
            onClick = onRefresh,
            modifier = Modifier.weight(1f),
        ) {
            Text("Refresh")
        }
    }
}

@Composable
private fun AndroidVersionNotes() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Android version notes", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Shell uid has been losing access on each Android release. " +
                "On Android 10+, apps that target the latest SDK hide their data from shell. " +
                "On Android 11+, several Settings.Global keys became read-only even for shell. " +
                "On Android 12+, PackageManager hides some packages from shell unless you pass MATCH_UNINSTALLED_PACKAGES. " +
                "On Android 14+, the shell role no longer covers some hidden APIs that used to work.",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your device: Android ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT}).",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun AllowedAppsCard(
    apps: List<Pair<Int, String>>,
    onRevoke: (Int, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Allowed apps", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (apps.isEmpty()) {
                Text(
                    "No apps are allowed yet. A client app can call Dazki.requestPermission() to ask.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            } else {
                apps.forEach { (uid, pkg) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(pkg, style = MaterialTheme.typography.bodyMedium)
                            Text("uid $uid", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                        OutlinedButton(onClick = { onRevoke(uid, pkg) }) {
                            Text("Revoke")
                        }
                    }
                    Divider()
                }
            }
        }
    }
}

@Composable
private fun CodeBlock(text: String, onCopy: (String) -> Unit) {
    Surface(
        color = Color(0xFF1C1C1C),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
            Text(
                text = text,
                color = Color(0xFFE0E0E0),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { onCopy(text) }) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White)
            }
        }
    }
}

private fun copy(context: Context, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    cm.setPrimaryClip(ClipData.newPlainText("dazki", text))
    toast(context, "Copied")
}

private fun toast(context: Context, msg: String) {
    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
}
