package dev.beehalf.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.beehalf.data.DocumentRepository
import dev.beehalf.data.local.DocumentEntity
import kotlinx.coroutines.launch

@Composable
fun DocumentScreen(repository: DocumentRepository) {
    val context = LocalContext.current
    val documents by repository.observeDocuments().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Documents stay on this device. TXT and Markdown files can now be previewed locally.") }
    var previewTitle by remember { mutableStateOf<String?>(null) }
    var previewContent by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            scope.launch {
                status = try {
                    repository.importFromUri(context, uri)
                    "Document reference saved locally."
                } catch (_: SecurityException) {
                    "Access was not granted. Please select the document again."
                } catch (_: Exception) {
                    "Could not import this document. Try selecting it again."
                }
            }
        }
    }

    previewContent?.let { content ->
        AlertDialog(
            onDismissRequest = {
                previewContent = null
                previewTitle = null
            },
            title = { Text(previewTitle ?: "Document preview") },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(content, style = MaterialTheme.typography.bodyMedium)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    previewContent = null
                    previewTitle = null
                }) { Text("Close") }
            }
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Local documents", style = MaterialTheme.typography.titleLarge)
        Text(status, style = MaterialTheme.typography.bodySmall)
        Button(onClick = { picker.launch(arrayOf("*/*")) }) {
            Text("Import document")
        }
        if (documents.isEmpty()) {
            Text("No documents imported yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(documents, key = { it.id }) { document ->
                    DocumentCard(
                        document = document,
                        onPreview = {
                            scope.launch {
                                status = "Reading document locally..."
                                try {
                                    val text = repository.readTextContent(context, document)
                                    previewTitle = document.displayName
                                    previewContent = if (text.length > MAX_PREVIEW_CHARS) {
                                        text.take(MAX_PREVIEW_CHARS) + "\n\n[Preview truncated. Full document extraction will be handled by the indexing pipeline.]"
                                    } else {
                                        text.ifBlank { "[This document is empty.]" }
                                    }
                                    status = "Document read locally. Its contents have not been uploaded."
                                } catch (error: Exception) {
                                    status = error.message ?: "Could not read this document."
                                }
                            }
                        },
                        onRemove = {
                            scope.launch {
                                repository.remove(context, document)
                                status = "Document reference removed."
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentCard(
    document: DocumentEntity,
    onPreview: () -> Unit,
    onRemove: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Text(document.displayName, style = MaterialTheme.typography.titleSmall)
            Text(document.mimeType, style = MaterialTheme.typography.bodySmall)
            Text(
                document.sizeBytes?.let { formatBytes(it) } ?: "Size unavailable",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onPreview) { Text("Read") }
                TextButton(onClick = onRemove) { Text("Remove") }
            }
        }
    }
}

private const val MAX_PREVIEW_CHARS = 12_000

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024L -> "$bytes B"
    bytes < 1024L * 1024L -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}
