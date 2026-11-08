package dev.beehalf.automation

import dev.beehalf.data.local.ActionProposalEntity
import dev.beehalf.data.local.BeehalfDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class ProposalStatus(val storageValue: String) {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    EXPIRED("expired")
}

/**
 * Persists user decisions only. This repository deliberately has no Android
 * context or executor dependency, so approving a proposal cannot run it.
 */
class ActionApprovalRepository(
    private val dao: BeehalfDao,
    private val clock: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { UUID.randomUUID().toString() },
    private val expiryMillis: Long = DEFAULT_EXPIRY_MILLIS
) {
    init {
        require(expiryMillis > 0L) { "Expiry duration must be positive." }
    }

    fun observeProposals(): Flow<List<ActionProposalEntity>> = dao.observeActionProposals()

    suspend fun enqueue(proposal: ActionProposal): ActionProposalEntity {
        val now = clock()
        val policy = ActionPolicy.evaluate(proposal, now)
        require(policy.decision == ActionDecision.APPROVAL_REQUIRED) { policy.reason }
        require(proposal.createdAtEpochMillis <= now) { "Proposal cannot be from the future." }
        val payload = when (val action = proposal.action) {
            is AutomationAction.OpenApp -> Triple("open_app", action.packageName, "")
            is AutomationAction.CreateReminder -> Triple("create_reminder", action.title, action.triggerAtEpochMillis.toString())
            is AutomationAction.OpenWebPage -> Triple("open_web_page", action.url, "")
            is AutomationAction.Unsupported -> error("Unsupported actions cannot enter the approval queue.")
        }
        val expiresAt = Math.addExact(proposal.createdAtEpochMillis, expiryMillis)
        require(expiresAt > now) { "Proposal has already expired." }
        val entity = ActionProposalEntity(
            id = proposal.id,
            actionType = payload.first,
            target = payload.second,
            detail = payload.third,
            summary = proposal.summary.trim(),
            status = ProposalStatus.PENDING.storageValue,
            createdAt = proposal.createdAtEpochMillis,
            expiresAt = expiresAt,
            decidedAt = null
        )
        dao.insertActionProposal(entity)
        return entity
    }

    suspend fun expireStale(): Int = dao.expireActionProposals(clock())

    suspend fun approve(id: String): Boolean {
        val now = clock()
        dao.expireActionProposals(now)
        return dao.decideActionProposal(id, ProposalStatus.APPROVED.storageValue, now) == 1
    }

    suspend fun reject(id: String): Boolean {
        val now = clock()
        dao.expireActionProposals(now)
        return dao.decideActionProposal(id, ProposalStatus.REJECTED.storageValue, now) == 1
    }

    companion object {
        const val DEFAULT_EXPIRY_MILLIS = 24L * 60L * 60L * 1000L
    }
}
