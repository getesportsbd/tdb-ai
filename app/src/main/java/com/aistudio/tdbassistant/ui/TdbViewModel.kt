package com.aistudio.tdbassistant.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.tdbassistant.data.ai.AiStreamEvent
import com.aistudio.tdbassistant.data.ai.GeminiDiagnosticResult
import com.aistudio.tdbassistant.data.ai.TdbAiRepository
import com.aistudio.tdbassistant.data.local.ActivityLogEntity
import com.aistudio.tdbassistant.data.local.ChatMessageEntity
import com.aistudio.tdbassistant.data.local.DailyGoalEntity
import com.aistudio.tdbassistant.data.local.GoalDomain
import com.aistudio.tdbassistant.data.local.MemoryCategory
import com.aistudio.tdbassistant.data.local.MemoryEntity
import com.aistudio.tdbassistant.data.local.TdbDatabase
import com.aistudio.tdbassistant.data.local.TdbLocalRepository
import com.aistudio.tdbassistant.data.preferences.AccentPalette
import com.aistudio.tdbassistant.data.preferences.AiEngineMode
import com.aistudio.tdbassistant.data.preferences.AiResponseStyle
import com.aistudio.tdbassistant.data.preferences.MotionQuality
import com.aistudio.tdbassistant.data.preferences.SettingsRepository
import com.aistudio.tdbassistant.data.preferences.TdbLanguage
import com.aistudio.tdbassistant.data.preferences.TdbModelOption
import com.aistudio.tdbassistant.data.preferences.TdbSettings
import com.aistudio.tdbassistant.data.preferences.ThemePreference
import com.aistudio.tdbassistant.data.preferences.VoiceStyle
import com.aistudio.tdbassistant.domain.CompanionPose
import com.aistudio.tdbassistant.domain.MessagingChannel
import com.aistudio.tdbassistant.domain.MobileCommandParser
import com.aistudio.tdbassistant.domain.OrbState
import com.aistudio.tdbassistant.domain.ParsedCommand
import com.aistudio.tdbassistant.domain.TdbPersonality
import com.aistudio.tdbassistant.domain.WakeWordMatcher
import com.aistudio.tdbassistant.notifications.TdbReminderReceiver
import com.aistudio.tdbassistant.platform.AndroidDeviceBridge
import com.aistudio.tdbassistant.platform.DeviceActionResult
import com.aistudio.tdbassistant.platform.InstalledAppItem
import com.aistudio.tdbassistant.platform.StorageSummary
import com.aistudio.tdbassistant.platform.TdbSpeechController
import com.aistudio.tdbassistant.platform.TtsEngineInfo
import com.aistudio.tdbassistant.platform.UsageSummaryReport
import com.aistudio.tdbassistant.security.SecurityManager
import com.aistudio.tdbassistant.voice.WakeWordForegroundService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TdbViewModel(application: Application) : AndroidViewModel(application) {
    private val db = TdbDatabase.getInstance(application)
    private val localRepo = TdbLocalRepository(
        db.chatMessageDao(),
        db.memoryDao(),
        db.dailyGoalDao(),
        db.activityLogDao()
    )
    private val settingsRepo = SettingsRepository(application)
    private val securityManager = SecurityManager(application)
    private val aiRepo = TdbAiRepository()
    val deviceBridge = AndroidDeviceBridge(application)

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _orbState = MutableStateFlow(OrbState.IDLE)
    val orbState: StateFlow<OrbState> = _orbState.asStateFlow()

    private val _pose = MutableStateFlow(CompanionPose.IDLE)
    val pose: StateFlow<CompanionPose> = _pose.asStateFlow()

    private val speechController = TdbSpeechController(
        context = application,
        onSpeakingStateChanged = { active ->
            _isSpeaking.value = active
            if (active) {
                _pose.value = CompanionPose.SPEAKING
                _orbState.value = OrbState.SPEAKING
            } else {
                if (_pose.value == CompanionPose.SPEAKING) _pose.value = CompanionPose.IDLE
                if (_orbState.value == OrbState.SPEAKING) _orbState.value = OrbState.IDLE
            }
        },
        onTtsError = { message ->
            _notice.value = message
        }
    )

    val settings: StateFlow<TdbSettings> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TdbSettings()
    )

    val messages: StateFlow<List<ChatMessageEntity>> = localRepo.messages.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val memories: StateFlow<List<MemoryEntity>> = localRepo.memories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val goals: StateFlow<List<DailyGoalEntity>> = localRepo.goals.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activityLogs: StateFlow<List<ActivityLogEntity>> = localRepo.activityLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val productivityScore: StateFlow<Int> = combine(goals, activityLogs) { g, l ->
        localRepo.computeProductivityScore(g, l)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val wakeWordRunning: StateFlow<Boolean> = WakeWordForegroundService.isRunning

    private val _useCharacterStage = MutableStateFlow(false)
    val useCharacterStage: StateFlow<Boolean> = _useCharacterStage.asStateFlow()

    private val _isFullBody = MutableStateFlow(false)
    val isFullBody: StateFlow<Boolean> = _isFullBody.asStateFlow()

    private val _streamingReply = MutableStateFlow<String?>(null)
    val streamingReply: StateFlow<String?> = _streamingReply.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _partialDictation = MutableStateFlow("")
    val partialDictation: StateFlow<String> = _partialDictation.asStateFlow()

    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    private val _pendingImage = MutableStateFlow<Bitmap?>(null)
    val pendingImage: StateFlow<Bitmap?> = _pendingImage.asStateFlow()

    private val _latestVisionReply = MutableStateFlow<String?>(null)
    val latestVisionReply: StateFlow<String?> = _latestVisionReply.asStateFlow()

    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice.asStateFlow()

    private val _errorBanner = MutableStateFlow<String?>(null)
    val errorBanner: StateFlow<String?> = _errorBanner.asStateFlow()

    private val _needsContactsPermissionFor = MutableStateFlow<PendingContactsRequest?>(null)
    val needsContactsPermissionFor: StateFlow<PendingContactsRequest?> =
        _needsContactsPermissionFor.asStateFlow()

    private val _pendingMessageConfirmation = MutableStateFlow<PendingMessageConfirmation?>(null)
    val pendingMessageConfirmation: StateFlow<PendingMessageConfirmation?> =
        _pendingMessageConfirmation.asStateFlow()

    private val _pendingCallConfirmation = MutableStateFlow<PendingCallConfirmation?>(null)
    val pendingCallConfirmation: StateFlow<PendingCallConfirmation?> =
        _pendingCallConfirmation.asStateFlow()

    private val _openVisionTabEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 4)
    val openVisionTabEvents: SharedFlow<Unit> = _openVisionTabEvents.asSharedFlow()

    private val _hasSavedApiKey = MutableStateFlow(securityManager.hasSavedApiKey())
    val hasSavedApiKey: StateFlow<Boolean> = _hasSavedApiKey.asStateFlow()

    private val _lastVerifiedModel = MutableStateFlow(securityManager.getLastVerifiedModel())
    val lastVerifiedModel: StateFlow<String> = _lastVerifiedModel.asStateFlow()

    private val _apiDiagnostic = MutableStateFlow<GeminiDiagnosticResult?>(null)
    val apiDiagnostic: StateFlow<GeminiDiagnosticResult?> = _apiDiagnostic.asStateFlow()

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _usageReport = MutableStateFlow(deviceBridge.getUsageSummary(1))
    val usageReport: StateFlow<UsageSummaryReport> = _usageReport.asStateFlow()

    private val _usageDays = MutableStateFlow(1)
    val usageDays: StateFlow<Int> = _usageDays.asStateFlow()

    private val _installedApps = MutableStateFlow<List<InstalledAppItem>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppItem>> = _installedApps.asStateFlow()

    private val _storageSummary = MutableStateFlow(deviceBridge.getStorageSummary())
    val storageSummary: StateFlow<StorageSummary> = _storageSummary.asStateFlow()

    private val _ttsEngines = MutableStateFlow<List<TtsEngineInfo>>(emptyList())
    val ttsEngines: StateFlow<List<TtsEngineInfo>> = _ttsEngines.asStateFlow()

    private val _webSummaryResult = MutableStateFlow<String?>(null)
    val webSummaryResult: StateFlow<String?> = _webSummaryResult.asStateFlow()

    private val _isSummarizingWeb = MutableStateFlow(false)
    val isSummarizingWeb: StateFlow<Boolean> = _isSummarizingWeb.asStateFlow()

    private val _focusTimer = MutableStateFlow(FocusTimerState())
    val focusTimer: StateFlow<FocusTimerState> = _focusTimer.asStateFlow()

    private var activeGenerationJob: Job? = null
    private var poseResetJob: Job? = null
    private var focusTimerJob: Job? = null

    init {
        viewModelScope.launch {
            delay(800)
            _ttsEngines.value = speechController.getAvailableEngines()
        }
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = deviceBridge.getInstalledApps()
        }
        viewModelScope.launch {
            WakeWordForegroundService.wakeEvents.collect { event ->
                when (event) {
                    is WakeWordForegroundService.WakeEvent.Unavailable -> {
                        _errorBanner.value = event.reason
                        settingsRepo.setWakeWordEnabled(false)
                    }
                    is WakeWordForegroundService.WakeEvent.Transcript -> {
                        val (woke, after) = WakeWordMatcher.extractAfterWakeWord(event.text)
                        if (woke) {
                            handleWakePhrase(after)
                        }
                    }
                }
            }
        }
    }

    private suspend fun handleWakePhrase(trailingCommand: String) {
        if (trailingCommand.isBlank()) {
            val ack = "জি, বলো। কী করতে পারি?"
            localRepo.addMessage("model", ack, actionBadge = "Hey TDB")
            _notice.value = ack
            speakWithCurrentSettings(ack)
            delay(1300)
            startVoiceDictation()
        } else {
            sendMessage(trailingCommand, null)
        }
    }

    fun updateComposerText(text: String) {
        _composerText.value = text
    }

    fun dismissNotice() {
        _notice.value = null
    }

    fun dismissError() {
        _errorBanner.value = null
        if (_orbState.value == OrbState.ERROR) {
            _orbState.value = OrbState.IDLE
        }
    }

    fun toggleStageMode() {
        _useCharacterStage.value = !_useCharacterStage.value
    }

    fun setGesturePose(newPose: CompanionPose, autoReset: Boolean = false) {
        poseResetJob?.cancel()
        _pose.value = newPose
        if (autoReset) {
            poseResetJob = viewModelScope.launch {
                delay(3000)
                if (_pose.value == newPose && !_isGenerating.value && !_isListening.value) {
                    _pose.value = CompanionPose.IDLE
                }
            }
        }
    }

    fun toggleFullBody() {
        _isFullBody.value = !_isFullBody.value
    }

    fun attachImageFromUri(uri: Uri) {
        viewModelScope.launch {
            val bitmap = decodeBitmapFromUri(uri)
            if (bitmap != null) {
                _pendingImage.value = bitmap
                _notice.value = "ছবি যুক্ত হয়েছে। এখন প্রশ্ন লিখুন বা পাঠান।"
            } else {
                _errorBanner.value = "ছবিটি লোড করা যায়নি। অন্য ছবি চেষ্টা করুন।"
            }
        }
    }

    fun analyzeVisionFromUri(uri: Uri, prompt: String = "এই ছবিটি বিশ্লেষণ করে বাংলায় সংক্ষেপে বলো।") {
        viewModelScope.launch {
            val bitmap = decodeBitmapFromUri(uri)
            if (bitmap != null) {
                analyzeVisionBitmap(bitmap, prompt)
            } else {
                _errorBanner.value = "ছবিটি লোড করা যায়নি।"
            }
        }
    }

    fun analyzeVisionBitmap(bitmap: Bitmap, prompt: String) {
        if (_isGenerating.value) return
        _latestVisionReply.value = null
        sendMessage(prompt, bitmap, isVisionTab = true)
    }

    private suspend fun decodeBitmapFromUri(uri: Uri): Bitmap? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = getApplication<Application>().contentResolver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(resolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = false
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(resolver, uri)
            }
        }.getOrNull()
    }

    fun clearAttachedImage() {
        _pendingImage.value = null
    }

    fun sendCurrentMessage() {
        val text = _composerText.value.trim()
        val image = _pendingImage.value
        if (text.isEmpty() && image == null) return
        if (_isGenerating.value) return

        val prompt = if (text.isEmpty()) "এই ছবিটি বিশ্লেষণ করে বাংলায় সংক্ষেপে বলো।" else text
        _composerText.value = ""
        _pendingImage.value = null
        sendMessage(prompt, image)
    }

    fun sendQuickPrompt(prompt: String) {
        if (_isGenerating.value) return
        sendMessage(prompt, null)
    }

    private fun sendMessage(prompt: String, image: Bitmap?, isVisionTab: Boolean = false) {
        _errorBanner.value = null
        _notice.value = null

        val waitingMsg = _pendingMessageConfirmation.value
        if (waitingMsg != null && waitingMsg.awaitingMessageInput && image == null) {
            viewModelScope.launch {
                localRepo.addMessage("user", prompt)
                _pendingMessageConfirmation.value = waitingMsg.copy(
                    message = prompt,
                    awaitingMessageInput = false
                )
                val preview = "মেসেজ প্রিভিউ প্রস্তুত: প্রাপক \"${waitingMsg.recipient.ifBlank { "অনির্ধারিত" }}\", বার্তা \"$prompt\"। পাঠাতে চাইলে নিচে Confirm চাপুন।"
                localRepo.addMessage("model", preview, actionBadge = "Confirm")
            }
            return
        }

        val parsed = if (image == null) MobileCommandParser.parse(prompt) else null
        if (parsed != null) {
            handleParsedCommand(prompt, parsed)
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            val currentSettings = settingsRepo.settings.first()
            val attachmentLabel = if (image != null) "Attached photo" else null
            val recentHistory = localRepo.getRecentMessages(18)
            val savedMemories = localRepo.getPromptMemories(18)
            val savedGoals = localRepo.goals.first()
            val customKey = if (currentSettings.aiEngineMode == AiEngineMode.CUSTOM_API_KEY) {
                securityManager.readApiKey()
            } else {
                ""
            }

            localRepo.addMessage("user", prompt, attachmentLabel = attachmentLabel)
            _isGenerating.value = true
            _streamingReply.value = ""
            _pose.value = CompanionPose.THINKING
            _orbState.value = OrbState.PROCESSING

            try {
                var completedReply = ""
                aiRepo.streamReply(
                    engineMode = currentSettings.aiEngineMode,
                    customApiKey = customKey,
                    modelOption = currentSettings.model,
                    language = currentSettings.language,
                    responseStyle = currentSettings.responseStyle,
                    memories = savedMemories,
                    goals = savedGoals,
                    recentHistory = recentHistory,
                    userPrompt = prompt,
                    attachedImage = image
                ).collect { event ->
                    when (event) {
                        is AiStreamEvent.Chunk -> {
                            _streamingReply.value = event.accumulatedText
                        }
                        is AiStreamEvent.Completed -> {
                            completedReply = event.finalText
                        }
                    }
                }

                _streamingReply.value = null
                _isGenerating.value = false
                localRepo.addMessage("model", completedReply)
                if (isVisionTab) {
                    _latestVisionReply.value = completedReply
                }

                if (currentSettings.spokenReplies) {
                    speakWithCurrentSettings(completedReply, currentSettings)
                } else {
                    _pose.value = CompanionPose.IDLE
                    _orbState.value = OrbState.IDLE
                }

                if (currentSettings.autoMemoryExtraction && image == null) {
                    launch {
                        val extracted = aiRepo.extractMemoryIfRelevant(prompt, savedMemories)
                        if (extracted != null) {
                            localRepo.addMemory(extracted.category, extracted.text)
                            _notice.value = "স্মৃতিতে সংরক্ষিত: ${extracted.text}"
                        }
                    }
                }
            } catch (e: CancellationException) {
                _streamingReply.value = null
                _isGenerating.value = false
                _pose.value = CompanionPose.IDLE
                _orbState.value = OrbState.IDLE
                throw e
            } catch (e: Exception) {
                _streamingReply.value = null
                _isGenerating.value = false
                _pose.value = CompanionPose.IDLE
                _orbState.value = OrbState.ERROR
                val msg = e.localizedMessage ?: "Gemini থেকে উত্তর আনা যায়নি। নেটওয়ার্ক বা API কনফিগারেশন দেখুন।"
                _errorBanner.value = msg
                if (isVisionTab) _latestVisionReply.value = msg
            }
        }
    }

    private fun handleParsedCommand(originalText: String, parsed: ParsedCommand) {
        viewModelScope.launch {
            localRepo.addMessage("user", originalText)
            _orbState.value = OrbState.EXECUTING
            when (parsed) {
                is ParsedCommand.WakeAcknowledge -> {
                    val reply = "জি, বলো। কী করতে পারি?"
                    localRepo.addMessage("model", reply, actionBadge = "Hey TDB")
                    _notice.value = reply
                    speakWithCurrentSettings(reply)
                }
                is ParsedCommand.OpenVision -> {
                    localRepo.addMessage("model", "TDB Vision ক্যামেরা ও বিশ্লেষণ স্ক্রিন খুলছি।", actionBadge = "Vision")
                    _openVisionTabEvents.tryEmit(Unit)
                    _orbState.value = OrbState.IDLE
                }
                is ParsedCommand.CompanionGesture -> {
                    setGesturePose(parsed.gesture, autoReset = parsed.gesture != CompanionPose.SITTING)
                    localRepo.addMessage("model", parsed.responseText, actionBadge = "Gesture")
                    _notice.value = parsed.responseText
                    _orbState.value = OrbState.IDLE
                }
                is ParsedCommand.OpenApp -> recordDeviceActionResult(deviceBridge.openApp(parsed.appName))
                is ParsedCommand.SearchYouTube -> recordDeviceActionResult(deviceBridge.searchYouTube(parsed.query))
                is ParsedCommand.YouTubeControl -> recordDeviceActionResult(deviceBridge.performYouTubeAction(parsed.action))
                is ParsedCommand.PhoneControl -> recordDeviceActionResult(deviceBridge.performPhoneControl(parsed.action))
                is ParsedCommand.SearchWeb -> recordDeviceActionResult(deviceBridge.searchWeb(parsed.query))
                is ParsedCommand.StartMessagingFlow -> {
                    val rec = parsed.recipient.orEmpty()
                    val msg = parsed.message.orEmpty()
                    if (msg.isBlank()) {
                        _pendingMessageConfirmation.value = PendingMessageConfirmation(
                            channel = parsed.channel,
                            recipient = rec,
                            message = "",
                            awaitingMessageInput = true
                        )
                        val ask = if (rec.isBlank()) {
                            "কাকে এবং কী মেসেজ পাঠাব? নিচের কার্ডে বা চ্যাটে লিখুন।"
                        } else {
                            "$rec-কে কী মেসেজ পাঠাব?"
                        }
                        localRepo.addMessage("model", ask, actionBadge = "Confirm")
                        if (settings.value.spokenReplies) speakWithCurrentSettings(ask)
                        else _orbState.value = OrbState.IDLE
                    } else {
                        _pendingMessageConfirmation.value = PendingMessageConfirmation(
                            channel = parsed.channel,
                            recipient = rec,
                            message = msg,
                            awaitingMessageInput = false
                        )
                        val ask = "${parsed.channel.name}: \"$rec\"-কে \"$msg\" পাঠানোর আগে নিচে Confirm চাপুন।"
                        localRepo.addMessage("model", ask, actionBadge = "Confirm")
                        _orbState.value = OrbState.IDLE
                    }
                }
                is ParsedCommand.StartCallFlow -> {
                    val target = parsed.target
                    if (target.isNullOrBlank()) {
                        recordDeviceActionResult(deviceBridge.openPhoneDialer(null))
                    } else {
                        _pendingCallConfirmation.value = PendingCallConfirmation(target)
                        val ask = "\"$target\"-কে ফোন ডায়ালারে কল প্রস্তুত করব কি? নিচে Confirm বা Cancel চাপুন।"
                        localRepo.addMessage("model", ask, actionBadge = "Call Confirm")
                        _orbState.value = OrbState.IDLE
                    }
                }
            }
        }
    }

    fun confirmPendingMessage(editedRecipient: String, editedMessage: String) {
        val current = _pendingMessageConfirmation.value ?: return
        _pendingMessageConfirmation.value = null
        viewModelScope.launch {
            val res = when (current.channel) {
                MessagingChannel.WHATSAPP -> deviceBridge.prepareWhatsAppDraft(editedRecipient, editedMessage)
                MessagingChannel.SMS -> deviceBridge.prepareSmsDraft(editedRecipient, editedMessage)
            }
            recordDeviceActionResult(res)
        }
    }

    fun cancelPendingMessage() {
        _pendingMessageConfirmation.value = null
        viewModelScope.launch {
            localRepo.addMessage("model", "মেসেজ পাঠানো বাতিল করা হয়েছে।", actionBadge = "Cancelled")
        }
    }

    fun confirmPendingCall() {
        val current = _pendingCallConfirmation.value ?: return
        _pendingCallConfirmation.value = null
        viewModelScope.launch {
            recordDeviceActionResult(deviceBridge.openPhoneDialer(current.target))
        }
    }

    fun cancelPendingCall() {
        _pendingCallConfirmation.value = null
        viewModelScope.launch {
            localRepo.addMessage("model", "কল প্রস্তুত করা বাতিল করা হয়েছে।", actionBadge = "Cancelled")
        }
    }

    fun executeActionOpenApp(appName: String): DeviceActionResult =
        deviceBridge.openApp(appName).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executeActionSearchYouTube(query: String): DeviceActionResult =
        deviceBridge.searchYouTube(query).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executeYouTubeControl(action: com.aistudio.tdbassistant.domain.YouTubeAction): DeviceActionResult =
        deviceBridge.performYouTubeAction(action).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executePhoneControl(action: com.aistudio.tdbassistant.domain.PhoneSystemAction): DeviceActionResult =
        deviceBridge.performPhoneControl(action).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executeActionSearchWeb(query: String): DeviceActionResult =
        deviceBridge.searchWeb(query).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executeActionWhatsApp(recipient: String, message: String): DeviceActionResult =
        deviceBridge.prepareWhatsAppDraft(recipient, message).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executeActionSms(recipient: String, message: String): DeviceActionResult =
        deviceBridge.prepareSmsDraft(recipient, message).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun executeActionDial(target: String?): DeviceActionResult =
        deviceBridge.openPhoneDialer(target).also { viewModelScope.launch { recordDeviceActionResult(it) } }

    fun onContactsPermissionResult(granted: Boolean) {
        val pending = _needsContactsPermissionFor.value ?: return
        _needsContactsPermissionFor.value = null
        viewModelScope.launch {
            if (granted) {
                val res = when {
                    pending.isCall -> deviceBridge.openPhoneDialer(pending.recipient)
                    pending.channel == MessagingChannel.SMS -> deviceBridge.prepareSmsDraft(pending.recipient, pending.message)
                    else -> deviceBridge.prepareWhatsAppDraft(pending.recipient, pending.message)
                }
                recordDeviceActionResult(res)
            } else {
                val msg = "কন্টাক্ট অনুমতি না দেওয়ায় নাম খোঁজা যায়নি। চাইলে দেশের কোডসহ ফোন নম্বর লিখুন।"
                localRepo.addMessage("model", msg)
                _notice.value = msg
            }
        }
    }

    private suspend fun recordDeviceActionResult(result: DeviceActionResult) {
        when (result) {
            is DeviceActionResult.Success -> {
                localRepo.addMessage("model", result.message, actionBadge = result.badge)
                _notice.value = result.message
                _orbState.value = OrbState.IDLE
            }
            is DeviceActionResult.NeedsContactsPermission -> {
                _needsContactsPermissionFor.value = PendingContactsRequest(
                    recipient = result.recipient,
                    message = result.message.orEmpty(),
                    isCall = result.isCall
                )
                _notice.value = "কন্টাক্ট খুঁজে পেতে Contacts অনুমতি দিন।"
                _orbState.value = OrbState.IDLE
            }
            is DeviceActionResult.Failure -> {
                localRepo.addMessage("model", result.message, actionBadge = "Info")
                _notice.value = result.message
                _orbState.value = OrbState.IDLE
            }
        }
    }

    fun startVoiceDictation() {
        val lang = settings.value.language.tag
        _isListening.value = true
        _partialDictation.value = ""
        _pose.value = CompanionPose.LISTENING
        _orbState.value = OrbState.LISTENING
        speechController.stopSpeaking()

        speechController.startDictation(
            languageTag = lang,
            onPartial = { partial -> _partialDictation.value = partial },
            onFinal = { text ->
                _isListening.value = false
                _partialDictation.value = ""
                _pose.value = CompanionPose.IDLE
                _orbState.value = OrbState.IDLE
                sendMessage(text, _pendingImage.value.also { _pendingImage.value = null })
            },
            onError = { err ->
                _isListening.value = false
                _partialDictation.value = ""
                _pose.value = CompanionPose.IDLE
                _orbState.value = OrbState.ERROR
                _notice.value = err
            }
        )
    }

    fun stopVoiceOrGeneration() {
        activeGenerationJob?.cancel()
        activeGenerationJob = null
        _streamingReply.value = null
        _isGenerating.value = false
        _isListening.value = false
        _partialDictation.value = ""
        speechController.stopDictation()
        speechController.stopSpeaking()
        _pose.value = CompanionPose.IDLE
        _orbState.value = OrbState.IDLE
        _notice.value = "থামানো হয়েছে।"
    }

    private fun speakWithCurrentSettings(text: String, cfg: TdbSettings = settings.value) {
        speechController.speak(
            text = text,
            languageTag = cfg.language.tag,
            voiceStyle = cfg.voiceStyle,
            speed = cfg.voiceSpeed,
            pitch = cfg.voicePitch,
            volume = cfg.speechVolume,
            enginePackage = cfg.ttsEnginePackage
        )
    }

    fun speakLastReply() {
        val lastModelMsg = messages.value.lastOrNull { it.role == "model" }?.text
        if (lastModelMsg.isNullOrBlank()) {
            _notice.value = "শোনানোর মতো কোনো উত্তর এখনো নেই।"
            return
        }
        speakWithCurrentSettings(lastModelMsg)
    }

    fun testCurrentVoice() {
        val cfg = settings.value
        val sample = when (cfg.language) {
            TdbLanguage.BANGLA -> "হ্যালো! আমি TDB Personal AI Assistant। আমাকে Deep Barmon তৈরি করেছেন।"
            TdbLanguage.HINDI -> "नमस्ते! मैं TDB Personal AI Assistant हूँ।"
            else -> TdbPersonality.creatorAnswerEn()
        }
        speakWithCurrentSettings(sample, cfg)
    }

    // API Key Management (Android Keystore encrypted)
    fun saveAndTestApiKey(rawKey: String) {
        viewModelScope.launch {
            _isTestingApi.value = true
            val modelId = settings.value.model.modelId
            val result = aiRepo.testDirectKey(rawKey, modelId)
            _apiDiagnostic.value = result
            if (result.ok) {
                val saved = securityManager.saveApiKey(rawKey)
                if (saved) {
                    securityManager.markVerified(modelId)
                    _hasSavedApiKey.value = true
                    _lastVerifiedModel.value = modelId
                    settingsRepo.setAiEngineMode(AiEngineMode.CUSTOM_API_KEY)
                }
            }
            _isTestingApi.value = false
        }
    }

    fun testSavedApiConnection() {
        viewModelScope.launch {
            _isTestingApi.value = true
            val key = securityManager.readApiKey()
            val modelId = settings.value.model.modelId
            val result = aiRepo.testDirectKey(key, modelId)
            _apiDiagnostic.value = result
            if (result.ok) {
                securityManager.markVerified(modelId)
                _lastVerifiedModel.value = modelId
            }
            _isTestingApi.value = false
        }
    }

    fun removeSavedApiKey() {
        securityManager.removeApiKey()
        _hasSavedApiKey.value = false
        _lastVerifiedModel.value = ""
        _apiDiagnostic.value = GeminiDiagnosticResult(
            ok = true,
            code = "KEY_REMOVED",
            message = "সংরক্ষিত API key মুছে ফেলা হয়েছে। এখন বিল্ট-ইন Firebase AI Logic সক্রিয়।"
        )
        viewModelScope.launch {
            settingsRepo.setAiEngineMode(AiEngineMode.FIREBASE_AI_LOGIC)
        }
    }

    // Web Summarizer
    fun summarizeWebUrl(url: String) {
        if (url.isBlank() || _isSummarizingWeb.value) return
        viewModelScope.launch {
            _isSummarizingWeb.value = true
            _webSummaryResult.value = null
            val cfg = settingsRepo.settings.first()
            val key = if (cfg.aiEngineMode == AiEngineMode.CUSTOM_API_KEY) securityManager.readApiKey() else ""
            runCatching {
                aiRepo.summarizeWebUrl(url, cfg.aiEngineMode, key, cfg.model, cfg.language)
            }.onSuccess { summary ->
                _webSummaryResult.value = summary
                localRepo.addMemory(MemoryCategory.SUMMARY, "Web Summary ($url): ${summary.take(180)}")
            }.onFailure { err ->
                _webSummaryResult.value = "ত্রুটি: ${err.localizedMessage ?: "ওয়েবপেজ সারাংশ করা যায়নি।"}"
            }
            _isSummarizingWeb.value = false
        }
    }

    // Focus Timer & Productivity Goals
    fun startFocusTimer(focusMins: Int = 25, breakMins: Int = 5, goal: DailyGoalEntity? = null) {
        focusTimerJob?.cancel()
        _focusTimer.value = FocusTimerState(
            isRunning = true,
            isBreak = false,
            focusMinutes = focusMins,
            breakMinutes = breakMins,
            remainingSeconds = focusMins * 60,
            linkedGoalTitle = goal?.title,
            linkedGoalId = goal?.id
        )
        runTimerLoop()
    }

    fun pauseOrResumeFocusTimer() {
        val current = _focusTimer.value
        if (current.isRunning) {
            focusTimerJob?.cancel()
            _focusTimer.value = current.copy(isRunning = false)
        } else if (current.remainingSeconds > 0) {
            _focusTimer.value = current.copy(isRunning = true)
            runTimerLoop()
        }
    }

    fun resetFocusTimer() {
        focusTimerJob?.cancel()
        val f = _focusTimer.value.focusMinutes
        _focusTimer.value = _focusTimer.value.copy(
            isRunning = false,
            isBreak = false,
            remainingSeconds = f * 60
        )
    }

    private fun runTimerLoop() {
        focusTimerJob?.cancel()
        focusTimerJob = viewModelScope.launch {
            while (_focusTimer.value.isRunning && _focusTimer.value.remainingSeconds > 0) {
                delay(1000L)
                val cur = _focusTimer.value
                val next = cur.remainingSeconds - 1
                _focusTimer.value = cur.copy(remainingSeconds = next)
                if (next <= 0) {
                    if (!cur.isBreak) {
                        localRepo.addActivityLog(
                            title = cur.linkedGoalTitle ?: "Focus Session",
                            domain = GoalDomain.STUDY,
                            durationMinutes = cur.focusMinutes
                        )
                        cur.linkedGoalId?.let { gid ->
                            goals.value.firstOrNull { it.id == gid }?.let { g ->
                                localRepo.logGoalMinutes(g, cur.focusMinutes)
                            }
                        }
                        val noticeMsg = "দারুণ! ${cur.focusMinutes} মিনিট ফোকাস সম্পন্ন হয়েছে। এখন ${cur.breakMinutes} মিনিটের বিরতি নিন।"
                        _notice.value = noticeMsg
                        TdbReminderReceiver.showNotification(
                            getApplication(),
                            5001,
                            "TDB Focus Complete",
                            noticeMsg
                        )
                        _focusTimer.value = cur.copy(
                            isBreak = true,
                            remainingSeconds = cur.breakMinutes * 60,
                            isRunning = true
                        )
                    } else {
                        _notice.value = "বিরতি শেষ! আবার কাজে ফিরতে পারেন।"
                        _focusTimer.value = cur.copy(
                            isBreak = false,
                            remainingSeconds = cur.focusMinutes * 60,
                            isRunning = false
                        )
                    }
                }
            }
        }
    }

    fun addGoal(title: String, domain: GoalDomain, targetMinutes: Int, reminderHour: Int?, reminderMinute: Int?) {
        viewModelScope.launch {
            localRepo.addGoal(title, domain, targetMinutes, reminderHour, reminderMinute)
            if (reminderHour != null && reminderMinute != null) {
                TdbReminderReceiver.scheduleDailyOrNext(
                    context = getApplication(),
                    requestCode = title.hashCode(),
                    hour24 = reminderHour,
                    minute = reminderMinute,
                    title = "TDB Goal Reminder",
                    body = "$title (${domain.labelBn}) — এখন শুরু করার সময় হয়েছে।"
                )
            }
            _notice.value = "নতুন লক্ষ্য যুক্ত হয়েছে।"
        }
    }

    fun toggleGoal(goal: DailyGoalEntity) {
        viewModelScope.launch { localRepo.toggleGoalCompletion(goal) }
    }

    fun deleteGoal(id: String) {
        viewModelScope.launch { localRepo.deleteGoal(id) }
    }

    fun addActivityLog(title: String, domain: GoalDomain, durationMinutes: Int, note: String = "") {
        viewModelScope.launch {
            localRepo.addActivityLog(title, domain, durationMinutes, note)
            _notice.value = "অ্যাক্টিভিটি লগ সংরক্ষিত হয়েছে।"
        }
    }

    fun deleteActivityLog(id: String) {
        viewModelScope.launch { localRepo.deleteActivityLog(id) }
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            localRepo.clearHistory()
            _notice.value = "কথোপকথনের ইতিহাস মুছে ফেলা হয়েছে।"
        }
    }

    fun addMemory(category: MemoryCategory, text: String) {
        viewModelScope.launch {
            localRepo.addMemory(category, text)
            _notice.value = "নতুন স্মৃতি যুক্ত হয়েছে।"
        }
    }

    fun editMemory(id: String, category: MemoryCategory, newText: String) {
        viewModelScope.launch {
            localRepo.editMemory(id, category, newText)
            _notice.value = "স্মৃতি আপডেট করা হয়েছে।"
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch { localRepo.deleteMemory(id) }
    }

    fun exportMemories() {
        val text = localRepo.formatExportText(memories.value)
        deviceBridge.shareText("TDB Personal AI — Recollections", text)
    }

    fun refreshUsageSummary(days: Int = _usageDays.value) {
        _usageDays.value = days
        _usageReport.value = deviceBridge.getUsageSummary(days)
    }

    fun refreshAppsAndStorage() {
        _storageSummary.value = deviceBridge.getStorageSummary()
        viewModelScope.launch(Dispatchers.IO) {
            _installedApps.value = deviceBridge.getInstalledApps()
        }
    }

    fun setWakeWordEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.setWakeWordEnabled(enabled)
            val app = getApplication<Application>()
            if (enabled) {
                WakeWordForegroundService.start(app, settings.value.language.tag)
                _notice.value = "Always-Available \"Hey TDB\" চালু করা হয়েছে।"
            } else {
                WakeWordForegroundService.stop(app)
                _notice.value = "ব্যাকগ্রাউন্ড Wake Word শোনা বন্ধ করা হয়েছে।"
            }
        }
    }

    fun setLanguage(language: TdbLanguage) = viewModelScope.launch { settingsRepo.setLanguage(language) }
    fun setModel(model: TdbModelOption) = viewModelScope.launch { settingsRepo.setModel(model) }
    fun setAiEngineMode(mode: AiEngineMode) = viewModelScope.launch { settingsRepo.setAiEngineMode(mode) }
    fun setResponseStyle(style: AiResponseStyle) = viewModelScope.launch { settingsRepo.setResponseStyle(style) }
    fun setVoiceStyle(style: VoiceStyle) = viewModelScope.launch { settingsRepo.setVoiceStyle(style) }
    fun setVoiceSpeed(speed: Float) = viewModelScope.launch { settingsRepo.setVoiceSpeed(speed) }
    fun setVoicePitch(pitch: Float) = viewModelScope.launch { settingsRepo.setVoicePitch(pitch) }
    fun setSpeechVolume(volume: Float) = viewModelScope.launch { settingsRepo.setSpeechVolume(volume) }
    fun setTtsEnginePackage(pkg: String) = viewModelScope.launch { settingsRepo.setTtsEnginePackage(pkg) }
    fun setSpokenReplies(enabled: Boolean) = viewModelScope.launch { settingsRepo.setSpokenReplies(enabled) }
    fun setMotionQuality(quality: MotionQuality) = viewModelScope.launch { settingsRepo.setMotionQuality(quality) }
    fun setAutoMemoryExtraction(enabled: Boolean) = viewModelScope.launch { settingsRepo.setAutoMemoryExtraction(enabled) }
    fun setThemePreference(theme: ThemePreference) = viewModelScope.launch { settingsRepo.setThemePreference(theme) }
    fun setAccentPalette(palette: AccentPalette) = viewModelScope.launch { settingsRepo.setAccentPalette(palette) }

    override fun onCleared() {
        super.onCleared()
        focusTimerJob?.cancel()
        speechController.release()
    }
}
