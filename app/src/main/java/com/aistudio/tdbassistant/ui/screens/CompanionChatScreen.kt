package com.aistudio.tdbassistant.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.aistudio.tdbassistant.R
import com.aistudio.tdbassistant.data.local.ChatMessageEntity
import com.aistudio.tdbassistant.data.preferences.AiEngineMode
import com.aistudio.tdbassistant.data.preferences.TdbSettings
import com.aistudio.tdbassistant.domain.CompanionPose
import com.aistudio.tdbassistant.domain.MessagingChannel
import com.aistudio.tdbassistant.domain.OrbState
import com.aistudio.tdbassistant.ui.PendingCallConfirmation
import com.aistudio.tdbassistant.ui.PendingContactsRequest
import com.aistudio.tdbassistant.ui.PendingMessageConfirmation
import com.aistudio.tdbassistant.ui.components.AiOrbVisualizer
import com.aistudio.tdbassistant.ui.components.CompanionStage

@Composable
fun CompanionChatScreen(
    settings: TdbSettings,
    orbState: OrbState,
    pose: CompanionPose,
    isFullBody: Boolean,
    useCharacterStage: Boolean,
    wakeWordRunning: Boolean,
    messages: List<ChatMessageEntity>,
    streamingReply: String?,
    isGenerating: Boolean,
    isListening: Boolean,
    partialDictation: String,
    isSpeaking: Boolean,
    composerText: String,
    pendingImage: Bitmap?,
    notice: String?,
    errorBanner: String?,
    needsContactsPermissionFor: PendingContactsRequest?,
    pendingMessageConfirmation: PendingMessageConfirmation?,
    pendingCallConfirmation: PendingCallConfirmation?,
    onComposerChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    onPoseSelected: (CompanionPose, Boolean) -> Unit,
    onToggleFullBody: () -> Unit,
    onToggleStageMode: () -> Unit,
    onAttachImageUri: (android.net.Uri) -> Unit,
    onClearImage: () -> Unit,
    onStartVoice: () -> Unit,
    onStopAll: () -> Unit,
    onSpeakLastReply: () -> Unit,
    onDismissNotice: () -> Unit,
    onDismissError: () -> Unit,
    onContactsPermissionResult: (Boolean) -> Unit,
    onConfirmPendingMessage: (String, String) -> Unit,
    onCancelPendingMessage: () -> Unit,
    onConfirmPendingCall: () -> Unit,
    onCancelPendingCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var chatExpanded by rememberSaveable { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) onAttachImageUri(uri)
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartVoice()
    }

    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onContactsPermissionResult(granted)
    }

    LaunchedEffect(needsContactsPermissionFor) {
        if (needsContactsPermissionFor != null) {
            contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    val triggerMicOrStop: () -> Unit = {
        if (isListening || isGenerating || isSpeaking) {
            onStopAll()
        } else {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (granted) onStartVoice()
            else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val listState = rememberLazyListState()
    LaunchedEffect(messages.size, streamingReply) {
        val total = messages.size + if (streamingReply != null) 1 else 0
        if (total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "T",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "TDB",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "V3.0 AI",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    val engineShort = if (settings.aiEngineMode == AiEngineMode.CUSTOM_API_KEY) "Key" else "Firebase"
                    Text(
                        text = "${settings.model.displayName} · $engineShort",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = useCharacterStage,
                    onClick = onToggleStageMode,
                    modifier = Modifier.testTag("toggle_stage_mode"),
                    leadingIcon = {
                        Icon(
                            imageVector = if (useCharacterStage) Icons.Rounded.Face else Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text(if (useCharacterStage) "3D সহচর" else "AI Orb") }
                )

                if (isGenerating || isSpeaking || isListening) {
                    FilledIconButton(
                        onClick = onStopAll,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("stop_button"),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Stop,
                            contentDescription = stringResource(id = R.string.cd_stop_action)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onSpeakLastReply,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("speak_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = stringResource(id = R.string.cd_read_aloud),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Main Hero & Conversation Area
        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            val isWide = maxWidth >= 680.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    HeroVisualStage(
                        useCharacterStage = useCharacterStage,
                        orbState = orbState,
                        wakeWordRunning = wakeWordRunning,
                        pose = pose,
                        isFullBody = isFullBody,
                        settings = settings,
                        onOrbClick = triggerMicOrStop,
                        onPoseSelected = onPoseSelected,
                        onToggleFullBody = onToggleFullBody,
                        modifier = Modifier
                            .weight(0.45f)
                            .fillMaxHeight()
                    )
                    ConversationPanel(
                        messages = messages,
                        streamingReply = streamingReply,
                        isListening = isListening,
                        partialDictation = partialDictation,
                        chatExpanded = true,
                        showExpandToggle = false,
                        onToggleExpand = {},
                        onQuickPrompt = onQuickPrompt,
                        listState = listState,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight()
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AnimatedVisibility(visible = !chatExpanded) {
                        HeroVisualStage(
                            useCharacterStage = useCharacterStage,
                            orbState = orbState,
                            wakeWordRunning = wakeWordRunning,
                            pose = pose,
                            isFullBody = isFullBody,
                            settings = settings,
                            onOrbClick = triggerMicOrStop,
                            onPoseSelected = onPoseSelected,
                            onToggleFullBody = onToggleFullBody,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(276.dp)
                        )
                    }

                    ConversationPanel(
                        messages = messages,
                        streamingReply = streamingReply,
                        isListening = isListening,
                        partialDictation = partialDictation,
                        chatExpanded = chatExpanded,
                        showExpandToggle = true,
                        onToggleExpand = { chatExpanded = !chatExpanded },
                        onQuickPrompt = onQuickPrompt,
                        listState = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    )
                }
            }
        }

        // Pending Interactive Message Confirmation Card (WhatsApp / SMS)
        AnimatedVisibility(visible = pendingMessageConfirmation != null) {
            pendingMessageConfirmation?.let { pending ->
                PendingMessageCard(
                    pending = pending,
                    onConfirm = onConfirmPendingMessage,
                    onCancel = onCancelPendingMessage
                )
            }
        }

        // Pending Interactive Call Confirmation Card
        AnimatedVisibility(visible = pendingCallConfirmation != null) {
            pendingCallConfirmation?.let { pendingCall ->
                PendingCallCard(
                    target = pendingCall.target,
                    onConfirm = onConfirmPendingCall,
                    onCancel = onCancelPendingCall
                )
            }
        }

        // Error or Notice Banners
        AnimatedVisibility(visible = errorBanner != null) {
            errorBanner?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = onDismissError, modifier = Modifier.size(28.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss error",
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = notice != null && errorBanner == null) {
            notice?.let { msg ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.85f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        IconButton(onClick = onDismissNotice, modifier = Modifier.size(24.dp)) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss notice",
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // Attached Photo Preview Chip
        AnimatedVisibility(visible = pendingImage != null) {
            pendingImage?.let { bmp ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "Selected photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(10.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "ছবি যুক্ত আছে · প্রশ্ন লিখুন বা পাঠান",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearImage) {
                        Icon(Icons.Rounded.Close, contentDescription = "Remove attached photo")
                    }
                }
            }
        }

        // Composer Dock
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 10.dp),
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            tonalElevation = 2.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("attach_photo_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddPhotoAlternate,
                        contentDescription = stringResource(id = R.string.cd_attach_image),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = triggerMicOrStop,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("voice_button")
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Rounded.GraphicEq else Icons.Rounded.Mic,
                        contentDescription = stringResource(id = R.string.cd_voice_dictation),
                        tint = if (isListening) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    )
                }

                OutlinedTextField(
                    value = composerText,
                    onValueChange = onComposerChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 120.dp)
                        .testTag("chat_input"),
                    placeholder = {
                        Text(
                            text = if (isListening) "শুনছি… কথা বলুন" else "TDB-কে কিছু বলো বা কমান্ড দাও…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() })
                )

                FilledIconButton(
                    onClick = onSend,
                    enabled = !isGenerating && (composerText.isNotBlank() || pendingImage != null),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("send_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(id = R.string.cd_send_message)
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroVisualStage(
    useCharacterStage: Boolean,
    orbState: OrbState,
    wakeWordRunning: Boolean,
    pose: CompanionPose,
    isFullBody: Boolean,
    settings: TdbSettings,
    onOrbClick: () -> Unit,
    onPoseSelected: (CompanionPose, Boolean) -> Unit,
    onToggleFullBody: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (useCharacterStage) {
        CompanionStage(
            pose = pose,
            isFullBody = isFullBody,
            motionQuality = settings.motionQuality,
            onPoseSelected = onPoseSelected,
            onToggleFullBody = onToggleFullBody,
            modifier = modifier
        )
    } else {
        AiOrbVisualizer(
            orbState = orbState,
            motionQuality = settings.motionQuality,
            wakeWordActive = wakeWordRunning,
            onOrbClick = onOrbClick,
            modifier = modifier
        )
    }
}

@Composable
private fun PendingMessageCard(
    pending: PendingMessageConfirmation,
    onConfirm: (String, String) -> Unit,
    onCancel: () -> Unit
) {
    var recipient by remember(pending) { mutableStateOf(pending.recipient) }
    var message by remember(pending) { mutableStateOf(pending.message) }
    val channelName = if (pending.channel == MessagingChannel.WHATSAPP) "WhatsApp" else "SMS"

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("pending_message_card"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "$channelName মেসেজ নিশ্চিতকরণ · এই মেসেজ পাঠাব কি?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            OutlinedTextField(
                value = recipient,
                onValueChange = { recipient = it },
                label = { Text("প্রাপকের নাম বা +880 নম্বর") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("বার্তা") },
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onConfirm(recipient, message) },
                    enabled = recipient.isNotBlank() && message.isNotBlank(),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("confirm_send_message_button")
                ) {
                    Text("$channelName খুলুন")
                }
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("cancel_send_message_button")
                ) {
                    Text("বাতিল")
                }
            }
        }
    }
}

@Composable
private fun PendingCallCard(
    target: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .testTag("pending_call_card"),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "ফোন কল নিশ্চিতকরণ", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = "$target-এর জন্য ডায়ালার প্রস্তুত করবেন?",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_call_button")
            ) {
                Text("ডায়ালার")
            }
            TextButton(onClick = onCancel) {
                Text("বাতিল")
            }
        }
    }
}

@Composable
private fun ConversationPanel(
    messages: List<ChatMessageEntity>,
    streamingReply: String?,
    isListening: Boolean,
    partialDictation: String,
    chatExpanded: Boolean,
    showExpandToggle: Boolean,
    onToggleExpand: () -> Unit,
    onQuickPrompt: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "আমাদের কথোপকথন",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (showExpandToggle) {
                    TextButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.testTag("toggle_chat_expand")
                    ) {
                        Text(
                            text = if (chatExpanded) "Orb দেখুন" else "বড় করুন",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = if (chatExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            if (isListening && partialDictation.isNotBlank()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "“$partialDictation”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }

            if (messages.isEmpty() && streamingReply == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "হ্যালো! আমি TDB Personal AI।",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "বাংলা বা ইংরেজিতে কথা বলুন, ভিশন ও ফোকাস পরিচালনা করুন, অথবা দ্রুত কমান্ড দিন।",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val starters = listOf(
                            "তুমি কে এবং কে তোমাকে তৈরি করেছে?",
                            "আজকের কাজের একটি সুন্দর পরিকল্পনা বানিয়ে দাও",
                            "Search Deep Barmon Bhai Vlog on YouTube",
                            "সামনে কী আছে দেখো"
                        )
                        items(starters) { prompt ->
                            AssistChip(
                                onClick = { onQuickPrompt(prompt) },
                                label = {
                                    Text(
                                        text = prompt,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatBubble(
                            role = msg.role,
                            text = msg.text,
                            attachmentLabel = msg.attachmentLabel,
                            actionBadge = msg.actionBadge
                        )
                    }
                    if (streamingReply != null) {
                        item(key = "streaming") {
                            ChatBubble(
                                role = "model",
                                text = streamingReply.ifEmpty { "চিন্তা করছি…" },
                                attachmentLabel = null,
                                actionBadge = "Live"
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(
    role: String,
    text: String,
    attachmentLabel: String?,
    actionBadge: String?
) {
    val isUser = role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (isUser) 18.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 18.dp
            ),
            color = if (isUser) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant,
            border = if (isUser) null else BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isUser) "YOU" else "TDB",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        else MaterialTheme.colorScheme.primary
                    )
                    if (!actionBadge.isNullOrBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = actionBadge,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (!attachmentLabel.isNullOrBlank()) {
                        Text(
                            text = "· $attachmentLabel",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isUser) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
