package dev.beehalf.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ConversationEntity::class, MessageEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class BeehalfDatabase : RoomDatabase() {
    abstract fun beehalfDao(): BeehalfDao

    companion object {
        @Volatile private var instance: BeehalfDatabase? = null

        fun getInstance(context: Context): BeehalfDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    BeehalfDatabase::class.java,
                    "beehalf-local.db"
                ).build().also { instance = it }
            }
    }
}
