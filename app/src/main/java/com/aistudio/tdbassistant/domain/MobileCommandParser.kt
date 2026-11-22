package com.aistudio.tdbassistant.domain

enum class OrbState(val labelBn: String, val labelEn: String) {
    IDLE("প্রস্তুত আছি", "Idle"),
    LISTENING("শুনছি…", "Listening"),
    PROCESSING("চিন্তা করছি…", "Processing"),
    SPEAKING("কথা বলছি…", "Speaking"),
    EXECUTING("কাজ সম্পন্ন করছি…", "Executing"),
    ERROR("ত্রুটি হয়েছে", "Error")
}

enum class CompanionPose(val labelBn: String, val labelEn: String) {
    IDLE("তোমার সঙ্গেই আছি", "With you"),
    THINKING("চিন্তা করছি…", "Thinking…"),
    SPEAKING("কথা বলছি…", "Speaking…"),
    LISTENING("শুনছি…", "Listening…"),
    WAVING("হাত নাড়ছি", "Waving"),
    SITTING("বসে আছি", "Sitting"),
    HAPPY("খুশি আছি", "Happy")
}

enum class YouTubeAction {
    OPEN_FIRST_VIDEO,
    PLAY,
    PAUSE,
    RESUME,
    NEXT,
    PREVIOUS,
    SCROLL_DOWN,
    SCROLL_UP,
    SPEED_2X,
    SPEED_1_5X,
    SPEED_0_5X,
    SEEK_2_MIN,
    FULLSCREEN_ON,
    FULLSCREEN_OFF,
    MUTE,
    UNMUTE,
    OPEN_COMMENTS,
    OPEN_DESCRIPTION
}

enum class PhoneSystemAction {
    OPEN_WIFI_SETTINGS,
    OPEN_BLUETOOTH_SETTINGS,
    OPEN_SYSTEM_SETTINGS,
    OPEN_NOTES,
    SCROLL_DOWN,
    SCROLL_UP,
    NAVIGATE_BACK,
    CLOSE_CURRENT_APP,
    VOLUME_UP,
    VOLUME_DOWN,
    BRIGHTNESS_DOWN,
    BRIGHTNESS_UP
}

enum class MessagingChannel {
    WHATSAPP,
    SMS
}

sealed interface ParsedCommand {
    data object WakeAcknowledge : ParsedCommand
    data class OpenApp(val appName: String) : ParsedCommand
    data class SearchYouTube(val query: String) : ParsedCommand
    data class YouTubeControl(val action: YouTubeAction) : ParsedCommand
    data class PhoneControl(val action: PhoneSystemAction) : ParsedCommand
    data class SearchWeb(val query: String) : ParsedCommand
    data class StartMessagingFlow(
        val channel: MessagingChannel,
        val recipient: String?,
        val message: String?
    ) : ParsedCommand
    data class StartCallFlow(val target: String?) : ParsedCommand
    data object OpenVision : ParsedCommand
    data class CompanionGesture(val gesture: CompanionPose, val responseText: String) : ParsedCommand
}

object WakeWordMatcher {
    private val wakePatterns = listOf(
        Regex("""^(?:hey|hello|hi)\s+tdb[,\s।!?.:-]*(.*)$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:হ্যালো|হাই|এই|ওহে)\s*(?:টিডিবি|তিদিবি|tdb)[,\s।!?.:-]*(.*)$""", RegexOption.IGNORE_CASE),
        Regex("""^(?:tdb|টিডিবি)[,\s।!?.:-]+(.*)$""", RegexOption.IGNORE_CASE)
    )

    fun extractAfterWakeWord(transcript: String): Pair<Boolean, String> {
        val trimmed = transcript.trim()
        if (trimmed.isEmpty()) return false to ""
        for (pattern in wakePatterns) {
            val match = pattern.matchEntire(trimmed)
            if (match != null) {
                return true to match.groupValues[1].trim()
            }
        }
        if (trimmed.equals("tdb", ignoreCase = true) || trimmed == "টিডিবি") {
            return true to ""
        }
        return false to trimmed
    }
}

object MobileCommandParser {
    private val politeRegex = Regex(
        """^(?:please|can you|could you|দয়া করে|একটু)\s+""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val ytSearch1 = Regex(
        """^(?:open\s+)?(?:youtube|you tube)(?:\s+and)?\s+search(?:\s+for)?\s+(.+)$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val ytSearch2 = Regex(
        """^search(?:\s+for)?\s+(.+?)\s+on\s+youtube$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val ytSearchBn = Regex(
        """^(?:youtube|ইউটিউব)(?:-এ|\s+এ|\s+তে)?\s+(.+?)\s+(?:সার্চ করো|খোঁজো|search করো|দেখাও)[।.!?]*$""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val webSearch1 = Regex(
        """^(?:open\s+)?(?:chrome|google chrome|browser)(?:\s+and)?\s+search(?:\s+for)?\s+(.+)$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val webSearch2 = Regex(
        """^search(?:\s+for)?\s+(.+?)\s+(?:on|in)\s+(?:chrome|google|browser|web)$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val webSearchBn = Regex(
        """^(?:গুগলে|ক্রোমে|ওয়েবে|chrome-এ)\s+(.+?)\s+(?:সার্চ করো|খোঁজো)[।.!?]*$""",
        setOf(RegexOption.IGNORE_CASE)
    )

    private val waDirectRegex = Regex(
        """^(?:message|msg|text|send)\s+(.+?)\s+(?:on|using)\s+(?:whatsapp|wp|wa)\s*:\s*(.+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val waDirectRegex2 = Regex(
        """^(?:whatsapp|wp|wa)\s+(?:message|msg)(?:\s+to)?\s+(.+?)\s*:\s*(.+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val waNumberBn = Regex(
        """^এই নম্বরে\s+whatsapp\s+(?:মেসেজ|message)\s+পাঠাও\s*[:：]\s*(.+)$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val smsDirectRegex = Regex(
        """^(?:sms|text)\s+(?:to\s+)?(.+?)\s*:\s*(.+)$""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    private val openAppEn = Regex(
        """^(?:open|launch)\s+([\p{L}\p{N}][\p{L}\p{N} ._-]{0,79})$""",
        setOf(RegexOption.IGNORE_CASE)
    )
    private val openAppBn = Regex(
        """^([\p{L}\p{N} ._-]{1,50}?)\s+(?:খোলো|চালু করো|open করো|kholo|open karo)[।.!?]*$""",
        setOf(RegexOption.IGNORE_CASE)
    )

    fun parse(rawInput: String): ParsedCommand? {
        val (woke, strippedWake) = WakeWordMatcher.extractAfterWakeWord(rawInput)
        if (woke && strippedWake.isEmpty()) {
            return ParsedCommand.WakeAcknowledge
        }
        var text = (if (woke) strippedWake else rawInput.trim())
        text = politeRegex.replace(text, "").trim()
        if (text.isEmpty()) return null

        val lower = text.lowercase().trimEnd('.', '!', '?', '।')

        // Vision / Camera commands
        if (lower.contains("সামনে কী আছে") ||
            lower.contains("ক্যামেরা") && lower.contains("দেখ") ||
            lower == "open camera" ||
            lower == "open vision"
        ) {
            return ParsedCommand.OpenVision
        }

        // Companion gestures
        when (lower) {
            "wave", "wave hand", "হাত নাড়ো", "হাই বলো" ->
                return ParsedCommand.CompanionGesture(CompanionPose.WAVING, "হাত নেড়ে শুভেচ্ছা জানালাম!")
            "sit", "sit down", "বসো" ->
                return ParsedCommand.CompanionGesture(CompanionPose.SITTING, "আরাম করে বসলাম।")
            "stand", "stand up", "দাঁড়াও" ->
                return ParsedCommand.CompanionGesture(CompanionPose.IDLE, "সোজা হয়ে দাঁড়ালাম।")
            "smile", "happy", "হাসো", "হাসি" ->
                return ParsedCommand.CompanionGesture(CompanionPose.HAPPY, "হাসিমুখে তোমার পাশেই আছি!")
        }

        // Phone system controls (Bangla + English)
        when {
            lower.contains("wi-fi") || lower.contains("wifi") || lower.contains("ওয়াই-ফাই") ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.OPEN_WIFI_SETTINGS)
            lower.contains("bluetooth") || lower.contains("ব্লুটুথ") ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.OPEN_BLUETOOTH_SETTINGS)
            lower == "settings" || lower == "open settings" || lower == "সেটিংস খোলো" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.OPEN_SYSTEM_SETTINGS)
            lower == "নোটস খোলো" || lower == "open notes" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.OPEN_NOTES)
            lower.contains("আগের স্ক্রিনে ফিরে") || lower == "go back" || lower == "back" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.NAVIGATE_BACK)
            lower.contains("এই অ্যাপটা বন্ধ") || lower == "close app" || lower == "go home" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.CLOSE_CURRENT_APP)
            lower.contains("volume বাড়াও") || lower == "volume up" || lower == "increase volume" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.VOLUME_UP)
            lower.contains("volume কমাও") || lower == "volume down" || lower == "decrease volume" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.VOLUME_DOWN)
            lower.contains("brightness কমাও") || lower == "brightness down" || lower == "decrease brightness" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.BRIGHTNESS_DOWN)
            lower.contains("brightness বাড়াও") || lower == "brightness up" || lower == "increase brightness" ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.BRIGHTNESS_UP)
        }

        // YouTube specific controls
        when {
            lower.contains("প্রথম ভিডিও") || lower.contains("open first video") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.OPEN_FIRST_VIDEO)
            lower.contains("play করো") || lower == "play video" || lower == "play" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.PLAY)
            lower.contains("pause করো") || lower == "pause video" || lower == "pause" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.PAUSE)
            lower.contains("resume করো") || lower == "resume video" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.RESUME)
            lower.contains("পরবর্তী ভিডিও") || lower == "next video" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.NEXT)
            lower.contains("আগের ভিডিও") || lower == "previous video" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.PREVIOUS)
            lower.contains("2x speed") || lower.contains("২x") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.SPEED_2X)
            lower.contains("1.5x speed") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.SPEED_1_5X)
            lower.contains("0.5x") || lower.contains("slow motion") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.SPEED_0_5X)
            lower.contains("২ মিনিটে যাও") || lower.contains("2 minute") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.SEEK_2_MIN)
            lower.contains("full screen বন্ধ") || lower == "exit full screen" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.FULLSCREEN_OFF)
            lower.contains("full screen") || lower == "fullscreen" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.FULLSCREEN_ON)
            lower.contains("শব্দ বন্ধ") || lower == "mute video" || lower == "mute" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.MUTE)
            lower.contains("শব্দ চালু") || lower == "unmute video" || lower == "unmute" ->
                return ParsedCommand.YouTubeControl(YouTubeAction.UNMUTE)
            lower.contains("কমেন্ট সেকশন") || lower.contains("comments") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.OPEN_COMMENTS)
            lower.contains("description") || lower.contains("ডেসক্রিপশন") ->
                return ParsedCommand.YouTubeControl(YouTubeAction.OPEN_DESCRIPTION)
            lower.contains("scroll down") || lower.contains("নিচে নামাও") || lower.contains("নিচে স্ক্রল") ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.SCROLL_DOWN)
            lower.contains("scroll up") || lower.contains("উপরে স্ক্রল") || lower.contains("উপরে scroll") ->
                return ParsedCommand.PhoneControl(PhoneSystemAction.SCROLL_UP)
        }

        // YouTube & Web Searches
        ytSearch1.matchEntire(text)?.let { return ParsedCommand.SearchYouTube(it.groupValues[1].trim()) }
        ytSearch2.matchEntire(text)?.let { return ParsedCommand.SearchYouTube(it.groupValues[1].trim()) }
        ytSearchBn.matchEntire(text)?.let { return ParsedCommand.SearchYouTube(it.groupValues[1].trim()) }

        webSearch1.matchEntire(text)?.let { return ParsedCommand.SearchWeb(it.groupValues[1].trim()) }
        webSearch2.matchEntire(text)?.let { return ParsedCommand.SearchWeb(it.groupValues[1].trim()) }
        webSearchBn.matchEntire(text)?.let { return ParsedCommand.SearchWeb(it.groupValues[1].trim()) }

        // Messaging flows
        waDirectRegex.matchEntire(text)?.let {
            return ParsedCommand.StartMessagingFlow(
                MessagingChannel.WHATSAPP,
                it.groupValues[1].trim(),
                it.groupValues[2].trim()
            )
        }
        waDirectRegex2.matchEntire(text)?.let {
            return ParsedCommand.StartMessagingFlow(
                MessagingChannel.WHATSAPP,
                it.groupValues[1].trim(),
                it.groupValues[2].trim()
            )
        }
        waNumberBn.matchEntire(text)?.let {
            return ParsedCommand.StartMessagingFlow(
                MessagingChannel.WHATSAPP,
                it.groupValues[1].trim(),
                null
            )
        }
        smsDirectRegex.matchEntire(text)?.let {
            return ParsedCommand.StartMessagingFlow(
                MessagingChannel.SMS,
                it.groupValues[1].trim(),
                it.groupValues[2].trim()
            )
        }
        if (lower.contains("মেসেজ পাঠাও") || lower.contains("send whatsapp") || lower.contains("send sms")) {
            val channel = if (lower.contains("sms")) MessagingChannel.SMS else MessagingChannel.WHATSAPP
            val guessedRecipient = when {
                lower.contains("বন্ধু") -> "বন্ধু"
                else -> null
            }
            return ParsedCommand.StartMessagingFlow(channel, guessedRecipient, null)
        }

        // Phone call flows
        if (lower.startsWith("call ") || lower.contains("কল করো") || lower.contains("ফোন করো") || lower.contains("ডায়ালার খোলো")) {
            val target = text
                .replace(Regex("""^(?:call|কল করো|ফোন করো)\s*""", RegexOption.IGNORE_CASE), "")
                .replace(Regex("""(?:কে|কে কল করো|ডায়ালার খোলো)"""), "")
                .trim()
                .takeIf { it.isNotEmpty() && it != "ডায়ালার" }
            return ParsedCommand.StartCallFlow(target)
        }

        // Open installed app
        val cleanedOpen = text.trimEnd('.', '!', '?', '।')
        openAppEn.matchEntire(cleanedOpen)?.let {
            val target = it.groupValues[1].trim()
            if (!target.contains(Regex("""\band\b""", RegexOption.IGNORE_CASE))) {
                return ParsedCommand.OpenApp(target)
            }
        }
        openAppBn.matchEntire(cleanedOpen)?.let {
            val target = it.groupValues[1].trim()
            if (target.isNotEmpty()) {
                return ParsedCommand.OpenApp(target)
            }
        }

        return null
    }
}
