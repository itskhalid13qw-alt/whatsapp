package com.messagestudio.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "recipients")
data class RecipientEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = ""
)

/** One saved message. (recipientId, dedupeKey) is unique, which is what removes duplicates. */
@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = RecipientEntity::class,
            parentColumns = ["id"],
            childColumns = ["recipientId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["recipientId", "dedupeKey"], unique = true)]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientId: Long,
    val text: String,
    val dedupeKey: String,
    /** "AI", "LOCAL" or "MANUAL" */
    val source: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "templates")
data class TemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val instruction: String,
    val language: String,
    val tone: String,
    val category: String,
    val length: String,
    val emoji: String,
    val customInstructions: String,
    val body: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "usage")
data class UsageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    /** GENERATE, IMPROVE, TEMPLATE or CHAT */
    val kind: String,
    val success: Boolean,
    val messages: Int = 0,
    val inputTokens: Long = 0,
    val outputTokens: Long = 0,
    val error: String = ""
)

data class UsageTotals(
    val requests: Int,
    val failures: Int,
    val inputTokens: Long,
    val outputTokens: Long
)
