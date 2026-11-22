package com.aistudio.tdbassistant.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AccessibilityNew
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aistudio.tdbassistant.BuildConfig
import com.aistudio.tdbassistant.data.ai.GeminiDiagnosticResult
import com.aistudio.tdbassistant.data.preferences.AccentPalette
import com.aistudio.tdbassistant.data.preferences.AiEngineMode
import com.aistudio.tdbassistant.data.preferences.AiResponseStyle
import com.aistudio.tdbassistant.data.preferences.MotionQuality
import com.aistudio.tdbassistant.data.preferences.TdbLanguage
import com.aistudio.tdbassistant.data.preferences.TdbModelOption
import com.aistudio.tdbassistant.data.preferences.TdbSettings
import com.aistudio.tdbassistant.data.preferences.ThemePreference
import com.aistudio.tdbassistant.data.preferences.VoiceStyle
import com.aistudio.tdbassistant.platform.TtsEngineInfo
import com.aistudio.tdbassistant.ui.components.ApiSettingsCard
import java.util.Locale

@Composable
fun SettingsScreen(
    settings: TdbSettings,
    hasSavedApiKey: Boolean,
    lastVerifiedModel: String,
    apiDiagnostic: GeminiDiagnosticResult?,
    isTestingApi: Boolean,
    wakeWordRunning: Boolean,
    accessibilityConnected: Boolean,
    ttsEngines: List<TtsEngineInfo>,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onSelectModel: (TdbModelOption) -> Unit,
    onSelectResponseStyle: (AiResponseStyle) -> Unit,
    onSaveAndTestApiKey: (String) -> Unit,
    onTestSavedApiConnection: () -> Unit,
    onRemoveSavedApiKey: () -> Unit,
    onSelectLanguage: (TdbLanguage) -> Unit,
    onSelectVoiceStyle: (VoiceStyle) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onVoicePitchChange: (Float) -> Unit,
    onSpeechVolumeChange: (Float) -> Unit,
    onSelectTtsEngine: (String) -> Unit,
    onTestVoice: () -> Unit,
    onToggleSpokenReplies: (Boolean) -> Unit,
    onToggleWakeWord: (Boolean) -> Unit,
    onSelectMotionQuality: (MotionQuality) -> Unit,
    onToggleAutoMemory: (Boolean) -> Unit,
    onSelectTheme: (ThemePreference) -> Unit,
    onSelectAccentPalette: (AccentPalette) -> Unit,
    onClearHistory: () -> Unit,
    onExportMemories: () -> Unit,
    onOpenAndroidAppSettings: () -> Unit,
    onOpenUsageAccessSettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenNotificationListenerSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var confirmClearDialog by remember { mutableStateOf(false) }

    val wakePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val audioGranted = results[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (audioGranted) {
            onToggleWakeWord(true)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    text = "স্মার্ট সেটিংস ড্যাশবোর্ড",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "API, ভয়েস, Wake Word, অটোমেশন ও গোপনীয়তা নিয়ন্ত্রণ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Dedicated Gemini API Configuration + Model Selector
        item {
            ApiSettingsCard(
                settings = settings,
                hasSavedApiKey = hasSavedApiKey,
                lastVerifiedModel = lastVerifiedModel,
                apiDiagnostic = apiDiagnostic,
                isTestingApi = isTestingApi,
                onSelectEngineMode = onSelectEngineMode,
                onSelectModel = onSelectModel,
                onSelectResponseStyle = onSelectResponseStyle,
                onSaveAndTestApiKey = onSaveAndTestApiKey,
                onTestSavedApiConnection = onTestSavedApiConnection,
                onRemoveSavedApiKey = onRemoveSavedApiKey
            )
        }

        // Always-Available Wake Word Card
        item {
            SettingsCard(
                title = "Always-Available ভয়েস অ্যাসিস্ট্যান্ট",
                icon = Icons.Rounded.Mic
            ) {
                ToggleRow(
                    title = "\"Hey TDB\" / \"হ্যালো টিডিবি\" Wake Word",
                    subtitle = if (wakeWordRunning) {
                        "সক্রিয়: ফোরগ্রাউন্ড সার্ভিস মাইক্রোফোন শুনছে (নোটিফিকেশন থেকে যেকোনো সময় Stop করুন)"
                    } else {
                        "বন্ধ আছে: চালু করলে \"Hey TDB\" বা \"হ্যালো টিডিবি\" বললেই TDB সাড়া দেবে"
                    },
                    checked = wakeWordRunning,
                    tag = "toggle_wake_word",
                    onCheckedChange = { enable ->
                        if (!enable) {
                            onToggleWakeWord(false)
                        } else {
                            val hasAudio = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            val needsNotif = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.POST_NOTIFICATIONS
                                ) != PackageManager.PERMISSION_GRANTED

                            if (hasAudio && !needsNotif) {
                                onToggleWakeWord(true)
                            } else {
                                val reqs = buildList {
                                    add(Manifest.permission.RECORD_AUDIO)
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }.toTypedArray()
                                wakePermissionLauncher.launch(reqs)
                            }
                        }
                    }
                )
            }
        }

        // Language & Professional Male / Female Voices
        item {
            SettingsCard(
                title = "ভাষা ও পেশাদার কণ্ঠস্বর (Male / Female TTS)",
                icon = Icons.Rounded.Language
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "প্রধান ভাষা", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(TdbLanguage.entries) { lang ->
                            FilterChip(
                                selected = settings.language == lang,
                                onClick = { onSelectLanguage(lang) },
                                label = { Text(lang.label) }
                            )
                        }
                    }

                    Text(text = "কণ্ঠের ধরন", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoiceStyle.entries.forEach { style ->
                            FilterChip(
                                selected = settings.voiceStyle == style,
                                onClick = { onSelectVoiceStyle(style) },
                                label = { Text(style.label) }
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "কথার গতি (Speed): %.2fx", settings.voiceSpeed),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = onVoiceSpeedChange,
                        valueRange = 0.6f..1.6f
                    )

                    Text(
                        text = String.format(Locale.US, "স্বর (Pitch): %.2fx", settings.voicePitch),
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = settings.voicePitch,
                        onValueChange = onVoicePitchChange,
                        valueRange = 0.6f..1.5f
                    )

                    Text(
                        text = "ভলিউম: ${(settings.speechVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Slider(
                        value = settings.speechVolume,
                        onValueChange = onSpeechVolumeChange,
                        valueRange = 0.2f..1.0f
                    )

                    if (ttsEngines.isNotEmpty()) {
                        Text(text = "TTS ইঞ্জিন নির্বাচন", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item {
                                FilterChip(
                                    selected = settings.ttsEnginePackage.isBlank(),
                                    onClick = { onSelectTtsEngine("") },
                                    label = { Text("System Default") }
                                )
                            }
                            items(ttsEngines) { engine ->
                                FilterChip(
                                    selected = settings.ttsEnginePackage == engine.packageName,
                                    onClick = { onSelectTtsEngine(engine.packageName) },
                                    label = { Text(engine.label) }
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onTestVoice,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("test_voice_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Voice · কণ্ঠ পরীক্ষা করুন")
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ToggleRow(
                    title = "উত্তর স্বয়ংক্রিয়ভাবে পড়ে শোনাও",
                    subtitle = "প্রতিটি AI উত্তরের পর Android TTS দিয়ে কণ্ঠে শোনাবে",
                    checked = settings.spokenReplies,
                    tag = "toggle_spoken_replies",
                    onCheckedChange = onToggleSpokenReplies
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                ToggleRow(
                    title = "স্বয়ংক্রিয় স্মৃতি সংরক্ষণ",
                    subtitle = "গুরুত্বপূর্ণ ব্যক্তিগত তথ্য স্বয়ংক্রিয়ভাবে স্মৃতিতে যোগ করবে",
                    checked = settings.autoMemoryExtraction,
                    tag = "toggle_auto_memory",
                    onCheckedChange = onToggleAutoMemory
                )
            }
        }

        // Theme, Accent Palette & Orb Animation
        item {
            SettingsCard(
                title = "থিম, রং ও Orb অ্যানিমেশন",
                icon = Icons.Rounded.Palette
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "অ্যাপ থিম", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { pref ->
                            FilterChip(
                                selected = settings.themePreference == pref,
                                onClick = { onSelectTheme(pref) },
                                label = { Text(pref.label) }
                            )
                        }
                    }

                    Text(text = "অ্যাকসেন্ট প্যালেট", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(AccentPalette.entries) { palette ->
                            FilterChip(
                                selected = settings.accentPalette == palette,
                                onClick = { onSelectAccentPalette(palette) },
                                label = { Text(palette.label) }
                            )
                        }
                    }

                    Text(text = "Orb ও চরিত্রের মোশন কোয়ালিটি", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(MotionQuality.entries) { q ->
                            FilterChip(
                                selected = settings.motionQuality == q,
                                onClick = { onSelectMotionQuality(q) },
                                label = { Text(q.label) }
                            )
                        }
                    }
                }
            }
        }

        // Phone Automation & Permissions
        item {
            SettingsCard(
                title = "ফোন অটোমেশন ও গোপনীয়তা",
                icon = Icons.Rounded.AccessibilityNew
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (accessibilityConnected) {
                            "TDB Accessibility Service চালু আছে (Back, Home, Scroll ও দৃশ্যমান YouTube কন্ট্রোল সক্রিয়)।"
                        } else {
                            "TDB Accessibility Service বন্ধ আছে। স্ক্রল, Back বা YouTube বাটন নিয়ন্ত্রণের জন্য চালু করতে পারেন।"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenAccessibilitySettings,
                            modifier = Modifier.weight(1f)
                        ) { Text("Accessibility") }
                        OutlinedButton(
                            onClick = onOpenUsageAccessSettings,
                            modifier = Modifier.weight(1f)
                        ) { Text("Usage Access") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onOpenNotificationListenerSettings,
                            modifier = Modifier.weight(1f)
                        ) { Text("Notification Access") }
                        OutlinedButton(
                            onClick = onOpenAndroidAppSettings,
                            modifier = Modifier.weight(1f)
                        ) { Text("App Permissions") }
                    }
                }
            }
        }

        // Data Management Card
        item {
            SettingsCard(
                title = "ডেটা ও ব্যাকআপ",
                icon = Icons.Rounded.Lock
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onExportMemories,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("স্মৃতি এক্সপোর্ট")
                        }
                        OutlinedButton(
                            onClick = { confirmClearDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_history_button")
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ইতিহাস মুছুন")
                        }
                    }
                }
            }
        }

        // About TDB Card
        item {
            SettingsCard(
                title = "TDB সম্পর্কে",
                icon = Icons.Rounded.Info
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "TDB Personal AI Assistant · v${BuildConfig.VERSION_NAME}",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "নির্মাতা ও প্রতিষ্ঠাতা: Deep Barmon (দীপ বর্মন)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "অফিশিয়াল ব্র্যান্ড: TDB · YouTube: Deep Barmon Bhai Vlog",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (confirmClearDialog) {
        AlertDialog(
            onDismissRequest = { confirmClearDialog = false },
            title = { Text("কথোপকথনের ইতিহাস মুছবেন?") },
            text = { Text("এই ডিভাইসে সংরক্ষিত সকল চ্যাট বার্তা মুছে যাবে। আপনার স্মৃতি ভাণ্ডার অক্ষত থাকবে।") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        confirmClearDialog = false
                    }
                ) {
                    Text("মুছে ফেলুন", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClearDialog = false }) {
                    Text("বাতিল")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            content()
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
