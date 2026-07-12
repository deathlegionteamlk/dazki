package dev.deathlegion.dazki.manager

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.deathlegion.dazki.api.PermissionRequestExtra

/**
 * Activity launched by either the manager callback (when a client app
 * calls requestPermission) or directly by a client app via Dazki
 * .requestPermission(activity). Shows the package name, its signing
 * identity, and Approve / Deny buttons.
 *
 * Result is delivered back to the caller through the in-process
 * DazkiManagerRepository, which updates the allow list and tells the
 * server.
 */
class PermissionRequestActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val packageName = intent.getStringExtra(PermissionRequestExtra.EXTRA_PACKAGE_NAME)
            ?: run {
                finish()
                return
            }
        val callerUid = intent.getIntExtra(PermissionRequestExtra.EXTRA_CALLER_UID, Process.myUid())

        val app = applicationContext as DazkiManagerApp
        val repository = app.repository

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    PermissionDialog(
                        packageName = packageName,
                        callerUid = callerUid,
                        onApprove = {
                            repository.allow(callerUid, packageName)
                            toast("Allowed $packageName")
                            finish()
                        },
                        onDeny = {
                            toast("Denied $packageName")
                            finish()
                        }
                    )
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val TAG = "Dazki/Permission"

        /** Used by the manager callback to launch this activity. */
        fun launch(context: Context, packageName: String, uid: Int) {
            val intent = Intent(context, PermissionRequestActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(PermissionRequestExtra.EXTRA_PACKAGE_NAME, packageName)
                putExtra(PermissionRequestExtra.EXTRA_CALLER_UID, uid)
            }
            context.startActivity(intent)
        }
    }
}

@Composable
private fun PermissionDialog(
    packageName: String,
    callerUid: Int,
    onApprove: () -> Unit,
    onDeny: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Dazki permission request",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "An app wants to use Dazki to call system APIs.")
        Text(text = "Package: $packageName")
        Text(text = "UID: $callerUid")
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onDeny,
                modifier = Modifier.weight(1f),
            ) { Text("Deny") }
            Button(
                onClick = onApprove,
                modifier = Modifier.weight(1f),
            ) { Text("Approve") }
        }
    }
}
