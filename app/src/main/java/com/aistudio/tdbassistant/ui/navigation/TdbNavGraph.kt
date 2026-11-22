package com.aistudio.tdbassistant.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.aistudio.tdbassistant.automation.TdbAccessibilityService
import com.aistudio.tdbassistant.data.preferences.ThemePreference
import com.aistudio.tdbassistant.ui.TdbViewModel
import com.aistudio.tdbassistant.ui.components.TdbVisionScreen
import com.aistudio.tdbassistant.ui.screens.CompanionChatScreen
import com.aistudio.tdbassistant.ui.screens.MemoriesScreen
import com.aistudio.tdbassistant.ui.screens.SettingsScreen
import com.aistudio.tdbassistant.ui.screens.WellnessActionsScreen
import com.aistudio.tdbassistant.ui.theme.TdbTheme
import kotlinx.serialization.Serializable

@Serializable
sealed interface TdbRoute {
    @Serializable
    data object Assistant : TdbRoute

    @Serializable
    data object Vision : TdbRoute

    @Serializable
    data object Wellness : TdbRoute

    @Serializable
    data object Memories : TdbRoute

    @Serializable
    data object Settings : TdbRoute
}

private data class NavTab(
    val route: TdbRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

private val TDB_TABS = listOf(
    NavTab(
        route = TdbRoute.Assistant,
        label = "সহচর",
        selectedIcon = Icons.Rounded.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        tag = "nav_companion"
    ),
    NavTab(
        route = TdbRoute.Vision,
        label = "ভিশন",
        selectedIcon = Icons.Rounded.CameraAlt,
        unselectedIcon = Icons.Outlined.CameraAlt,
        tag = "nav_vision"
    ),
    NavTab(
        route = TdbRoute.Wellness,
        label = "ফোকাস",
        selectedIcon = Icons.Rounded.Bolt,
        unselectedIcon = Icons.Outlined.Bolt,
        tag = "nav_wellness"
    ),
    NavTab(
        route = TdbRoute.Memories,
        label = "স্মৃতি",
        selectedIcon = Icons.Rounded.Psychology,
        unselectedIcon = Icons.Outlined.Psychology,
        tag = "nav_memories"
    ),
    NavTab(
        route = TdbRoute.Settings,
        label = "সেটিংস",
        selectedIcon = Icons.Rounded.Settings,
        unselectedIcon = Icons.Outlined.Settings,
        tag = "nav_settings"
    )
)

@Composable
fun TdbAppRoot(viewModel: TdbViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val useDark = when (settings.themePreference) {
        ThemePreference.SYSTEM -> isSystemInDarkTheme()
        ThemePreference.LIGHT -> false
        ThemePreference.DARK -> true
    }

    TdbTheme(darkTheme = useDark, accentPalette = settings.accentPalette) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination

        val orbState by viewModel.orbState.collectAsStateWithLifecycle()
        val pose by viewModel.pose.collectAsStateWithLifecycle()
        val isFullBody by viewModel.isFullBody.collectAsStateWithLifecycle()
        val useCharacterStage by viewModel.useCharacterStage.collectAsStateWithLifecycle()
        val wakeWordRunning by viewModel.wakeWordRunning.collectAsStateWithLifecycle()
        val messages by viewModel.messages.collectAsStateWithLifecycle()
        val memories by viewModel.memories.collectAsStateWithLifecycle()
        val goals by viewModel.goals.collectAsStateWithLifecycle()
        val activityLogs by viewModel.activityLogs.collectAsStateWithLifecycle()
        val productivityScore by viewModel.productivityScore.collectAsStateWithLifecycle()
        val focusTimer by viewModel.focusTimer.collectAsStateWithLifecycle()
        val streamingReply by viewModel.streamingReply.collectAsStateWithLifecycle()
        val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
        val isListening by viewModel.isListening.collectAsStateWithLifecycle()
        val partialDictation by viewModel.partialDictation.collectAsStateWithLifecycle()
        val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
        val composerText by viewModel.composerText.collectAsStateWithLifecycle()
        val pendingImage by viewModel.pendingImage.collectAsStateWithLifecycle()
        val latestVisionReply by viewModel.latestVisionReply.collectAsStateWithLifecycle()
        val notice by viewModel.notice.collectAsStateWithLifecycle()
        val errorBanner by viewModel.errorBanner.collectAsStateWithLifecycle()
        val needsContacts by viewModel.needsContactsPermissionFor.collectAsStateWithLifecycle()
        val pendingMsgConf by viewModel.pendingMessageConfirmation.collectAsStateWithLifecycle()
        val pendingCallConf by viewModel.pendingCallConfirmation.collectAsStateWithLifecycle()
        val usageReport by viewModel.usageReport.collectAsStateWithLifecycle()
        val usageDays by viewModel.usageDays.collectAsStateWithLifecycle()
        val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
        val storageSummary by viewModel.storageSummary.collectAsStateWithLifecycle()
        val webSummaryResult by viewModel.webSummaryResult.collectAsStateWithLifecycle()
        val isSummarizingWeb by viewModel.isSummarizingWeb.collectAsStateWithLifecycle()
        val hasSavedApiKey by viewModel.hasSavedApiKey.collectAsStateWithLifecycle()
        val lastVerifiedModel by viewModel.lastVerifiedModel.collectAsStateWithLifecycle()
        val apiDiagnostic by viewModel.apiDiagnostic.collectAsStateWithLifecycle()
        val isTestingApi by viewModel.isTestingApi.collectAsStateWithLifecycle()
        val ttsEngines by viewModel.ttsEngines.collectAsStateWithLifecycle()
        val accessibilityConnected by TdbAccessibilityService.isConnected.collectAsStateWithLifecycle()

        val navigateToTab: (TdbRoute) -> Unit = { route ->
            navController.navigate(route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }

        LaunchedEffect(Unit) {
            viewModel.openVisionTabEvents.collect {
                navigateToTab(TdbRoute.Vision)
            }
        }

        val bgBrush = Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.background,
                MaterialTheme.colorScheme.background
            )
        )

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
        ) {
            val useRail = maxWidth >= 680.dp

            Scaffold(
                containerColor = androidx.compose.ui.graphics.Color.Transparent,
                contentWindowInsets = WindowInsets.safeDrawing,
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f)
                        ) {
                            TDB_TABS.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.hasRoute(tab.route::class)
                                } == true
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag(tab.tag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    if (useRail) {
                        NavigationRail(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                        ) {
                            TDB_TABS.forEach { tab ->
                                val selected = currentDestination?.hierarchy?.any {
                                    it.hasRoute(tab.route::class)
                                } == true
                                NavigationRailItem(
                                    selected = selected,
                                    onClick = { navigateToTab(tab.route) },
                                    modifier = Modifier.testTag(tab.tag),
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    },
                                    label = { Text(tab.label) }
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = TdbRoute.Assistant,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable<TdbRoute.Assistant> {
                            CompanionChatScreen(
                                settings = settings,
                                orbState = orbState,
                                pose = pose,
                                isFullBody = isFullBody,
                                useCharacterStage = useCharacterStage,
                                wakeWordRunning = wakeWordRunning,
                                messages = messages,
                                streamingReply = streamingReply,
                                isGenerating = isGenerating,
                                isListening = isListening,
                                partialDictation = partialDictation,
                                isSpeaking = isSpeaking,
                                composerText = composerText,
                                pendingImage = pendingImage,
                                notice = notice,
                                errorBanner = errorBanner,
                                needsContactsPermissionFor = needsContacts,
                                pendingMessageConfirmation = pendingMsgConf,
                                pendingCallConfirmation = pendingCallConf,
                                onComposerChange = viewModel::updateComposerText,
                                onSend = viewModel::sendCurrentMessage,
                                onQuickPrompt = viewModel::sendQuickPrompt,
                                onPoseSelected = viewModel::setGesturePose,
                                onToggleFullBody = viewModel::toggleFullBody,
                                onToggleStageMode = viewModel::toggleStageMode,
                                onAttachImageUri = viewModel::attachImageFromUri,
                                onClearImage = viewModel::clearAttachedImage,
                                onStartVoice = viewModel::startVoiceDictation,
                                onStopAll = viewModel::stopVoiceOrGeneration,
                                onSpeakLastReply = viewModel::speakLastReply,
                                onDismissNotice = viewModel::dismissNotice,
                                onDismissError = viewModel::dismissError,
                                onContactsPermissionResult = viewModel::onContactsPermissionResult,
                                onConfirmPendingMessage = viewModel::confirmPendingMessage,
                                onCancelPendingMessage = viewModel::cancelPendingMessage,
                                onConfirmPendingCall = viewModel::confirmPendingCall,
                                onCancelPendingCall = viewModel::cancelPendingCall
                            )
                        }

                        composable<TdbRoute.Vision> {
                            TdbVisionScreen(
                                isGenerating = isGenerating,
                                latestVisionReply = latestVisionReply,
                                onAnalyzeBitmap = viewModel::analyzeVisionBitmap,
                                onPickPhotoUri = { uri -> viewModel.analyzeVisionFromUri(uri) }
                            )
                        }

                        composable<TdbRoute.Wellness> {
                            WellnessActionsScreen(
                                productivityScore = productivityScore,
                                focusTimer = focusTimer,
                                goals = goals,
                                activityLogs = activityLogs,
                                usageReport = usageReport,
                                usageDays = usageDays,
                                accessibilityConnected = accessibilityConnected,
                                installedApps = installedApps,
                                storageSummary = storageSummary,
                                webSummaryResult = webSummaryResult,
                                isSummarizingWeb = isSummarizingWeb,
                                onStartFocus = viewModel::startFocusTimer,
                                onPauseResumeFocus = viewModel::pauseOrResumeFocusTimer,
                                onResetFocus = viewModel::resetFocusTimer,
                                onAddGoal = viewModel::addGoal,
                                onToggleGoal = viewModel::toggleGoal,
                                onDeleteGoal = viewModel::deleteGoal,
                                onAddActivityLog = viewModel::addActivityLog,
                                onDeleteActivityLog = viewModel::deleteActivityLog,
                                onRefreshUsage = viewModel::refreshUsageSummary,
                                onOpenUsageSettings = { viewModel.deviceBridge.openUsageAccessSettings() },
                                onOpenAccessibilitySettings = { viewModel.deviceBridge.openAccessibilitySettings() },
                                onAskCoachAboutUsage = { coachPrompt ->
                                    navigateToTab(TdbRoute.Assistant)
                                    viewModel.sendQuickPrompt(coachPrompt)
                                },
                                onPhoneControl = viewModel::executePhoneControl,
                                onYouTubeControl = viewModel::executeYouTubeControl,
                                onOpenApp = viewModel::executeActionOpenApp,
                                onSearchYouTube = viewModel::executeActionSearchYouTube,
                                onSearchWeb = viewModel::executeActionSearchWeb,
                                onSummarizeWebUrl = viewModel::summarizeWebUrl,
                                onOpenUrlInBrowser = { url -> viewModel.deviceBridge.openUrlInBrowser(url) },
                                onPrepareWhatsApp = viewModel::executeActionWhatsApp,
                                onPrepareSms = viewModel::executeActionSms,
                                onDialPhone = viewModel::executeActionDial,
                                onOpenDocumentUri = { uri -> viewModel.deviceBridge.openDocumentUri(uri) },
                                onShareDocumentUri = { uri -> viewModel.deviceBridge.shareDocumentUri("TDB File Share", uri) },
                                onRefreshAppsAndStorage = viewModel::refreshAppsAndStorage
                            )
                        }

                        composable<TdbRoute.Memories> {
                            MemoriesScreen(
                                memories = memories,
                                onAddMemory = viewModel::addMemory,
                                onEditMemory = viewModel::editMemory,
                                onDeleteMemory = viewModel::deleteMemory,
                                onExportMemories = viewModel::exportMemories
                            )
                        }

                        composable<TdbRoute.Settings> {
                            SettingsScreen(
                                settings = settings,
                                hasSavedApiKey = hasSavedApiKey,
                                lastVerifiedModel = lastVerifiedModel,
                                apiDiagnostic = apiDiagnostic,
                                isTestingApi = isTestingApi,
                                wakeWordRunning = wakeWordRunning,
                                accessibilityConnected = accessibilityConnected,
                                ttsEngines = ttsEngines,
                                onSelectEngineMode = viewModel::setAiEngineMode,
                                onSelectModel = viewModel::setModel,
                                onSelectResponseStyle = viewModel::setResponseStyle,
                                onSaveAndTestApiKey = viewModel::saveAndTestApiKey,
                                onTestSavedApiConnection = viewModel::testSavedApiConnection,
                                onRemoveSavedApiKey = viewModel::removeSavedApiKey,
                                onSelectLanguage = viewModel::setLanguage,
                                onSelectVoiceStyle = viewModel::setVoiceStyle,
                                onVoiceSpeedChange = viewModel::setVoiceSpeed,
                                onVoicePitchChange = viewModel::setVoicePitch,
                                onSpeechVolumeChange = viewModel::setSpeechVolume,
                                onSelectTtsEngine = viewModel::setTtsEnginePackage,
                                onTestVoice = viewModel::testCurrentVoice,
                                onToggleSpokenReplies = viewModel::setSpokenReplies,
                                onToggleWakeWord = viewModel::setWakeWordEnabled,
                                onSelectMotionQuality = viewModel::setMotionQuality,
                                onToggleAutoMemory = viewModel::setAutoMemoryExtraction,
                                onSelectTheme = viewModel::setThemePreference,
                                onSelectAccentPalette = viewModel::setAccentPalette,
                                onClearHistory = viewModel::clearChatHistory,
                                onExportMemories = viewModel::exportMemories,
                                onOpenAndroidAppSettings = { viewModel.deviceBridge.openAppSettings() },
                                onOpenUsageAccessSettings = { viewModel.deviceBridge.openUsageAccessSettings() },
                                onOpenAccessibilitySettings = { viewModel.deviceBridge.openAccessibilitySettings() },
                                onOpenNotificationListenerSettings = { viewModel.deviceBridge.openNotificationListenerSettings() }
                            )
                        }
                    }
                }
            }
        }
    }
}
