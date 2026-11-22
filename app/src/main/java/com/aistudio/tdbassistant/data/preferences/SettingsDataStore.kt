package com.aistudio.tdbassistant.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tdbDataStore: DataStore<Preferences> by preferencesDataStore(name = "tdb_settings")

enum class TdbLanguage(val tag: String, val label: String, val shortLabel: String) {
    BANGLA("bn-BD", "বাংলা (প্রধান)", "বাংলা"),
    HINDI("hi-IN", "Hindi / Hinglish", "हिन्दी"),
    ENGLISH_IN("en-IN", "English (India)", "EN-IN"),
    ENGLISH_US("en-US", "English (US)", "EN-US");

    companion object {
        fun fromTag(tag: String?): TdbLanguage =
            entries.firstOrNull { it.tag == tag } ?: BANGLA
    }
}

enum class TdbModelOption(
    val modelId: String,
    val displayName: String,
    val summary: String
) {
    GEMINI_3_8_FLASH(
        modelId = "gemini-3.8-flash",
        displayName = "Gemini 3.8 Flash",
        summary = "Default · Fast, balanced Bangla & English companion"
    ),
    GEMINI_3_5_FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        displayName = "Gemini 3.5 Flash-Lite",
        summary = "Ultra-fast · Low latency everyday replies"
    ),
    GEMINI_4_ARGON(
        modelId = "gemini-4-argon",
        displayName = "Gemini 4 Argon",
        summary = "Deep reasoning · Complex planning & analysis"
    );

    companion object {
        fun fromId(id: String?): TdbModelOption =
            entries.firstOrNull { it.modelId == id } ?: GEMINI_3_8_FLASH
    }
}

enum class AiEngineMode(val id: String, val label: String, val description: String) {
    FIREBASE_AI_LOGIC(
        id = "firebase_ai_logic",
        label = "Firebase AI Logic (Built-in)",
        description = "No manual API key required · Protected by App Check"
    ),
    CUSTOM_API_KEY(
        id = "custom_api_key",
        label = "Personal Gemini API Key",
        description = "Uses your Android Keystore-encrypted Gemini API key"
    );

    companion object {
        fun fromId(id: String?): AiEngineMode =
            entries.firstOrNull { it.id == id } ?: FIREBASE_AI_LOGIC
    }
}

enum class AiResponseStyle(val id: String, val labelBn: String, val promptHint: String) {
    BALANCED("balanced", "সাবলীল ও ভারসাম্যপূর্ণ", "Warm, balanced, clear and practical responses."),
    CONCISE("concise", "সংক্ষিপ্ত ও দ্রুত", "Brief, direct, action-oriented replies."),
    DETAILED("detailed", "বিস্তারিত ও বিশ্লেষণধর্মী", "Thorough explanations with structured steps.");

    companion object {
        fun fromId(id: String?): AiResponseStyle =
            entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

enum class VoiceStyle(val id: String, val label: String) {
    FEMALE("female", "Female · নারী কণ্ঠ"),
    MALE("male", "Male · পুরুষ কণ্ঠ");

    companion object {
        fun fromId(id: String?): VoiceStyle =
            entries.firstOrNull { it.id == id } ?: FEMALE
    }
}

enum class MotionQuality(val id: String, val label: String) {
    LOW("low", "Low · Save battery"),
    BALANCED("balanced", "Balanced · Smooth"),
    HIGH("high", "High · Expressive");

    companion object {
        fun fromId(id: String?): MotionQuality =
            entries.firstOrNull { it.id == id } ?: BALANCED
    }
}

enum class ThemePreference(val id: String, val label: String) {
    SYSTEM("system", "System"),
    LIGHT("light", "Light Studio"),
    DARK("dark", "Dark Obsidian");

    companion object {
        fun fromId(id: String?): ThemePreference =
            entries.firstOrNull { it.id == id } ?: SYSTEM
    }
}

enum class AccentPalette(val id: String, val label: String) {
    EMERALD("emerald", "Deep Emerald"),
    CHAMPAGNE("champagne", "Champagne Gold"),
    ROYAL_TEAL("teal", "Nordic Teal");

    companion object {
        fun fromId(id: String?): AccentPalette =
            entries.firstOrNull { it.id == id } ?: EMERALD
    }
}

data class TdbSettings(
    val language: TdbLanguage = TdbLanguage.BANGLA,
    val model: TdbModelOption = TdbModelOption.GEMINI_3_8_FLASH,
    val aiEngineMode: AiEngineMode = AiEngineMode.FIREBASE_AI_LOGIC,
    val responseStyle: AiResponseStyle = AiResponseStyle.BALANCED,
    val voiceStyle: VoiceStyle = VoiceStyle.FEMALE,
    val voiceSpeed: Float = 1.0f,
    val voicePitch: Float = 1.0f,
    val speechVolume: Float = 1.0f,
    val ttsEnginePackage: String = "",
    val spokenReplies: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val motionQuality: MotionQuality = MotionQuality.BALANCED,
    val autoMemoryExtraction: Boolean = true,
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val accentPalette: AccentPalette = AccentPalette.EMERALD
)

class SettingsRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language_tag")
        val MODEL = stringPreferencesKey("model_id")
        val ENGINE_MODE = stringPreferencesKey("ai_engine_mode")
        val RESPONSE_STYLE = stringPreferencesKey("ai_response_style")
        val VOICE_STYLE = stringPreferencesKey("voice_style")
        val VOICE_SPEED = floatPreferencesKey("voice_speed")
        val VOICE_PITCH = floatPreferencesKey("voice_pitch")
        val SPEECH_VOLUME = floatPreferencesKey("speech_volume")
        val TTS_ENGINE = stringPreferencesKey("tts_engine_package")
        val SPOKEN_REPLIES = booleanPreferencesKey("spoken_replies")
        val WAKE_WORD = booleanPreferencesKey("wake_word_enabled")
        val MOTION_QUALITY = stringPreferencesKey("motion_quality")
        val AUTO_MEMORY = booleanPreferencesKey("auto_memory_extraction")
        val THEME_PREF = stringPreferencesKey("theme_preference")
        val ACCENT_PALETTE = stringPreferencesKey("accent_palette")
    }

    val settings: Flow<TdbSettings> = context.tdbDataStore.data.map { prefs ->
        TdbSettings(
            language = TdbLanguage.fromTag(prefs[Keys.LANGUAGE]),
            model = TdbModelOption.fromId(prefs[Keys.MODEL]),
            aiEngineMode = AiEngineMode.fromId(prefs[Keys.ENGINE_MODE]),
            responseStyle = AiResponseStyle.fromId(prefs[Keys.RESPONSE_STYLE]),
            voiceStyle = VoiceStyle.fromId(prefs[Keys.VOICE_STYLE]),
            voiceSpeed = (prefs[Keys.VOICE_SPEED] ?: 1.0f).coerceIn(0.6f, 1.6f),
            voicePitch = (prefs[Keys.VOICE_PITCH] ?: 1.0f).coerceIn(0.6f, 1.5f),
            speechVolume = (prefs[Keys.SPEECH_VOLUME] ?: 1.0f).coerceIn(0.2f, 1.0f),
            ttsEnginePackage = prefs[Keys.TTS_ENGINE] ?: "",
            spokenReplies = prefs[Keys.SPOKEN_REPLIES] ?: false,
            wakeWordEnabled = prefs[Keys.WAKE_WORD] ?: false,
            motionQuality = MotionQuality.fromId(prefs[Keys.MOTION_QUALITY]),
            autoMemoryExtraction = prefs[Keys.AUTO_MEMORY] ?: true,
            themePreference = ThemePreference.fromId(prefs[Keys.THEME_PREF]),
            accentPalette = AccentPalette.fromId(prefs[Keys.ACCENT_PALETTE])
        )
    }

    suspend fun setLanguage(language: TdbLanguage) {
        context.tdbDataStore.edit { it[Keys.LANGUAGE] = language.tag }
    }

    suspend fun setModel(model: TdbModelOption) {
        context.tdbDataStore.edit { it[Keys.MODEL] = model.modelId }
    }

    suspend fun setAiEngineMode(mode: AiEngineMode) {
        context.tdbDataStore.edit { it[Keys.ENGINE_MODE] = mode.id }
    }

    suspend fun setResponseStyle(style: AiResponseStyle) {
        context.tdbDataStore.edit { it[Keys.RESPONSE_STYLE] = style.id }
    }

    suspend fun setVoiceStyle(style: VoiceStyle) {
        context.tdbDataStore.edit { it[Keys.VOICE_STYLE] = style.id }
    }

    suspend fun setVoiceSpeed(speed: Float) {
        context.tdbDataStore.edit { it[Keys.VOICE_SPEED] = speed.coerceIn(0.6f, 1.6f) }
    }

    suspend fun setVoicePitch(pitch: Float) {
        context.tdbDataStore.edit { it[Keys.VOICE_PITCH] = pitch.coerceIn(0.6f, 1.5f) }
    }

    suspend fun setSpeechVolume(volume: Float) {
        context.tdbDataStore.edit { it[Keys.SPEECH_VOLUME] = volume.coerceIn(0.2f, 1.0f) }
    }

    suspend fun setTtsEnginePackage(pkg: String) {
        context.tdbDataStore.edit { it[Keys.TTS_ENGINE] = pkg }
    }

    suspend fun setSpokenReplies(enabled: Boolean) {
        context.tdbDataStore.edit { it[Keys.SPOKEN_REPLIES] = enabled }
    }

    suspend fun setWakeWordEnabled(enabled: Boolean) {
        context.tdbDataStore.edit { it[Keys.WAKE_WORD] = enabled }
    }

    suspend fun setMotionQuality(quality: MotionQuality) {
        context.tdbDataStore.edit { it[Keys.MOTION_QUALITY] = quality.id }
    }

    suspend fun setAutoMemoryExtraction(enabled: Boolean) {
        context.tdbDataStore.edit { it[Keys.AUTO_MEMORY] = enabled }
    }

    suspend fun setThemePreference(theme: ThemePreference) {
        context.tdbDataStore.edit { it[Keys.THEME_PREF] = theme.id }
    }

    suspend fun setAccentPalette(accent: AccentPalette) {
        context.tdbDataStore.edit { it[Keys.ACCENT_PALETTE] = accent.id }
    }
}
