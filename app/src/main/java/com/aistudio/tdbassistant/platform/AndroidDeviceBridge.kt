package com.aistudio.tdbassistant.platform

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Process
import android.os.StatFs
import android.provider.ContactsContract
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.KeyEvent
import androidx.core.content.ContextCompat
import com.aistudio.tdbassistant.automation.TdbAccessibilityService
import com.aistudio.tdbassistant.data.preferences.VoiceStyle
import com.aistudio.tdbassistant.domain.PhoneSystemAction
import com.aistudio.tdbassistant.domain.YouTubeAction
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

sealed interface DeviceActionResult {
    data class Success(val message: String, val badge: String) : DeviceActionResult
    data class NeedsContactsPermission(
        val recipient: String,
        val message: String?,
        val isCall: Boolean = false
    ) : DeviceActionResult
    data class Failure(val message: String) : DeviceActionResult
}

data class AppUsageEntry(
    val appName: String,
    val packageName: String,
    val minutes: Long
)

data class UsageSummaryReport(
    val hasPermission: Boolean,
    val days: Int,
    val totalMinutes: Long,
    val topApps: List<AppUsageEntry>,
    val summaryPromptText: String
)

data class InstalledAppItem(
    val label: String,
    val packageName: String
)

data class StorageSummary(
    val totalGb: Float,
    val freeGb: Float,
    val usedPercent: Int
)

data class TtsEngineInfo(
    val label: String,
    val packageName: String
)

class AndroidDeviceBridge(private val context: Context) {

    fun openApp(inputName: String): DeviceActionResult {
        val normalized = inputName.trim().lowercase(Locale.ROOT)
        if (normalized.isEmpty()) {
            return DeviceActionResult.Failure("কোন অ্যাপটি খুলতে হবে তা লিখুন বা বলুন।")
        }

        val pm = context.packageManager
        val intent: Intent? = when (normalized) {
            "wp", "wa", "whats app", "whatsapp", "হোয়াটসঅ্যাপ" -> {
                val pkg = findInstalledWhatsAppPackage()
                    ?: return DeviceActionResult.Failure("এই ডিভাইসে WhatsApp ইনস্টল করা নেই।")
                pm.getLaunchIntentForPackage(pkg)
                    ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/")).setPackage(pkg)
            }
            "youtube", "you tube", "ইউটিউব" -> {
                val pkg = when {
                    isInstalled("com.google.android.youtube") -> "com.google.android.youtube"
                    isInstalled("com.android.chrome") -> "com.android.chrome"
                    else -> null
                }
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    if (pkg != null) setPackage(pkg)
                }
            }
            "chrome", "google chrome", "browser", "ক্রোম" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                    if (isInstalled("com.android.chrome")) setPackage("com.android.chrome")
                }
            }
            "settings", "সেটিংস" -> Intent(Settings.ACTION_SETTINGS)
            "notes", "নোটস" -> findLauncherAppIntent("note") ?: findLauncherAppIntent("keep")
            else -> findLauncherAppIntent(normalized)
        }

        if (intent == null) {
            return DeviceActionResult.Failure("\"$inputName\" নামের কোনো ইনস্টল করা অ্যাপ পাওয়া যায়নি।")
        }

        return if (launchIntent(intent)) {
            DeviceActionResult.Success("$inputName খোলা হয়েছে।", "Opened $inputName")
        } else {
            DeviceActionResult.Failure("$inputName অ্যাপটি খোলা যায়নি।")
        }
    }

    fun performPhoneControl(action: PhoneSystemAction): DeviceActionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val acc = TdbAccessibilityService.current()

        return when (action) {
            PhoneSystemAction.OPEN_WIFI_SETTINGS -> {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    Intent(Settings.Panel.ACTION_WIFI)
                } else {
                    Intent(Settings.ACTION_WIFI_SETTINGS)
                }
                if (launchIntent(intent) || launchIntent(Intent(Settings.ACTION_WIFI_SETTINGS))) {
                    DeviceActionResult.Success("Wi-Fi সেটিংস খোলা হয়েছে।", "Wi-Fi Settings")
                } else {
                    DeviceActionResult.Failure("Wi-Fi সেটিংস খোলা যায়নি।")
                }
            }
            PhoneSystemAction.OPEN_BLUETOOTH_SETTINGS -> {
                if (launchIntent(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))) {
                    DeviceActionResult.Success("Bluetooth সেটিংস খোলা হয়েছে।", "Bluetooth Settings")
                } else {
                    DeviceActionResult.Failure("Bluetooth সেটিংস খোলা যায়নি।")
                }
            }
            PhoneSystemAction.OPEN_SYSTEM_SETTINGS -> {
                if (launchIntent(Intent(Settings.ACTION_SETTINGS))) {
                    DeviceActionResult.Success("ফোনের Settings খোলা হয়েছে।", "Settings")
                } else {
                    DeviceActionResult.Failure("Settings খোলা যায়নি।")
                }
            }
            PhoneSystemAction.OPEN_NOTES -> openApp("notes")
            PhoneSystemAction.VOLUME_UP -> {
                audio?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_SHOW_UI
                )
                DeviceActionResult.Success("ফোনের Volume বাড়ানো হয়েছে।", "Volume +")
            }
            PhoneSystemAction.VOLUME_DOWN -> {
                audio?.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_SHOW_UI
                )
                DeviceActionResult.Success("ফোনের Volume কমানো হয়েছে।", "Volume -")
            }
            PhoneSystemAction.BRIGHTNESS_DOWN, PhoneSystemAction.BRIGHTNESS_UP -> {
                if (Settings.System.canWrite(context)) {
                    val current = runCatching {
                        Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
                    }.getOrDefault(128)
                    val delta = if (action == PhoneSystemAction.BRIGHTNESS_UP) 45 else -45
                    val next = (current + delta).coerceIn(20, 255)
                    Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, next)
                    DeviceActionResult.Success("স্ক্রিনের Brightness সমন্বয় করা হয়েছে।", "Brightness")
                } else {
                    launchIntent(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                    DeviceActionResult.Success(
                        "সিস্টেম Brightness পরিবর্তনের জন্য Display Settings খোলা হয়েছে।",
                        "Display Settings"
                    )
                }
            }
            PhoneSystemAction.SCROLL_DOWN -> {
                if (acc == null) {
                    DeviceActionResult.Failure("স্ক্রল করতে Settings > Phone Automation থেকে TDB Accessibility Service চালু করুন।")
                } else if (acc.performScroll(down = true)) {
                    DeviceActionResult.Success("স্ক্রিন নিচে স্ক্রল করা হয়েছে।", "Scroll Down")
                } else {
                    DeviceActionResult.Failure("বর্তমান স্ক্রিনে স্ক্রল করা যায়নি।")
                }
            }
            PhoneSystemAction.SCROLL_UP -> {
                if (acc == null) {
                    DeviceActionResult.Failure("স্ক্রল করতে Settings > Phone Automation থেকে TDB Accessibility Service চালু করুন।")
                } else if (acc.performScroll(down = false)) {
                    DeviceActionResult.Success("স্ক্রিন উপরে স্ক্রল করা হয়েছে।", "Scroll Up")
                } else {
                    DeviceActionResult.Failure("বর্তমান স্ক্রিনে স্ক্রল করা যায়নি।")
                }
            }
            PhoneSystemAction.NAVIGATE_BACK -> {
                if (acc == null) {
                    DeviceActionResult.Failure("আগের স্ক্রিনে ফিরতে TDB Accessibility Service চালু করুন।")
                } else if (acc.performBack()) {
                    DeviceActionResult.Success("আগের স্ক্রিনে ফিরে যাওয়া হয়েছে।", "Back")
                } else {
                    DeviceActionResult.Failure("Back অ্যাকশন কাজ করেনি।")
                }
            }
            PhoneSystemAction.CLOSE_CURRENT_APP -> {
                if (acc == null) {
                    DeviceActionResult.Failure("অ্যাপ বন্ধ করে Home-এ যেতে TDB Accessibility Service চালু করুন।")
                } else if (acc.performHome()) {
                    DeviceActionResult.Success("হোম স্ক্রিনে ফিরে যাওয়া হয়েছে।", "Home")
                } else {
                    DeviceActionResult.Failure("Home অ্যাকশন সম্পন্ন করা যায়নি।")
                }
            }
        }
    }

    fun performYouTubeAction(action: YouTubeAction): DeviceActionResult {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val acc = TdbAccessibilityService.current()

        return when (action) {
            YouTubeAction.PLAY, YouTubeAction.RESUME -> {
                dispatchMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PLAY)
                DeviceActionResult.Success("মিডিয়া Play/Resume সিগন্যাল পাঠানো হয়েছে।", "YouTube Play")
            }
            YouTubeAction.PAUSE -> {
                dispatchMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PAUSE)
                DeviceActionResult.Success("মিডিয়া Pause সিগন্যাল পাঠানো হয়েছে।", "YouTube Pause")
            }
            YouTubeAction.NEXT -> {
                dispatchMediaKey(audio, KeyEvent.KEYCODE_MEDIA_NEXT)
                DeviceActionResult.Success("পরবর্তী ভিডিওর জন্য Next সিগন্যাল পাঠানো হয়েছে।", "Next Video")
            }
            YouTubeAction.PREVIOUS -> {
                dispatchMediaKey(audio, KeyEvent.KEYCODE_MEDIA_PREVIOUS)
                DeviceActionResult.Success("আগের ভিডিওর জন্য Previous সিগন্যাল পাঠানো হয়েছে।", "Prev Video")
            }
            YouTubeAction.MUTE -> {
                audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI)
                DeviceActionResult.Success("ভিডিওর শব্দ Mute করা হয়েছে।", "Muted")
            }
            YouTubeAction.UNMUTE -> {
                audio?.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, AudioManager.FLAG_SHOW_UI)
                DeviceActionResult.Success("ভিডিওর শব্দ চালু করা হয়েছে।", "Unmuted")
            }
            YouTubeAction.SCROLL_DOWN -> performPhoneControl(PhoneSystemAction.SCROLL_DOWN)
            YouTubeAction.SCROLL_UP -> performPhoneControl(PhoneSystemAction.SCROLL_UP)
            YouTubeAction.OPEN_FIRST_VIDEO -> {
                if (acc == null) {
                    DeviceActionResult.Failure("প্রথম ভিডিও খুলতে TDB Accessibility Service চালু করুন।")
                } else if (acc.tryOpenFirstYouTubeVideo()) {
                    DeviceActionResult.Success("দৃশ্যমান প্রথম ভিডিওটি খোলা হয়েছে।", "Open Video")
                } else {
                    DeviceActionResult.Failure("YouTube-এ কোনো ক্লিকযোগ্য ভিডিও কার্ড খুঁজে পাওয়া যায়নি।")
                }
            }
            YouTubeAction.FULLSCREEN_ON, YouTubeAction.FULLSCREEN_OFF -> {
                if (acc == null) {
                    DeviceActionResult.Failure("Full Screen নিয়ন্ত্রণ করতে TDB Accessibility Service চালু করুন।")
                } else {
                    val keywords = if (action == YouTubeAction.FULLSCREEN_ON) {
                        listOf("enter full screen", "fullscreen", "full screen")
                    } else {
                        listOf("exit full screen", "minimize", "full screen")
                    }
                    if (acc.tryClickByTextOrDescription(keywords)) {
                        DeviceActionResult.Success("Full Screen বাটনে ট্যাপ করা হয়েছে।", "Full Screen")
                    } else {
                        DeviceActionResult.Failure("পর্দায় Full Screen বাটন খুঁজে পাওয়া যায়নি।")
                    }
                }
            }
            YouTubeAction.OPEN_COMMENTS -> {
                if (acc == null) {
                    DeviceActionResult.Failure("কমেন্ট খুলতে TDB Accessibility Service চালু করুন।")
                } else if (acc.tryClickByTextOrDescription(listOf("comments", "comment"))) {
                    DeviceActionResult.Success("Comments সেকশন খোলা হয়েছে।", "Comments")
                } else {
                    DeviceActionResult.Failure("YouTube পর্দায় Comments বাটন দৃশ্যমান নয়।")
                }
            }
            YouTubeAction.OPEN_DESCRIPTION -> {
                if (acc == null) {
                    DeviceActionResult.Failure("Description খুলতে TDB Accessibility Service চালু করুন।")
                } else if (acc.tryClickByTextOrDescription(listOf("more", "description"))) {
                    DeviceActionResult.Success("Video description খোলা হয়েছে।", "Description")
                } else {
                    DeviceActionResult.Failure("YouTube পর্দায় Description বাটন পাওয়া যায়নি।")
                }
            }
            YouTubeAction.SPEED_2X, YouTubeAction.SPEED_1_5X, YouTubeAction.SPEED_0_5X -> {
                val targetSpeed = when (action) {
                    YouTubeAction.SPEED_2X -> "2x"
                    YouTubeAction.SPEED_1_5X -> "1.5x"
                    else -> "0.5x"
                }
                if (acc == null) {
                    DeviceActionResult.Failure("Playback speed ($targetSpeed) পরিবর্তনের জন্য TDB Accessibility Service চালু করুন।")
                } else if (acc.tryClickByTextOrDescription(listOf("playback speed", "settings", targetSpeed))) {
                    DeviceActionResult.Success("YouTube Playback Settings মেনু খোলা হয়েছে ($targetSpeed)।", "Speed $targetSpeed")
                } else {
                    DeviceActionResult.Failure("YouTube প্লেয়ার কন্ট্রোল দৃশ্যমান থাকলে $targetSpeed সেটিং খোলা যাবে; এখন পাওয়া যায়নি।")
                }
            }
            YouTubeAction.SEEK_2_MIN -> {
                DeviceActionResult.Failure("YouTube অ্যাপের ভেতর নির্দিষ্ট মিনিটে সরাসরি জাম্প করার পাবলিক Intent/MediaSession API অনুমোদিত নয়।")
            }
        }
    }

    private fun dispatchMediaKey(audio: AudioManager?, keyCode: Int) {
        audio ?: return
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audio.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    fun searchYouTube(query: String): DeviceActionResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return DeviceActionResult.Failure("YouTube-এ কী খুঁজবেন তা লিখুন।")
        val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        val uri = Uri.parse("https://www.youtube.com/results?search_query=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (isInstalled("com.google.android.youtube")) setPackage("com.google.android.youtube")
            else if (isInstalled("com.android.chrome")) setPackage("com.android.chrome")
        }
        return if (launchIntent(intent) || launchIntent(Intent(Intent.ACTION_VIEW, uri))) {
            DeviceActionResult.Success("YouTube-এ \"$trimmed\" সার্চ খোলা হয়েছে।", "YouTube: $trimmed")
        } else {
            DeviceActionResult.Failure("YouTube বা ব্রাউজার খোলা যায়নি।")
        }
    }

    fun searchWeb(query: String): DeviceActionResult {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return DeviceActionResult.Failure("ওয়েবে কী খুঁজবেন তা লিখুন।")
        val encoded = URLEncoder.encode(trimmed, StandardCharsets.UTF_8.name())
        val uri = Uri.parse("https://www.google.com/search?q=$encoded")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            if (isInstalled("com.android.chrome")) setPackage("com.android.chrome")
        }
        return if (launchIntent(intent) || launchIntent(Intent(Intent.ACTION_VIEW, uri))) {
            DeviceActionResult.Success("ব্রাউজারে \"$trimmed\" সার্চ খোলা হয়েছে।", "Web: $trimmed")
        } else {
            DeviceActionResult.Failure("সার্চ খোলার জন্য উপযুক্ত ব্রাউজার পাওয়া যায়নি।")
        }
    }

    fun openUrlInBrowser(rawUrl: String): Boolean {
        val clean = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        return launchIntent(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
    }

    fun resolveRecipientPhone(recipient: String, messageForPermission: String?, isCall: Boolean): Pair<DeviceActionResult?, Pair<String, String>?> {
        val cleanRecipient = recipient.trim()
        val phoneRegex = Regex("""^\+?[0-9 ()-]+$""")
        if (phoneRegex.matches(cleanRecipient)) {
            var digits = cleanRecipient.replace(Regex("[ ()+-]"), "")
            if (digits.startsWith("00")) digits = digits.substring(2)
            if (digits.length < 6 || digits.length > 15) {
                return DeviceActionResult.Failure("সঠিক ফোন নম্বর দিন (যেমন +880... বা +91...)।") to null
            }
            val dialFormat = if (cleanRecipient.startsWith("+")) "+$digits" else digits
            return null to (cleanRecipient to dialFormat)
        }

        val granted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            return DeviceActionResult.NeedsContactsPermission(
                recipient = cleanRecipient,
                message = messageForPermission,
                isCall = isCall
            ) to null
        }

        val match = findContactPhone(cleanRecipient)
            ?: return DeviceActionResult.Failure("\"$cleanRecipient\" নামে কোনো সংরক্ষিত কন্টাক্ট নম্বর পাওয়া যায়নি।") to null
        return null to match
    }

    fun prepareWhatsAppDraft(recipient: String, message: String): DeviceActionResult {
        val waPkg = findInstalledWhatsAppPackage()
            ?: return DeviceActionResult.Failure("এই ডিভাইসে WhatsApp ইনস্টল করা নেই।")
        if (recipient.isBlank() || message.isBlank()) {
            return DeviceActionResult.Failure("প্রাপক ও বার্তা উভয়ই প্রয়োজন।")
        }
        val (err, resolved) = resolveRecipientPhone(recipient, message, isCall = false)
        if (err != null) return err
        val (displayTarget, phone) = resolved!!

        val digitsOnly = phone.replace("+", "")
        val encodedMsg = URLEncoder.encode(message.trim(), StandardCharsets.UTF_8.name())
        val uri = Uri.parse("https://wa.me/$digitsOnly?text=$encodedMsg")
        val intent = Intent(Intent.ACTION_VIEW, uri).setPackage(waPkg)
        return if (launchIntent(intent) || launchIntent(Intent(Intent.ACTION_VIEW, uri))) {
            DeviceActionResult.Success(
                "$displayTarget-এর জন্য WhatsApp খোলা হয়েছে। অনুগ্রহ করে যাচাই করে Send চাপুন।",
                "WhatsApp: $displayTarget"
            )
        } else {
            DeviceActionResult.Failure("WhatsApp খসড়া খোলা যায়নি।")
        }
    }

    fun prepareSmsDraft(recipient: String, message: String): DeviceActionResult {
        if (recipient.isBlank() || message.isBlank()) {
            return DeviceActionResult.Failure("প্রাপক ও SMS বার্তা প্রয়োজন।")
        }
        val (err, resolved) = resolveRecipientPhone(recipient, message, isCall = false)
        if (err != null) return err
        val (displayTarget, phone) = resolved!!

        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")).apply {
            putExtra("sms_body", message.trim())
        }
        return if (launchIntent(intent)) {
            DeviceActionResult.Success(
                "$displayTarget-এর জন্য SMS অ্যাপ খোলা হয়েছে। পাঠাতে Send চাপুন।",
                "SMS: $displayTarget"
            )
        } else {
            DeviceActionResult.Failure("এই ডিভাইসে SMS অ্যাপ পাওয়া যায়নি।")
        }
    }

    fun openPhoneDialer(target: String?): DeviceActionResult {
        if (target.isNullOrBlank()) {
            val ok = launchIntent(Intent(Intent.ACTION_DIAL))
            return if (ok) {
                DeviceActionResult.Success("ফোন ডায়ালার খোলা হয়েছে।", "Dialer")
            } else {
                DeviceActionResult.Failure("ফোন ডায়ালার খোলা যায়নি।")
            }
        }
        val (err, resolved) = resolveRecipientPhone(target, null, isCall = true)
        if (err != null) return err
        val (displayTarget, phone) = resolved!!
        val ok = launchIntent(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
        return if (ok) {
            DeviceActionResult.Success(
                "$displayTarget ($phone)-এর নম্বর ডায়ালারে প্রস্তুত করা হয়েছে। কল করতে Call বাটনে চাপুন।",
                "Dial: $displayTarget"
            )
        } else {
            DeviceActionResult.Failure("ফোন ডায়ালার খোলা যায়নি।")
        }
    }

    fun getInstalledApps(): List<InstalledAppItem> {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(mainIntent, 0)
            .mapNotNull { info ->
                val pkg = info.activityInfo?.packageName ?: return@mapNotNull null
                val label = info.loadLabel(pm).toString()
                InstalledAppItem(label = label, packageName = pkg)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    fun getStorageSummary(): StorageSummary {
        val stat = StatFs(Environment.getDataDirectory().path)
        val total = stat.totalBytes.toDouble()
        val avail = stat.availableBytes.toDouble()
        val gb = 1024.0 * 1024.0 * 1024.0
        val totalGb = (total / gb).toFloat()
        val freeGb = (avail / gb).toFloat()
        val usedPct = if (total > 0) (((total - avail) / total) * 100.0).toInt().coerceIn(0, 100) else 0
        return StorageSummary(totalGb = totalGb, freeGb = freeGb, usedPercent = usedPct)
    }

    fun getUsageSummary(days: Int = 1): UsageSummaryReport {
        val hasAccess = hasUsageStatsPermission()
        if (!hasAccess) {
            return UsageSummaryReport(false, days, 0L, emptyList(), "")
        }

        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return UsageSummaryReport(true, days, 0L, emptyList(), "")

        val end = System.currentTimeMillis()
        val start = end - days * 24L * 60L * 60L * 1000L
        val interval = when {
            days >= 28 -> UsageStatsManager.INTERVAL_MONTHLY
            days > 1 -> UsageStatsManager.INTERVAL_WEEKLY
            else -> UsageStatsManager.INTERVAL_DAILY
        }
        val stats = manager.queryUsageStats(interval, start, end).orEmpty()

        val pm = context.packageManager
        val aggregated = mutableMapOf<String, Long>()
        stats.forEach { stat ->
            if (stat.totalTimeInForeground >= 60_000L) {
                aggregated[stat.packageName] =
                    (aggregated[stat.packageName] ?: 0L) + stat.totalTimeInForeground
            }
        }

        val entries = aggregated.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { (pkg, ms) ->
                val label = runCatching {
                    val info = pm.getApplicationInfo(pkg, 0)
                    pm.getApplicationLabel(info).toString()
                }.getOrDefault(pkg.substringAfterLast('.'))
                AppUsageEntry(
                    appName = label,
                    packageName = pkg,
                    minutes = ms / 60_000L
                )
            }

        val totalMinutes = aggregated.values.sum() / 60_000L
        val windowLabel = when (days) {
            1 -> "আজ (গত ২৪ ঘণ্টায়)"
            7 -> "গত ৭ দিনে"
            else -> "গত ৩০ দিনে"
        }
        val promptSummary = buildString {
            val hrs = totalMinutes / 60
            val remMins = totalMinutes % 60
            appendLine("$windowLabel মোট আনুমানিক ফোন ব্যবহার: ${hrs} ঘণ্টা ${remMins} মিনিট ($totalMinutes মিনিট)।")
            entries.take(5).forEachIndexed { idx, app ->
                appendLine("${idx + 1}. ${app.appName}: ${app.minutes} মিনিট")
            }
        }.trim()

        return UsageSummaryReport(
            hasPermission = true,
            days = days,
            totalMinutes = totalMinutes,
            topApps = entries,
            summaryPromptText = promptSummary
        )
    }

    fun openUsageAccessSettings(): Boolean =
        launchIntent(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openAccessibilitySettings(): Boolean =
        launchIntent(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openNotificationListenerSettings(): Boolean =
        launchIntent(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))

    fun openAppSettings(): Boolean =
        launchIntent(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.packageName, null)
            )
        )

    fun isAccessibilityEnabled(): Boolean {
        if (TdbAccessibilityService.isConnected.value) return true
        val expected = ComponentName(context, TdbAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ).orEmpty()
        return enabled.contains(expected, ignoreCase = true)
    }

    fun shareText(title: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, body)
        }
        launchIntent(Intent.createChooser(intent, title))
    }

    fun shareDocumentUri(title: String, uri: Uri) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        launchIntent(Intent.createChooser(intent, title))
    }

    fun openDocumentUri(uri: Uri): Boolean {
        val mime = context.contentResolver.getType(uri) ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return launchIntent(intent)
    }

    private fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    private fun findInstalledWhatsAppPackage(): String? = when {
        isInstalled("com.whatsapp") -> "com.whatsapp"
        isInstalled("com.whatsapp.w4b") -> "com.whatsapp.w4b"
        else -> null
    }

    private fun isInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    }.getOrDefault(false)

    private fun findLauncherAppIntent(queryLower: String): Intent? {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(mainIntent, 0)
        val best = apps.firstOrNull { resolve ->
            val label = resolve.loadLabel(pm).toString().lowercase(Locale.ROOT)
            label == queryLower || label.contains(queryLower)
        } ?: return null
        return pm.getLaunchIntentForPackage(best.activityInfo.packageName)
    }

    private fun findContactPhone(query: String): Pair<String, String>? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val args = arrayOf("%$query%")
        context.contentResolver.query(uri, projection, selection, args, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val name = cursor.getString(0) ?: query
                val rawNum = cursor.getString(1) ?: return null
                val cleanDial = rawNum.replace(Regex("[^0-9+]"), "")
                if (cleanDial.length >= 6) {
                    return name to cleanDial
                }
            }
        }
        return null
    }

    private fun launchIntent(intent: Intent): Boolean = runCatching {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    }.getOrDefault(false)
}

class TdbSpeechController(
    private val context: Context,
    private val onSpeakingStateChanged: (Boolean) -> Unit,
    private val onTtsError: (String) -> Unit
) {
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var activeEnginePkg: String = ""
    private var speechRecognizer: SpeechRecognizer? = null

    init {
        initTts("")
    }

    private fun initTts(enginePackage: String) {
        tts?.stop()
        tts?.shutdown()
        ttsReady = false
        activeEnginePkg = enginePackage
        val listener = TextToSpeech.OnInitListener { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = onSpeakingStateChanged(true)
                    override fun onDone(utteranceId: String?) = onSpeakingStateChanged(false)
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = onSpeakingStateChanged(false)
                })
            }
        }
        tts = if (enginePackage.isBlank()) {
            TextToSpeech(context.applicationContext, listener)
        } else {
            TextToSpeech(context.applicationContext, listener, enginePackage)
        }
    }

    fun getAvailableEngines(): List<TtsEngineInfo> {
        val engine = tts ?: return emptyList()
        return engine.engines.map {
            TtsEngineInfo(label = it.label, packageName = it.name)
        }
    }

    fun speak(
        text: String,
        languageTag: String,
        voiceStyle: VoiceStyle,
        speed: Float = 1.0f,
        pitch: Float = 1.0f,
        volume: Float = 1.0f,
        enginePackage: String = ""
    ) {
        if (enginePackage != activeEnginePkg && enginePackage.isNotBlank()) {
            initTts(enginePackage)
        }
        val engine = tts
        if (!ttsReady || engine == null) {
            onTtsError("Android Text-to-Speech ইঞ্জিন এখনো প্রস্তুত নয়।")
            return
        }
        val locale = Locale.forLanguageTag(languageTag)
        val availability = engine.setLanguage(locale)
        if (availability == TextToSpeech.LANG_MISSING_DATA ||
            availability == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            onTtsError("এই ডিভাইসের TTS ইঞ্জিনে $languageTag ভাষা ইনস্টল নেই।")
            return
        }
        val basePitch = if (voiceStyle == VoiceStyle.MALE) 0.84f else 1.08f
        engine.setPitch((basePitch * pitch).coerceIn(0.5f, 1.8f))
        engine.setSpeechRate(speed.coerceIn(0.5f, 1.8f))

        val params = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0.1f, 1.0f))
        }
        val clean = text.replace(Regex("[*#`_]"), "").take(TextToSpeech.getMaxSpeechInputLength())
        val code = engine.speak(clean, TextToSpeech.QUEUE_FLUSH, params, "tdb_reply")
        if (code == TextToSpeech.ERROR) {
            onSpeakingStateChanged(false)
            onTtsError("কণ্ঠস্বর চালু করা যায়নি।")
        }
    }

    fun stopSpeaking() {
        tts?.stop()
        onSpeakingStateChanged(false)
    }

    fun startDictation(
        languageTag: String,
        onPartial: (String) -> Unit,
        onFinal: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            onError("এই ডিভাইসে ভয়েস ডিক্টেশন সার্ভিস পাওয়া যায়নি।")
            return
        }
        stopDictation()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "কথা বোঝা যায়নি, আবার বলুন।"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "কোনো শব্দ শোনা যায়নি।"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "মাইক্রোফোন অনুমতি প্রয়োজন।"
                    else -> "ভয়েস ইনপুট ব্যর্থ হয়েছে (কোড $error)।"
                }
                onError(msg)
            }

            override fun onResults(results: Bundle?) {
                val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) onFinal(text)
                else onError("কথা বোঝা যায়নি, আবার চেষ্টা করুন।")
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val text = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                    .orEmpty()
                if (text.isNotEmpty()) onPartial(text)
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        recognizer.startListening(intent)
    }

    fun stopDictation() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    fun release() {
        stopDictation()
        tts?.stop()
        tts?.shutdown()
        tts = null
    }
}
