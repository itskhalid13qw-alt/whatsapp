package com.messagestudio.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipientDao {
    @Insert
    suspend fun insert(recipient: RecipientEntity): Long

    @Query("SELECT * FROM recipients ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecipientEntity>>

    @Delete
    suspend fun delete(recipient: RecipientEntity)
}

@Dao
interface MessageDao {
    /** Returns -1 when the (recipient, text) pair already exists, so duplicates are never saved. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(message: MessageEntity): Long

    @Query("SELECT * FROM messages WHERE recipientId = :recipientId ORDER BY id DESC")
    fun observeFor(recipientId: Long): Flow<List<MessageEntity>>

    @Query("SELECT dedupeKey FROM messages WHERE recipientId = :recipientId")
    suspend fun keysFor(recipientId: Long): List<String>

    @Query("UPDATE messages SET text = :text, dedupeKey = :key WHERE id = :id")
    suspend fun updateText(id: Long, text: String, key: String): Int

    @Query("DELETE FROM messages WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface TemplateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(template: TemplateEntity): Long

    @Query("SELECT * FROM templates ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<TemplateEntity>>

    @Query("DELETE FROM templates WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface UsageDao {
    @Insert
    suspend fun insert(usage: UsageEntity): Long

    @Query(
        "SELECT COUNT(*) AS requests, " +
            "COALESCE(SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END), 0) AS failures, " +
            "COALESCE(SUM(inputTokens), 0) AS inputTokens, " +
            "COALESCE(SUM(outputTokens), 0) AS outputTokens FROM usage"
    )
    fun totals(): Flow<UsageTotals>

    @Query("SELECT * FROM usage ORDER BY id DESC LIMIT :limit")
    fun recent(limit: Int): Flow<List<UsageEntity>>

    @Query("SELECT COALESCE(SUM(messages), 0) FROM usage WHERE kind = 'GENERATE' AND success = 1 AND timestamp >= :since")
    suspend fun generatedSince(since: Long): Int

    @Query("DELETE FROM usage")
    suspend fun clear()
}
