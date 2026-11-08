package dev.beehalf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, MemoryEntity::class, DocumentEntity::class, ActionProposalEntity::class],
    version = 3,
    exportSchema = false
)
abstract class BeehalfDatabase : RoomDatabase() {
    abstract fun beehalfDao(): BeehalfDao

    companion object {
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS documents (
                        id TEXT NOT NULL,
                        uri TEXT NOT NULL,
                        displayName TEXT NOT NULL,
                        mimeType TEXT NOT NULL,
                        sizeBytes INTEGER,
                        importedAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_documents_uri ON documents(uri)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_documents_importedAt ON documents(importedAt)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS action_proposals (
                        id TEXT NOT NULL,
                        actionType TEXT NOT NULL,
                        target TEXT NOT NULL,
                        detail TEXT NOT NULL,
                        summary TEXT NOT NULL,
                        status TEXT NOT NULL,
                        createdAt INTEGER NOT NULL,
                        expiresAt INTEGER NOT NULL,
                        decidedAt INTEGER,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_action_proposals_status ON action_proposals(status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_action_proposals_expiresAt ON action_proposals(expiresAt)")
            }
        }

        @Volatile private var instance: BeehalfDatabase? = null

        fun getInstance(context: Context): BeehalfDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BeehalfDatabase::class.java,
                    "beehalf-local.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
