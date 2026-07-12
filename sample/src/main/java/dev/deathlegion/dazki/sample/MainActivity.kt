package dev.deathlegion.dazki.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.deathlegion.dazki.api.Dazki
import dev.deathlegion.dazki.api.DazkiFlags
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sample client. Demonstrates the three things a third-party app does
 * with Dazki:
 *
 *   1. check whether the service is running
 *   2. ask the user for permission (via the manager app)
 *   3. call through the service to a privileged API
 *
 * The sample depends on the api module, which is the same AAR a real
 * third-party app would publish to Maven and consume.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dazki.init(applicationContext)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    SampleScreen()
                }
            }
        }
    }
}

@Composable
private fun SampleScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var running by remember { mutableStateOf(false) }
    var allowed by remember { mutableStateOf(false) }
    var output by remember { mutableStateOf("(no calls yet)") }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            running = Dazki.isServiceRunning()
            allowed = Dazki.isAllowed()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("dazki sample", style = MaterialTheme.typography.headlineSmall)

        InfoCard("Service running", running.toString())
        InfoCard("This app allowed", allowed.toString())
        InfoCard("Platform", Dazki.platformInfo())

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    running = Dazki.isServiceRunning()
                    allowed = Dazki.isAllowed()
                },
                modifier = Modifier.weight(1f),
            ) { Text("Refresh") }
            OutlinedButton(
                onClick = { Dazki.requestPermission(context) },
                modifier = Modifier.weight(1f),
            ) { Text("Request permission") }
        }

        Button(
            onClick = {
                output = try {
                    if (!running) "service is not running"
                    else if (!allowed) "not allowed; call Request permission first"
                    else {
                        val service = Dazki.getService()
                        val packages = service.listPackages(DazkiFlags.MATCH_UNINSTALLED_PACKAGES)
                        "listPackages returned ${packages.size} entries\nfirst 5: ${packages.take(5).joinToString()}"
                    }
                } catch (e: Throwable) {
                    "call failed: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Call listPackages(MATCH_UNINSTALLED_PACKAGES)") }

        Button(
            onClick = {
                output = try {
                    if (!running || !allowed) "service not running or not allowed"
                    else {
                        val service = Dazki.getService()
                        val ok = service.forceStopPackage(context.packageName)
                        "forceStopPackage(self) -> $ok"
                    }
                } catch (e: Throwable) {
                    "call failed: ${e.message}"
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Call forceStopPackage(self)") }

        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1C))
        ) {
            Text(
                text = output,
                color = Color(0xFFE0E0E0),
                modifier = Modifier.padding(12.dp),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun InfoCard(label: String, value: String) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
