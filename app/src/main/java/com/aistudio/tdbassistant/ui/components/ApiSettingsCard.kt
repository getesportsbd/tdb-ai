package com.aistudio.tdbassistant.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.aistudio.tdbassistant.data.ai.GeminiDiagnosticResult
import com.aistudio.tdbassistant.data.preferences.AiEngineMode
import com.aistudio.tdbassistant.data.preferences.AiResponseStyle
import com.aistudio.tdbassistant.data.preferences.TdbModelOption
import com.aistudio.tdbassistant.data.preferences.TdbSettings

@Composable
fun ApiSettingsCard(
    settings: TdbSettings,
    hasSavedApiKey: Boolean,
    lastVerifiedModel: String,
    apiDiagnostic: GeminiDiagnosticResult?,
    isTestingApi: Boolean,
    onSelectEngineMode: (AiEngineMode) -> Unit,
    onSelectModel: (TdbModelOption) -> Unit,
    onSelectResponseStyle: (AiResponseStyle) -> Unit,
    onSaveAndTestApiKey: (String) -> Unit,
    onTestSavedApiConnection: () -> Unit,
    onRemoveSavedApiKey: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showApiConfigDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AI ব্রেইন ও Gemini API কনফিগারেশন",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedButton(
                    onClick = { showApiConfigDialog = true },
                    modifier = Modifier.testTag("open_api_settings_button")
                ) {
                    Icon(Icons.Rounded.Key, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("API Settings")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "সংযোগের ধরন", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AiEngineMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.aiEngineMode == mode,
                            onClick = { onSelectEngineMode(mode) },
                            label = {
                                Text(
                                    if (mode == AiEngineMode.FIREBASE_AI_LOGIC) "Built-in Firebase"
                                    else "Personal API Key"
                                )
                            }
                        )
                    }
                }

                // Connection Status Badge
                val statusText = when {
                    settings.aiEngineMode == AiEngineMode.FIREBASE_AI_LOGIC ->
                        "সক্রিয়: Firebase AI Logic (App Check সুরক্ষিত · আলাদা Key প্রয়োজন নেই)"
                    hasSavedApiKey && lastVerifiedModel.isNotBlank() ->
                        "সংরক্ষিত (Android Keystore): $lastVerifiedModel যাচাইকৃত"
                    hasSavedApiKey ->
                        "API Key সংরক্ষিত আছে (Test Connection চাপুন)"
                    else ->
                        "Personal API Key মোড নির্বাচিত কিন্তু কোনো Key সংরক্ষিত নেই"
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp)
                    )
                }

                if (apiDiagnostic != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (apiDiagnostic.ok) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "[${apiDiagnostic.code}] ${apiDiagnostic.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (apiDiagnostic.ok) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp)
                        )
                    }
                }

                Text(text = "উত্তরের ধরন (Response Style)", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AiResponseStyle.entries) { style ->
                        FilterChip(
                            selected = settings.responseStyle == style,
                            onClick = { onSelectResponseStyle(style) },
                            label = { Text(style.labelBn) }
                        )
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            TdbModelOption.entries.forEachIndexed { idx, option ->
                if (idx > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                val selected = settings.model == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectModel(option) }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("model_${option.modelId}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = option.summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = option.displayName,
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showApiConfigDialog) {
        ApiKeyConfigDialog(
            settings = settings,
            hasSavedApiKey = hasSavedApiKey,
            apiDiagnostic = apiDiagnostic,
            isTesting = isTestingApi,
            onDismiss = { showApiConfigDialog = false },
            onSelectModel = onSelectModel,
            onSaveAndTest = onSaveAndTestApiKey,
            onTestSaved = onTestSavedApiConnection,
            onRemove = onRemoveSavedApiKey
        )
    }
}

@Composable
private fun ApiKeyConfigDialog(
    settings: TdbSettings,
    hasSavedApiKey: Boolean,
    apiDiagnostic: GeminiDiagnosticResult?,
    isTesting: Boolean,
    onDismiss: () -> Unit,
    onSelectModel: (TdbModelOption) -> Unit,
    onSaveAndTest: (String) -> Unit,
    onTestSaved: () -> Unit,
    onRemove: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gemini API Configuration") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "আপনার ব্যক্তিগত Gemini API Key Android Keystore (AES-256-GCM) দিয়ে এনক্রিপ্টেড হয়ে এই ডিভাইসে থাকে।",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text(if (hasSavedApiKey) "নতুন Key দিয়ে আপডেট করুন" else "Gemini API Key") },
                    placeholder = { Text("YOUR_GEMINI_API_KEY_HERE") },
                    singleLine = true,
                    visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showKey = !showKey }) {
                            Icon(
                                imageVector = if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = "Toggle key visibility"
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("api_key_input")
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(TdbModelOption.entries) { model ->
                        FilterChip(
                            selected = settings.model == model,
                            onClick = { onSelectModel(model) },
                            label = { Text(model.displayName) }
                        )
                    }
                }

                if (apiDiagnostic != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (apiDiagnostic.ok) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = "[${apiDiagnostic.code}] ${apiDiagnostic.message}",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (apiDiagnostic.ok) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (hasSavedApiKey) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onTestSaved,
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isTesting) "Testing…" else "Test Saved Key")
                        }
                        OutlinedButton(
                            onClick = onRemove,
                            enabled = !isTesting,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("remove_api_key_button")
                        ) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSaveAndTest(apiKeyInput) },
                enabled = apiKeyInput.isNotBlank() && !isTesting,
                modifier = Modifier.testTag("save_test_api_key_button")
            ) {
                Text(if (isTesting) "যাচাই হচ্ছে…" else if (hasSavedApiKey) "Update & Test" else "Save & Test")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("বন্ধ করুন")
            }
        }
    )
}
