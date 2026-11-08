package dev.beehalf.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import dev.beehalf.data.local.BeehalfDao
import dev.beehalf.data.local.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID

class DocumentRepository(private val dao: BeehalfDao) {
    fun observeDocuments(): Flow<List<DocumentEntity>> = dao.observeDocuments()

    suspend fun importFromUri(context: Context, uri: Uri): DocumentEntity = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        resolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        val metadata = queryMetadata(resolver, uri)
        val existing = dao.getDocumentByUri(uri.toString())
        val document = DocumentEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            uri = uri.toString(),
            displayName = metadata.first,
            mimeType = resolver.getType(uri) ?: "application/octet-stream",
            sizeBytes = metadata.second,
            importedAt = existing?.importedAt ?: System.currentTimeMillis()
        )
        dao.upsertDocument(document)
        document
    }

    suspend fun remove(context: Context, document: DocumentEntity) = withContext(Dispatchers.IO) {
        dao.deleteDocument(document.id)
        runCatching {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(document.uri),
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        }
    }

    private fun queryMetadata(resolver: ContentResolver, uri: Uri): Pair<String, Long?> {
        var name = uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() } ?: "Imported document"
        var size: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) cursor.getString(nameIndex)?.takeIf { it.isNotBlank() }?.let { name = it }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex).takeIf { it >= 0L }?.let { size = it }
            }
        }
        return name to size
    }
}
