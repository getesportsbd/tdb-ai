package com.aistudio.tdbassistant.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class MemoryCategory(
    val id: String,
    val titleEn: String,
    val titleBn: String
) {
    IDENTITY("identity", "Identity Core", "পরিচয়"),
    PREFERENCE("preference", "Preferences", "পছন্দ"),
    GOAL("goal", "Personal Goals", "লক্ষ্য"),
    PROJECT("project", "Active Projects", "প্রজেক্ট"),
    RELATIONSHIP("relationship", "Relationships", "সম্পর্ক"),
    EMOTIONAL("emotional", "Milestones", "স্মরণীয় মুহূর্ত"),
    BEHAVIOR("behavior", "Habits", "অভ্যাস"),
    NOTE("note", "Saved Notes", "নোটস"),
    SUMMARY("summary", "Conversation Summaries", "সারাংশ"),
    DAILY_REPORT("daily_report", "Daily Reports", "দৈনিক রিপোর্ট"),
    TASK_HISTORY("task_history", "Task History", "কাজের ইতিহাস");

    companion object {
        fun fromId(raw: String): MemoryCategory =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: IDENTITY
    }
}

enum class GoalDomain(val id: String, val labelBn: String, val labelEn: String) {
    STUDY("study", "পড়াশোনা", "Study"),
    WORK("work", "কাজ", "Work"),
    BUSINESS("business", "ব্যবসা", "Business"),
    PERSONAL("personal", "ব্যক্তিগত", "Personal");

    companion object {
        fun fromId(raw: String): GoalDomain =
            entries.firstOrNull { it.id.equals(raw, ignoreCase = true) } ?: PERSONAL
    }
}

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val role: String,
    val text: String,
    val attachmentLabel: String? = null,
    val actionBadge: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "memories")
data class MemoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String,
    val text: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_goals")
data class DailyGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val domain: String = GoalDomain.STUDY.id,
    val targetMinutes: Int = 60,
    val completedMinutes: Int = 0,
    val isCompleted: Boolean = false,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "activity_logs")
data class ActivityLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val domain: String,
    val durationMinutes: Int,
    val note: String = "",
    val loggedAt: Long = System.currentTimeMillis()
)
