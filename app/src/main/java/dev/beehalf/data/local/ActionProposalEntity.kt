package dev.beehalf.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "action_proposals",
    indices = [Index("status"), Index("expiresAt")]
)
data class ActionProposalEntity(
    @PrimaryKey val id: String,
    val actionType: String,
    val target: String,
    val detail: String,
    val summary: String,
    val status: String,
    val createdAt: Long,
    val expiresAt: Long,
    val decidedAt: Long?
)
