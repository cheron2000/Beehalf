package dev.beehalf.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "documents",
    indices = [Index(value = ["uri"], unique = true), Index("importedAt")]
)
data class DocumentEntity(
    @PrimaryKey val id: String,
    val uri: String,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val importedAt: Long
) {
    init {
        require(uri.isNotBlank()) { "Document URI cannot be blank" }
        require(displayName.isNotBlank()) { "Document name cannot be blank" }
        require(sizeBytes == null || sizeBytes >= 0L) { "Document size cannot be negative" }
    }
}
