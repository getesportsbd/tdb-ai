package com.aistudio.tdbassistant.data.local

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class TdbLocalRepository(
    private val chatDao: ChatMessageDao,
    private val memoryDao: MemoryDao,
    private val goalDao: DailyGoalDao,
    private val activityDao: ActivityLogDao
) {
    val messages: Flow<List<ChatMessageEntity>> = chatDao.observeAll()
    val memories: Flow<List<MemoryEntity>> = memoryDao.observeAll()
    val goals: Flow<List<DailyGoalEntity>> = goalDao.observeAll()
    val activityLogs: Flow<List<ActivityLogEntity>> = activityDao.observeAll()

    suspend fun addMessage(
        role: String,
        text: String,
        attachmentLabel: String? = null,
        actionBadge: String? = null
    ) {
        chatDao.insert(
            ChatMessageEntity(
                role = role,
                text = text,
                attachmentLabel = attachmentLabel,
                actionBadge = actionBadge
            )
        )
    }

    suspend fun getRecentMessages(limit: Int = 20): List<ChatMessageEntity> {
        return chatDao.getRecent(limit).reversed()
    }

    suspend fun clearHistory() {
        chatDao.clearAll()
    }

    suspend fun getPromptMemories(limit: Int = 20): List<MemoryEntity> {
        return memoryDao.getTopMemories(limit)
    }

    suspend fun addMemory(category: MemoryCategory, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val now = System.currentTimeMillis()
        memoryDao.upsert(
            MemoryEntity(
                id = UUID.randomUUID().toString(),
                category = category.id,
                text = trimmed,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun editMemory(id: String, category: MemoryCategory, newText: String) {
        val trimmed = newText.trim()
        if (trimmed.isEmpty()) return
        memoryDao.updateMemory(id, category.id, trimmed, System.currentTimeMillis())
    }

    suspend fun deleteMemory(id: String) {
        memoryDao.deleteById(id)
    }

    suspend fun clearAllMemories() {
        memoryDao.clearAll()
    }

    suspend fun addGoal(
        title: String,
        domain: GoalDomain,
        targetMinutes: Int,
        reminderHour: Int? = null,
        reminderMinute: Int? = null
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        goalDao.upsert(
            DailyGoalEntity(
                title = trimmed,
                domain = domain.id,
                targetMinutes = targetMinutes.coerceIn(5, 600),
                reminderHour = reminderHour,
                reminderMinute = reminderMinute
            )
        )
    }

    suspend fun toggleGoalCompletion(goal: DailyGoalEntity) {
        goalDao.upsert(goal.copy(isCompleted = !goal.isCompleted))
    }

    suspend fun logGoalMinutes(goal: DailyGoalEntity, addMinutes: Int) {
        val updated = (goal.completedMinutes + addMinutes).coerceAtLeast(0)
        goalDao.upsert(
            goal.copy(
                completedMinutes = updated,
                isCompleted = updated >= goal.targetMinutes
            )
        )
    }

    suspend fun deleteGoal(id: String) {
        goalDao.deleteById(id)
    }

    suspend fun addActivityLog(
        title: String,
        domain: GoalDomain,
        durationMinutes: Int,
        note: String = ""
    ) {
        val trimmed = title.trim()
        if (trimmed.isEmpty()) return
        activityDao.insert(
            ActivityLogEntity(
                title = trimmed,
                domain = domain.id,
                durationMinutes = durationMinutes.coerceIn(1, 720),
                note = note.trim()
            )
        )
    }

    suspend fun deleteActivityLog(id: String) {
        activityDao.deleteById(id)
    }

    fun computeProductivityScore(
        goals: List<DailyGoalEntity>,
        logs: List<ActivityLogEntity>
    ): Int {
        if (goals.isEmpty() && logs.isEmpty()) return 0
        val now = System.currentTimeMillis()
        val weekCutoff = now - 7L * 24L * 60L * 60L * 1000L
        val recentFocusMinutes = logs.filter { it.loggedAt >= weekCutoff }.sumOf { it.durationMinutes }
        val goalRatio = if (goals.isEmpty()) 0.5f else {
            val done = goals.count { it.isCompleted }.toFloat()
            done / goals.size.toFloat()
        }
        val focusScore = (recentFocusMinutes.coerceAtMost(420) / 420f) * 50f
        val goalScore = goalRatio * 50f
        return (focusScore + goalScore).toInt().coerceIn(0, 100)
    }

    fun formatExportText(items: List<MemoryEntity>): String {
        if (items.isEmpty()) return "TDB Personal AI — No saved recollections yet."
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        return buildString {
            appendLine("TDB Personal AI Assistant v3.0 — Exported Memories (${items.size})")
            appendLine("=============================================================")
            items.forEach { item ->
                val cat = MemoryCategory.fromId(item.category)
                val date = formatter.format(Date(item.updatedAt))
                appendLine("[${cat.titleEn}] ${item.text} ($date)")
            }
        }
    }
}
