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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.beehalf.ai.groq.GroqClient
import dev.beehalf.chat.ChatViewModel
import dev.beehalf.data.local.BeehalfDatabase
import dev.beehalf.memory.ConversationRepository
import dev.beehalf.memory.MemoryRepository
import dev.beehalf.security.ApiKeyVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val vault = ApiKeyVault(applicationContext)
        val dao = BeehalfDatabase.getInstance(applicationContext).beehalfDao()
        val repository = ConversationRepository(dao)
        val memoryRepository = MemoryRepository(dao)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
                    return ChatViewModel(repository, memoryRepository, vault) as T
                }
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BeehalfHome(vault = vault, factory = factory, memoryRepository = memoryRepository)
                }
            }
        }
    }
}

@Composable
private fun BeehalfHome(
    vault: ApiKeyVault,
    factory: ViewModelProvider.Factory,
    memoryRepository: MemoryRepository
) {
    val chatViewModel: ChatViewModel = viewModel(factory = factory)
    val state by chatViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var showMemories by remember { mutableStateOf(false) }

    var apiKey by remember { mutableStateOf("") }
    var providerModel by remember { mutableStateOf("llama-3.3-70b-versatile") }
    var providerStatus by remember { mutableStateOf("Configure Groq to enable responses.") }
    var providerBusy by remember { mutableStateOf(false) }
    var providerExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        apiKey = withContext(Dispatchers.IO) { vault.read().orEmpty() }
        if (apiKey.isNotBlank()) providerStatus = "A key is stored on this device. Validate it before chatting."
    }
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex)
    }

    Scaffold(
        bottomBar = {
            Surface(shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier.fillMaxWidth().imePadding().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = state.draft,
                        onValueChange = chatViewModel::updateDraft,
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Message Beehalf…") },
                        maxLines = 4,
                        enabled = !state.isSending
                    )
                    Button(
                        onClick = chatViewModel::sendMessage,
                        enabled = state.draft.isNotBlank() && !state.isSending
                    ) { Text(if (state.isSending) "…" else "Send") }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Beehalf", style = MaterialTheme.typography.headlineMedium)
                    Text("Private, local-first AI assistant", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showMemories = !showMemories }) { Text(if (showMemories) "Chat" else "Memories") }
                    if (!showMemories) TextButton(onClick = chatViewModel::startNewConversation) { Text("New chat") }
                }
            }

            if (showMemories) {
                MemoryScreen(repository = memoryRepository)
            } else {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                state.conversations.forEachIndexed { index, conversation ->
                    FilterChip(
                        selected = conversation.id == state.selectedConversationId,
                        onClick = { chatViewModel.selectConversation(conversation.id) },
                        label = { Text(conversation.title.ifBlank { "Chat ${index + 1}" }, maxLines = 1) }
                    )
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Groq provider", style = MaterialTheme.typography.titleSmall)
                            Text(providerStatus, style = MaterialTheme.typography.bodySmall)
                        }
                        TextButton(onClick = { providerExpanded = !providerExpanded }) {
                            Text(if (providerExpanded) "Hide" else "Settings")
                        }
                    }
                    if (providerExpanded) {
                        OutlinedTextField(
                            value = apiKey,
                            onValueChange = { apiKey = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Groq API key") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = providerModel,
                            onValueChange = {
                                providerModel = it
                                chatViewModel.updateModel(it)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Model ID") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = apiKey.isNotBlank() && !providerBusy,
                                onClick = {
                                    scope.launch {
                                        providerBusy = true
                                        providerStatus = "Validating connection…"
                                        try {
                                            withContext(Dispatchers.IO) {
                                                GroqClient().validate(apiKey.trim())
                                                vault.save(apiKey.trim())
                                            }
                                            chatViewModel.updateModel(providerModel)
                                            providerStatus = "Connected. API key encrypted on this device."
                                        } catch (_: Exception) {
                                            providerStatus = "Connection failed. Check the key and network."
                                        } finally {
                                            providerBusy = false
                                        }
                                    }
                                }
                            ) { Text(if (providerBusy) "Checking…" else "Validate & save") }
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) { vault.clear() }
                                        apiKey = ""
                                        providerStatus = "API key removed from this device."
                                    }
                                }
                            ) { Text("Remove key") }
                        }
                    }
                }
            }

            state.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            if (state.messages.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Start a conversation", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Messages are saved locally. When you send a prompt, Beehalf selects a bounded recent context window and sends that context to Groq for a response.",
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.messages, key = { it.id }) { message ->
                        MessageBubble(role = message.role, content = message.content)
                    }
                    if (state.isSending) {
                        item { Text("Beehalf is thinking…", style = MaterialTheme.typography.bodySmall) }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(role: String, content: String) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.88f),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (isUser) "You" else "Beehalf", style = MaterialTheme.typography.labelMedium)
                Text(content, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
