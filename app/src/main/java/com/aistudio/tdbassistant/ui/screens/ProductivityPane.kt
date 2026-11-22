package com.aistudio.tdbassistant.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.data.local.ActivityLogEntity
import com.aistudio.tdbassistant.data.local.DailyGoalEntity
import com.aistudio.tdbassistant.data.local.GoalDomain
import com.aistudio.tdbassistant.platform.UsageSummaryReport
import com.aistudio.tdbassistant.ui.FocusTimerState
import java.util.Locale

@Composable
fun ProductivityPane(
    productivityScore: Int,
    focusTimer: FocusTimerState,
    goals: List<DailyGoalEntity>,
    activityLogs: List<ActivityLogEntity>,
    usageReport: UsageSummaryReport,
    usageDays: Int,
    onStartFocus: (Int, Int, DailyGoalEntity?) -> Unit,
    onPauseResumeFocus: () -> Unit,
    onResetFocus: () -> Unit,
    onAddGoal: (String, GoalDomain, Int, Int?, Int?) -> Unit,
    onToggleGoal: (DailyGoalEntity) -> Unit,
    onDeleteGoal: (String) -> Unit,
    onAddActivityLog: (String, GoalDomain, Int, String) -> Unit,
    onDeleteActivityLog: (String) -> Unit,
    onRefreshUsage: (Int) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onAskCoachAboutUsage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddGoal by remember { mutableStateOf(false) }
    var showAddLog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Productivity Score + Focus Timer Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "প্রোডাক্টিভিটি স্কোর",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "দৈনিক লক্ষ্য ও ফোকাস সেশনের ওপর ভিত্তি করে",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$productivityScore / 100",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { (productivityScore / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )

                    // Pomodoro Focus / Break Timer
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (focusTimer.isBreak) "বিরতি চলছে (Break)" else "ফোকাস টাইমার",
                                        style = MaterialTheme.typography.labelLarge
                                    )
                                }
                                val mins = focusTimer.remainingSeconds / 60
                                val secs = focusTimer.remainingSeconds % 60
                                Text(
                                    text = String.format(Locale.US, "%02d:%02d", mins, secs),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            focusTimer.linkedGoalTitle?.let { linked ->
                                Text(
                                    text = "লক্ষ্য: $linked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            LinearProgressIndicator(
                                progress = { focusTimer.progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (focusTimer.isRunning) onPauseResumeFocus()
                                        else if (focusTimer.remainingSeconds < focusTimer.totalSeconds) onPauseResumeFocus()
                                        else onStartFocus(25, 5, null)
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("focus_timer_toggle")
                                ) {
                                    Icon(
                                        imageVector = if (focusTimer.isRunning) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (focusTimer.isRunning) "Pause" else "Start 25m")
                                }
                                OutlinedButton(
                                    onClick = { onStartFocus(45, 10, null) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Deep 45m")
                                }
                                IconButton(onClick = onResetFocus) {
                                    Icon(Icons.Rounded.Replay, contentDescription = "Reset timer")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Daily Goals Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "দৈনিক লক্ষ্য (Goals)", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = { showAddGoal = true },
                            modifier = Modifier.testTag("add_goal_button")
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("লক্ষ্য যোগ")
                        }
                    }

                    if (goals.isEmpty()) {
                        Text(
                            text = "পড়াশোনা, কাজ বা ব্যবসার জন্য দৈনিক লক্ষ্য যোগ করুন ও রিমাইন্ডার সেট করুন।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        goals.forEach { goal ->
                            val domain = GoalDomain.fromId(goal.domain)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(onClick = { onToggleGoal(goal) }) {
                                        Icon(
                                            imageVector = if (goal.isCompleted) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = "Toggle goal",
                                            tint = if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = goal.title,
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                                        )
                                        val remText = if (goal.reminderHour != null && goal.reminderMinute != null) {
                                            " · রিমাইন্ডার ${String.format(Locale.US, "%02d:%02d", goal.reminderHour, goal.reminderMinute)}"
                                        } else ""
                                        Text(
                                            text = "${domain.labelBn} · ${goal.completedMinutes}/${goal.targetMinutes} মি.$remText",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(onClick = { onStartFocus(25, 5, goal) }) {
                                        Text("Focus")
                                    }
                                    IconButton(onClick = { onDeleteGoal(goal.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete goal")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Manual Activity Logs Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "ব্যক্তিগত অ্যাক্টিভিটি লগ", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(onClick = { showAddLog = true }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("লগ করুন")
                        }
                    }
                    if (activityLogs.isEmpty()) {
                        Text(
                            text = "পড়াশোনা, কাজ বা ফোকাস সেশনের সময় এখানে সংরক্ষিত থাকে।",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        activityLogs.take(6).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = log.title, style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = "${GoalDomain.fromId(log.domain).labelBn} · ${log.durationMinutes} মিনিট",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(onClick = { onDeleteActivityLog(log.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete log")
                                }
                            }
                        }
                    }
                }
            }
        }

        // UsageStatsManager Daily / Weekly / Monthly Monitor
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccessTime, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "ফোন ব্যবহার বিশ্লেষণ", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(
                            onClick = { onRefreshUsage(usageDays) },
                            modifier = Modifier.testTag("refresh_usage_button")
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("রিফ্রেশ")
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1 to "২৪ ঘণ্টা", 7 to "৭ দিন", 30 to "৩০ দিন").forEach { (days, label) ->
                            FilterChip(
                                selected = usageDays == days,
                                onClick = { onRefreshUsage(days) },
                                label = { Text(label) }
                            )
                        }
                    }

                    if (!usageReport.hasPermission) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = "ঐচ্ছিক Usage Access অনুমতি প্রয়োজন", style = MaterialTheme.typography.labelLarge)
                                }
                                Text(
                                    text = "এটি কেবল কোন অ্যাপ কত মিনিট চলেছে তা দেখায়—কোনো মেসেজ বা পাসওয়ার্ড পড়ে না।",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onOpenUsageSettings,
                                    modifier = Modifier.testTag("grant_usage_access_button")
                                ) {
                                    Text("Usage Access সেটিংস খুলুন")
                                }
                            }
                        }
                    } else {
                        Text(
                            text = "মোট আনুমানিক সময়: ${usageReport.totalMinutes} মিনিট",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (usageReport.totalMinutes >= 180) {
                            Text(
                                text = "পরামর্শ: দীর্ঘ সময় স্ক্রিনে ছিলেন—চোখ ও মনের বিশ্রামের জন্য ৫-১০ মিনিট বিরতি নিন।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        if (usageReport.topApps.isEmpty()) {
                            Text(
                                text = "এই সময়ের মধ্যে উল্লেখযোগ্য অ্যাপ ব্যবহার পাওয়া যায়নি।",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            val maxMins = usageReport.topApps.maxOf { it.minutes }.coerceAtLeast(1L)
                            usageReport.topApps.forEach { app ->
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = app.appName, style = MaterialTheme.typography.bodyMedium)
                                        Text(
                                            text = "${app.minutes} মিনিট",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    LinearProgressIndicator(
                                        progress = { (app.minutes.toFloat() / maxMins.toFloat()).coerceIn(0f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onAskCoachAboutUsage(
                                        "আমার সাম্প্রতিক ফোন ব্যবহারের সারসংক্ষেপ দেখে বাংলায় একটি বাস্তবসম্মত প্রোডাক্টিভিটি ও সুস্থতার রিপোর্ট দাও:\n${usageReport.summaryPromptText}"
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("ask_usage_coach_button")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("TDB থেকে দৈনিক রিপোর্ট ও পরামর্শ নিন")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = { showAddGoal = false },
            onSave = { title, domain, mins, remH, remM ->
                onAddGoal(title, domain, mins, remH, remM)
                showAddGoal = false
            }
        )
    }

    if (showAddLog) {
        AddActivityLogDialog(
            onDismiss = { showAddLog = false },
            onSave = { title, domain, mins, note ->
                onAddActivityLog(title, domain, mins, note)
                showAddLog = false
            }
        )
    }
}

@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSave: (String, GoalDomain, Int, Int?, Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf(GoalDomain.STUDY) }
    var targetMins by remember { mutableStateOf("60") }
    var reminderText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("নতুন দৈনিক লক্ষ্য") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GoalDomain.entries) { d ->
                        FilterChip(
                            selected = domain == d,
                            onClick = { domain = d },
                            label = { Text(d.labelBn) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("লক্ষ্যের নাম") },
                    placeholder = { Text("যেমন: Kotlin অনুশীলন") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = targetMins,
                    onValueChange = { targetMins = it.filter { ch -> ch.isDigit() } },
                    label = { Text("লক্ষ্য সময় (মিনিট)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reminderText,
                    onValueChange = { reminderText = it },
                    label = { Text("রিমাইন্ডার সময় (ঐচ্ছিক, HH:mm)") },
                    placeholder = { Text("যেমন: 20:30") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val mins = targetMins.toIntOrNull()?.coerceIn(5, 600) ?: 60
                    val parts = reminderText.trim().split(":")
                    val rh = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 0..23 }
                    val rm = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 0..59 }
                    onSave(title, domain, mins, if (rh != null && rm != null) rh else null, if (rh != null && rm != null) rm else null)
                },
                enabled = title.isNotBlank()
            ) { Text("সংরক্ষণ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
private fun AddActivityLogDialog(
    onDismiss: () -> Unit,
    onSave: (String, GoalDomain, Int, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var domain by remember { mutableStateOf(GoalDomain.WORK) }
    var minutes by remember { mutableIntStateOf(30) }
    var note by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("অ্যাক্টিভিটি লগ করুন") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(GoalDomain.entries) { d ->
                        FilterChip(
                            selected = domain == d,
                            onClick = { domain = d },
                            label = { Text(d.labelBn) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("কী কাজ করেছেন?") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(15, 30, 60, 90).forEach { m ->
                        FilterChip(
                            selected = minutes == m,
                            onClick = { minutes = m },
                            label = { Text("${m}m") }
                        )
                    }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("নোট (ঐচ্ছিক)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(title, domain, minutes, note) },
                enabled = title.isNotBlank()
            ) { Text("যোগ করুন") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}
