package net.sudoer.nipovpn

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import net.sudoer.nipovpn.ui.theme.NipoVPNTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            NipoVPNTheme {
                NipoVpnScreen(
                    onStart = {
                        val intent = Intent(this, NipoVpnService::class.java)
                        ContextCompat.startForegroundService(this, intent)
                    },
                    onStop = {
                        val intent = Intent(this, NipoVpnService::class.java)
                        stopService(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun NipoVpnScreen(
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NipoVPN",
                style = MaterialTheme.typography.headlineLarge
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start NipoVPN")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Stop NipoVPN")
            }
        }
    }
}