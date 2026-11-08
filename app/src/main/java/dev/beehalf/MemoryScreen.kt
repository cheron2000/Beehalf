package dev.beehalf

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.beehalf.memory.MemoryCategory
import dev.beehalf.memory.MemoryDraft
import dev.beehalf.memory.MemoryCandidate
import dev.beehalf.memory.MemoryCandidateExtractor
import dev.beehalf.data.local.MessageEntity
import dev.beehalf.security.ApiKeyVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import dev.beehalf.memory.MemoryRepository
import kotlinx.coroutines.launch

@Composable
fun MemoryScreen(repository: MemoryRepository, messages: List<MessageEntity>, conversationId: String?, vault: ApiKeyVault) {
    val memories by repository.observeMemories().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var content by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(MemoryCategory.SEMANTIC) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var candidates by remember { mutableStateOf<List<MemoryCandidate>>(emptyList()) }
    var extracting by remember { mutableStateOf(false) }
    var retentionDays by remember { mutableStateOf(180) }
    var retentionMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Saved memory", style = MaterialTheme.typography.titleLarge)
        Text(
            "Memories are stored on this device and may be included as reference context in future chats.",
            style = MaterialTheme.typography.bodySmall
        )
        OutlinedTextField(
            value = content,
            onValueChange = { content = it; error = null },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("What should Beehalf remember?") },
            minLines = 2
        )
        Button(
            enabled = messages.isNotEmpty() && !extracting,
            onClick = {
                scope.launch {
                    extracting = true
                    error = null
                    try {
                        val key = withContext(Dispatchers.IO) { vault.read() }
                        if (key.isNullOrBlank()) {
                            error = "Configure your Groq API key before extracting memories."
                        } else {
                            candidates = MemoryCandidateExtractor().propose(key, messages)
                            if (candidates.isEmpty()) error = "No confident, non-sensitive memories were found."
                        }
                    } catch (_: Exception) {
                        error = "Memory suggestions could not be generated. Check your Groq settings and connection."
                    } finally {
                        extracting = false
                    }
                }
            }
        ) { Text(if (extracting) "Analyzing conversation…" else "Suggest memories from this chat") }
        candidates.forEachIndexed { index, candidate ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(candidate.content, style = MaterialTheme.typography.bodyLarge)
                    Text("${candidate.category.displayName} · confidence ${(candidate.confidence * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                    if (candidate.rationale.isNotBlank()) Text(candidate.rationale, style = MaterialTheme.typography.bodySmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            scope.launch {
                                repository.save(MemoryDraft(candidate.content, candidate.category, "conversation:${conversationId ?: "unknown"}", candidate.confidence))
                                candidates = candidates.filterIndexed { candidateIndex, _ -> candidateIndex != index }
                            }
                        }) { Text("Approve & save") }
                        TextButton(onClick = { candidates = candidates.filterIndexed { candidateIndex, _ -> candidateIndex != index } }) { Text("Dismiss") }
                    }
                }
            }
        }
        Text("Category", style = MaterialTheme.typography.labelMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(MemoryCategory.WORKING, MemoryCategory.EPISODIC, MemoryCategory.SEMANTIC).forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option.displayName) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(MemoryCategory.PROCEDURAL, MemoryCategory.KNOWLEDGE).forEach { option ->
                FilterChip(
                    selected = category == option,
                    onClick = { category = option },
                    label = { Text(option.displayName) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = content.isNotBlank(),
                onClick = {
                    scope.launch {
                        try {
                            repository.save(
                                MemoryDraft(content = content, category = category, source = "user"),
                                existingId = editingId
                            )
                            content = ""
                            editingId = null
                            error = null
                        } catch (_: IllegalArgumentException) {
                            error = "Enter a valid memory before saving."
                        }
                    }
                }
            ) { Text(if (editingId == null) "Save memory" else "Update memory") }
            if (editingId != null) {
                TextButton(onClick = { content = ""; editingId = null; error = null }) { Text("Cancel edit") }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        Text("Retention", style = MaterialTheme.typography.titleMedium)
        Text("Remove unpinned memories older than the selected period. Pinned memories and working summaries are kept.", style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(30, 90, 180, 365).forEach { days ->
                FilterChip(selected = retentionDays == days, onClick = { retentionDays = days }, label = { Text("$days days") })
            }
        }
        Button(onClick = {
            scope.launch {
                val cutoff = System.currentTimeMillis() - retentionDays.toLong() * 24L * 60L * 60L * 1000L
                val deleted = repository.applyRetention(cutoff)
                retentionMessage = "$deleted unpinned memories removed."
            }
        }) { Text("Apply retention") }
        retentionMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

        Text("Your memories (${memories.size})", style = MaterialTheme.typography.titleMedium)
        if (memories.isEmpty()) {
            Text("No saved memories yet. Add one above.", style = MaterialTheme.typography.bodyMedium)
        }
        memories.forEach { memory ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(memory.content, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${MemoryCategory.fromStorage(memory.category).displayName} · ${memory.source} · ${if (memory.pinned) "Pinned" else "Not pinned"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            content = memory.content
                            category = MemoryCategory.fromStorage(memory.category)
                            editingId = memory.id
                        }) { Text("Edit") }
                        TextButton(onClick = {
                            scope.launch { repository.setPinned(memory.id, !memory.pinned) }
                        }) { Text(if (memory.pinned) "Unpin" else "Pin") }
                        TextButton(onClick = {
                            scope.launch {
                                repository.delete(memory.id)
                                if (editingId == memory.id) {
                                    content = ""
                                    editingId = null
                                }
                            }
                        }) { Text("Delete") }
                    }
                }
            }
        }
    }
}
