package com.messagestudio.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [RecipientEntity::class, MessageEntity::class, TemplateEntity::class, UsageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipients(): RecipientDao
    abstract fun messages(): MessageDao
    abstract fun templates(): TemplateDao
    abstract fun usage(): UsageDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "message_studio.db"
                ).build().also { instance = it }
            }
    }
}
