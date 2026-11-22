package com.aistudio.tdbassistant.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.data.local.ActivityLogEntity
import com.aistudio.tdbassistant.data.local.DailyGoalEntity
import com.aistudio.tdbassistant.data.local.GoalDomain
import com.aistudio.tdbassistant.domain.PhoneSystemAction
import com.aistudio.tdbassistant.domain.YouTubeAction
import com.aistudio.tdbassistant.platform.DeviceActionResult
import com.aistudio.tdbassistant.platform.InstalledAppItem
import com.aistudio.tdbassistant.platform.StorageSummary
import com.aistudio.tdbassistant.platform.UsageSummaryReport
import com.aistudio.tdbassistant.ui.FocusTimerState

@Composable
fun WellnessActionsScreen(
    productivityScore: Int,
    focusTimer: FocusTimerState,
    goals: List<DailyGoalEntity>,
    activityLogs: List<ActivityLogEntity>,
    usageReport: UsageSummaryReport,
    usageDays: Int,
    accessibilityConnected: Boolean,
    installedApps: List<InstalledAppItem>,
    storageSummary: StorageSummary,
    webSummaryResult: String?,
    isSummarizingWeb: Boolean,
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
    onOpenAccessibilitySettings: () -> Unit,
    onAskCoachAboutUsage: (String) -> Unit,
    onPhoneControl: (PhoneSystemAction) -> DeviceActionResult,
    onYouTubeControl: (YouTubeAction) -> DeviceActionResult,
    onOpenApp: (String) -> DeviceActionResult,
    onSearchYouTube: (String) -> DeviceActionResult,
    onSearchWeb: (String) -> DeviceActionResult,
    onSummarizeWebUrl: (String) -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
    onPrepareWhatsApp: (String, String) -> DeviceActionResult,
    onPrepareSms: (String, String) -> DeviceActionResult,
    onDialPhone: (String?) -> DeviceActionResult,
    onOpenDocumentUri: (Uri) -> Unit,
    onShareDocumentUri: (Uri) -> Unit,
    onRefreshAppsAndStorage: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSegment by rememberSaveable { mutableIntStateOf(0) }

    Column(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 10.dp)) {
            Text(
                text = "প্রোডাক্টিভিটি ও স্মার্ট টুলস",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSegment == 0,
                    onClick = { selectedSegment = 0 },
                    modifier = Modifier.testTag("segment_productivity"),
                    label = { Text("ফোকাস ও ব্যবহার") }
                )
                FilterChip(
                    selected = selectedSegment == 1,
                    onClick = { selectedSegment = 1 },
                    modifier = Modifier.testTag("segment_tools"),
                    label = { Text("কন্ট্রোল, ওয়েব ও ফাইল") }
                )
            }
        }

        if (selectedSegment == 0) {
            ProductivityPane(
                productivityScore = productivityScore,
                focusTimer = focusTimer,
                goals = goals,
                activityLogs = activityLogs,
                usageReport = usageReport,
                usageDays = usageDays,
                onStartFocus = onStartFocus,
                onPauseResumeFocus = onPauseResumeFocus,
                onResetFocus = onResetFocus,
                onAddGoal = onAddGoal,
                onToggleGoal = onToggleGoal,
                onDeleteGoal = onDeleteGoal,
                onAddActivityLog = onAddActivityLog,
                onDeleteActivityLog = onDeleteActivityLog,
                onRefreshUsage = onRefreshUsage,
                onOpenUsageSettings = onOpenUsageSettings,
                onAskCoachAboutUsage = onAskCoachAboutUsage,
                modifier = Modifier.weight(1f)
            )
        } else {
            AutomationToolsPane(
                accessibilityConnected = accessibilityConnected,
                installedApps = installedApps,
                storageSummary = storageSummary,
                webSummaryResult = webSummaryResult,
                isSummarizingWeb = isSummarizingWeb,
                onOpenAccessibilitySettings = onOpenAccessibilitySettings,
                onPhoneControl = onPhoneControl,
                onYouTubeControl = onYouTubeControl,
                onOpenApp = onOpenApp,
                onSearchYouTube = onSearchYouTube,
                onSearchWeb = onSearchWeb,
                onSummarizeWebUrl = onSummarizeWebUrl,
                onOpenUrlInBrowser = onOpenUrlInBrowser,
                onPrepareWhatsApp = onPrepareWhatsApp,
                onPrepareSms = onPrepareSms,
                onDialPhone = onDialPhone,
                onOpenDocumentUri = onOpenDocumentUri,
                onShareDocumentUri = onShareDocumentUri,
                onRefreshAppsAndStorage = onRefreshAppsAndStorage,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
