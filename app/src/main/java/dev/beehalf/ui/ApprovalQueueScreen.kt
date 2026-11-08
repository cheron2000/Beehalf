package dev.beehalf.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.beehalf.automation.ActionApprovalRepository
import dev.beehalf.data.local.ActionProposalEntity
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun ApprovalQueueScreen(repository: ActionApprovalRepository) {
    val proposals by repository.observeProposals().collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(repository) { repository.expireStale() }

    Column(
        modifier = Modifier.fillMaxSize().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Action approval queue", style = MaterialTheme.typography.titleLarge)
        Text(
            "Review each proposed action. Approval records your decision only; no action is executed.",
            style = MaterialTheme.typography.bodySmall
        )
        if (proposals.isEmpty()) {
            Text("No action proposals yet.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(proposals, key = { it.id }) { proposal ->
                    ApprovalProposalCard(
                        proposal = proposal,
                        onApprove = { scope.launch { repository.approve(proposal.id) } },
                        onReject = { scope.launch { repository.reject(proposal.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun ApprovalProposalCard(
    proposal: ActionProposalEntity,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    val pending = proposal.status == "pending"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(proposal.summary, style = MaterialTheme.typography.titleSmall)
            Text("Action: ${proposal.actionType.replace('_', ' ')}")
            Text(
                when (proposal.actionType) {
                    "open_app" -> "Package: ${proposal.target}"
                    "create_reminder" -> "Title: ${proposal.target}\nTrigger: ${proposal.detail}"
                    "open_web_page" -> "HTTPS address: ${proposal.target}"
                    else -> "Target: ${proposal.target}"
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Text("Status: ${proposal.status.uppercase()}", style = MaterialTheme.typography.labelMedium)
            Text("Expires: ${DateFormat.getDateTimeInstance().format(Date(proposal.expiresAt))}", style = MaterialTheme.typography.bodySmall)
            if (pending) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onApprove) { Text("Approve") }
                    TextButton(onClick = onReject) { Text("Reject") }
                }
            } else if (proposal.decidedAt != null) {
                Text(
                    "Decision recorded: ${DateFormat.getDateTimeInstance().format(Date(proposal.decidedAt))}. No action was executed.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
