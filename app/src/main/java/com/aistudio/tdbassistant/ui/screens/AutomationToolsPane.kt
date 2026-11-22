package com.aistudio.tdbassistant.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Launch
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.domain.PhoneSystemAction
import com.aistudio.tdbassistant.domain.YouTubeAction
import com.aistudio.tdbassistant.platform.DeviceActionResult
import com.aistudio.tdbassistant.platform.InstalledAppItem
import com.aistudio.tdbassistant.platform.StorageSummary
import java.util.Locale

@Composable
fun AutomationToolsPane(
    accessibilityConnected: Boolean,
    installedApps: List<InstalledAppItem>,
    storageSummary: StorageSummary,
    webSummaryResult: String?,
    isSummarizingWeb: Boolean,
    onOpenAccessibilitySettings: () -> Unit,
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
    var searchQuery by remember { mutableStateOf("") }
    var webUrl by remember { mutableStateOf("") }
    var appFilter by remember { mutableStateOf("") }
    var msgChannel by remember { mutableIntStateOf(0) } // 0=WhatsApp, 1=SMS, 2=Call
    var recipient by remember { mutableStateOf("") }
    var msgBody by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var lastPickedUri by remember { mutableStateOf<Uri?>(null) }

    val filePickerOpen = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            lastPickedUri = uri
            onOpenDocumentUri(uri)
        }
    }

    val mapResult: (DeviceActionResult) -> Unit = { res ->
        feedback = when (res) {
            is DeviceActionResult.Success -> res.message
            is DeviceActionResult.Failure -> res.message
            is DeviceActionResult.NeedsContactsPermission ->
                "সংরক্ষিত নাম খুঁজতে চ্যাট ট্যাবে Contacts অনুমতি দিন বা সরাসরি নম্বর ব্যবহার করুন।"
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!feedback.isNullOrBlank()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = feedback.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    )
                }
            }
        }

        // Phone Control + Accessibility Service Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AccessibilityNew, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "স্মার্ট ফোন কন্ট্রোল", style = MaterialTheme.typography.titleMedium)
                        }
                        FilterChip(
                            selected = accessibilityConnected,
                            onClick = onOpenAccessibilitySettings,
                            label = { Text(if (accessibilityConnected) "Accessibility চালু" else "চালু করুন") }
                        )
                    }

                    val phoneButtons = listOf(
                        "Wi-Fi" to PhoneSystemAction.OPEN_WIFI_SETTINGS,
                        "Bluetooth" to PhoneSystemAction.OPEN_BLUETOOTH_SETTINGS,
                        "Settings" to PhoneSystemAction.OPEN_SYSTEM_SETTINGS,
                        "Notes" to PhoneSystemAction.OPEN_NOTES,
                        "Vol +" to PhoneSystemAction.VOLUME_UP,
                        "Vol -" to PhoneSystemAction.VOLUME_DOWN,
                        "Bright +" to PhoneSystemAction.BRIGHTNESS_UP,
                        "Bright -" to PhoneSystemAction.BRIGHTNESS_DOWN,
                        "Back" to PhoneSystemAction.NAVIGATE_BACK,
                        "Home" to PhoneSystemAction.CLOSE_CURRENT_APP,
                        "Scroll ↓" to PhoneSystemAction.SCROLL_DOWN,
                        "Scroll ↑" to PhoneSystemAction.SCROLL_UP
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(phoneButtons) { (label, act) ->
                            AssistChip(
                                onClick = { mapResult(onPhoneControl(act)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // YouTube Automation & Search Card
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PlayCircleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "YouTube অটোমেশন ও ওয়েব সার্চ", style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("YouTube বা Google-এ কী খুঁজবেন?") },
                        placeholder = { Text("যেমন: Deep Barmon Bhai Vlog বাংলা") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("quick_search_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { mapResult(onSearchYouTube(searchQuery)) },
                            enabled = searchQuery.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("youtube_search_button")
                        ) { Text("YouTube Search") }

                        OutlinedButton(
                            onClick = { mapResult(onSearchWeb(searchQuery)) },
                            enabled = searchQuery.isNotBlank(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("web_search_button")
                        ) { Text("Web Search") }
                    }

                    Text(
                        text = "YouTube মিডিয়া ও স্ক্রিন নিয়ন্ত্রণ",
                        style = MaterialTheme.typography.labelLarge
                    )
                    val ytControls = listOf(
                        "Play" to YouTubeAction.PLAY,
                        "Pause" to YouTubeAction.PAUSE,
                        "Next" to YouTubeAction.NEXT,
                        "Prev" to YouTubeAction.PREVIOUS,
                        "1st Video" to YouTubeAction.OPEN_FIRST_VIDEO,
                        "Fullscreen" to YouTubeAction.FULLSCREEN_ON,
                        "Mute" to YouTubeAction.MUTE,
                        "Unmute" to YouTubeAction.UNMUTE,
                        "Comments" to YouTubeAction.OPEN_COMMENTS,
                        "Desc" to YouTubeAction.OPEN_DESCRIPTION,
                        "2x" to YouTubeAction.SPEED_2X,
                        "1.5x" to YouTubeAction.SPEED_1_5X,
                        "0.5x" to YouTubeAction.SPEED_0_5X
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ytControls) { (label, act) ->
                            AssistChip(
                                onClick = { mapResult(onYouTubeControl(act)) },
                                label = { Text(label) }
                            )
                        }
                    }
                }
            }
        }

        // WhatsApp, SMS & Phone Call Composer
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (msgChannel == 2) Icons.Rounded.Call else Icons.AutoMirrored.Rounded.Chat,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "WhatsApp, SMS ও কল", style = MaterialTheme.typography.titleMedium)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("WhatsApp", "SMS", "Phone Call").forEachIndexed { idx, label ->
                            FilterChip(
                                selected = msgChannel == idx,
                                onClick = { msgChannel = idx },
                                label = { Text(label) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = recipient,
                        onValueChange = { recipient = it },
                        label = { Text("প্রাপকের নাম বা +880/+91 নম্বর") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wa_recipient_input")
                    )

                    if (msgChannel != 2) {
                        OutlinedTextField(
                            value = msgBody,
                            onValueChange = { msgBody = it },
                            label = { Text("বার্তা লিখুন") },
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wa_message_input")
                        )
                    }

                    Button(
                        onClick = {
                            val res = when (msgChannel) {
                                0 -> onPrepareWhatsApp(recipient, msgBody)
                                1 -> onPrepareSms(recipient, msgBody)
                                else -> onDialPhone(recipient.ifBlank { null })
                            }
                            mapResult(res)
                        },
                        enabled = if (msgChannel == 2) true else recipient.isNotBlank() && msgBody.isNotBlank(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("wa_prepare_button")
                    ) {
                        Text(
                            when (msgChannel) {
                                0 -> "WhatsApp খসড়া খুলুন"
                                1 -> "SMS অ্যাপে খুলুন"
                                else -> "ফোন ডায়ালার প্রস্তুত করুন"
                            }
                        )
                    }
                }
            }
        }

        // Web Page Summarizer
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ওয়েবপেজ সারাংশ (Gemini)", style = MaterialTheme.typography.titleMedium)
                    }
                    OutlinedTextField(
                        value = webUrl,
                        onValueChange = { webUrl = it },
                        label = { Text("ওয়েবসাইট URL লিখুন") },
                        placeholder = { Text("https://example.com/article") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onSummarizeWebUrl(webUrl) },
                            enabled = webUrl.isNotBlank() && !isSummarizingWeb,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isSummarizingWeb) "সারাংশ হচ্ছে…" else "সারাংশ করুন")
                        }
                        OutlinedButton(
                            onClick = { onOpenUrlInBrowser(webUrl) },
                            enabled = webUrl.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("ব্রাউজারে খুলুন")
                        }
                    }
                    if (!webSummaryResult.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = webSummaryResult,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }

        // Installed Apps Launcher & SAF Document/Storage Manager
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f))
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "অ্যাপ ও ফাইল ম্যানেজার", style = MaterialTheme.typography.titleMedium)
                        }
                        OutlinedButton(onClick = onRefreshAppsAndStorage) {
                            Text("রিফ্রেশ")
                        }
                    }

                    Text(
                        text = String.format(
                            Locale.US,
                            "স্টোরেজ: %.1f GB খালি / %.1f GB মোট (%d%% ব্যবহৃত)",
                            storageSummary.freeGb,
                            storageSummary.totalGb,
                            storageSummary.usedPercent
                        ),
                        style = MaterialTheme.typography.bodySmall
                    )
                    LinearProgressIndicator(
                        progress = { (storageSummary.usedPercent / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { filePickerOpen.launch(arrayOf("*/*")) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ফাইল খুলুন (SAF)")
                        }
                        if (lastPickedUri != null) {
                            OutlinedButton(
                                onClick = { lastPickedUri?.let(onShareDocumentUri) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("শেষ ফাইল শেয়ার")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = appFilter,
                        onValueChange = { appFilter = it },
                        label = { Text("ইনস্টল করা অ্যাপ খুঁজুন ও খুলুন") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_app_input")
                    )

                    val visibleApps = remember(installedApps, appFilter) {
                        val q = appFilter.trim()
                        val base = if (q.isEmpty()) installedApps else installedApps.filter {
                            it.label.contains(q, ignoreCase = true)
                        }
                        base.take(12)
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(visibleApps) { item ->
                            AssistChip(
                                onClick = { mapResult(onOpenApp(item.label)) },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Rounded.Launch, contentDescription = null, modifier = Modifier.size(14.dp))
                                },
                                label = { Text(item.label) }
                            )
                        }
                    }
                }
            }
        }
    }
}
