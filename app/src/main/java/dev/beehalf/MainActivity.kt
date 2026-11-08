package dev.beehalf

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.beehalf.ai.groq.GroqClient
import dev.beehalf.security.ApiKeyVault
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vault = ApiKeyVault(applicationContext)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BeehalfHome(vault = vault)
                }
            }
        }
    }
}

@Composable
private fun BeehalfHome(vault: ApiKeyVault) {
    val scope = rememberCoroutineScope()
    var apiKey by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("llama-3.3-70b-versatile") }
    var status by remember { mutableStateOf("Add your Groq API key to connect inference.") }
    var busy by remember { mutableStateOf(false) }
    var connected by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiKey = vault.read().orEmpty()
        connected = apiKey.isNotBlank()
        if (connected) status = "API key stored on this device. Validate the connection."
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Beehalf", style = MaterialTheme.typography.headlineLarge)
        Text("Android-first AI automation", style = MaterialTheme.typography.titleMedium)
        Text(
            "Local-first memory and knowledge. Groq provides language generation; your data stays on this device unless included in an inference request.",
            style = MaterialTheme.typography.bodyMedium
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("LLM provider · Groq BYOK", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; connected = false },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Groq API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Model ID") },
                    singleLine = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        enabled = apiKey.isNotBlank() && !busy,
                        onClick = {
                            scope.launch {
                                busy = true
                                status = "Validating Groq connection…"
                                runCatching { GroqClient().validate(apiKey.trim()) }
                                    .onSuccess {
                                        vault.save(apiKey.trim())
                                        connected = true
                                        status = "Groq connection validated. Key encrypted at rest."
                                    }
                                    .onFailure { status = "Connection failed: ${it.message ?: "Unknown error"}" }
                                busy = false
                            }
                        }
                    ) { Text(if (busy) "Checking…" else "Validate & save") }
                    TextButton(onClick = {
                        scope.launch {
                            vault.clear()
                            apiKey = ""
                            connected = false
                            status = "Stored API key removed."
                        }
                    }) { Text("Remove key") }
                }
                Text(status, style = MaterialTheme.typography.bodySmall)
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Core modules", style = MaterialTheme.typography.titleMedium)
                ModuleStatus("Memory engine", "Foundation planned · local Room storage")
                ModuleStatus("RAG engine", "Foundation planned · on-device document index")
                ModuleStatus("Automation core", "Permission-gated action execution")
                ModuleStatus("WhatsApp adapter", "Not enabled · notification access will be opt-in")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("Beehalf 0.1.0 · Early development", style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun ModuleStatus(title: String, detail: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
