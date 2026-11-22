package com.aistudio.tdbassistant.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.aistudio.tdbassistant.data.local.ChatMessageEntity
import com.aistudio.tdbassistant.data.local.DailyGoalEntity
import com.aistudio.tdbassistant.data.local.MemoryCategory
import com.aistudio.tdbassistant.data.local.MemoryEntity
import com.aistudio.tdbassistant.data.preferences.AiEngineMode
import com.aistudio.tdbassistant.data.preferences.AiResponseStyle
import com.aistudio.tdbassistant.data.preferences.TdbLanguage
import com.aistudio.tdbassistant.data.preferences.TdbModelOption
import com.aistudio.tdbassistant.domain.TdbPersonality
import com.google.firebase.Firebase
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TAG = "TdbAiRepository"

sealed interface AiStreamEvent {
    data class Chunk(val accumulatedText: String) : AiStreamEvent
    data class Completed(val finalText: String) : AiStreamEvent
}

data class ExtractedMemory(
    val category: MemoryCategory,
    val text: String
)

class TdbAiRepository {
    private val aiClient by lazy { Firebase.ai }
    private val directClient = DirectGeminiClient()

    private val memorySchema by lazy {
        Schema.obj(
            properties = mapOf(
                "shouldSave" to Schema.boolean(description = "True only if the user shared a durable personal fact, preference, goal, project, relationship, milestone, or habit worth remembering."),
                "category" to Schema.enumeration(
                    values = MemoryCategory.entries.map { it.id },
                    description = "Best matching memory category."
                ),
                "memoryText" to Schema.string(description = "Concise factual summary of what to remember.")
            ),
            optionalProperties = listOf("category", "memoryText")
        )
    }

    private val memoryExtractorModel: GenerativeModel by lazy {
        aiClient.generativeModel(
            modelName = TdbModelOption.GEMINI_3_5_FLASH_LITE.modelId,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = memorySchema
                temperature = 0.2f
            }
        )
    }

    fun streamReply(
        engineMode: AiEngineMode,
        customApiKey: String,
        modelOption: TdbModelOption,
        language: TdbLanguage,
        responseStyle: AiResponseStyle,
        memories: List<MemoryEntity>,
        goals: List<DailyGoalEntity>,
        recentHistory: List<ChatMessageEntity>,
        userPrompt: String,
        attachedImage: Bitmap?
    ): Flow<AiStreamEvent> = flow {
        val systemText = TdbPersonality.buildSystemInstruction(language, responseStyle, memories, goals)

        if (engineMode == AiEngineMode.CUSTOM_API_KEY) {
            if (customApiKey.isBlank()) {
                throw IllegalStateException("Personal API Key মোড চালু আছে কিন্তু কোনো key সংরক্ষিত নেই। Settings > API Config-এ key দিন অথবা Firebase AI Logic মোড বেছে নিন।")
            }
            val reply = directClient.generateContent(
                apiKey = customApiKey,
                modelId = modelOption.modelId,
                systemInstruction = systemText,
                historyTurns = recentHistory.takeLast(18).map { it.role to it.text },
                userPrompt = userPrompt,
                attachedImage = attachedImage?.let(::scaleBitmapForUpload)
            )
            emit(AiStreamEvent.Chunk(reply))
            emit(AiStreamEvent.Completed(reply))
            return@flow
        }

        val model = aiClient.generativeModel(
            modelName = modelOption.modelId,
            systemInstruction = content { text(systemText) }
        )

        val historyContents: List<Content> = recentHistory.takeLast(18).map { turn ->
            content(role = if (turn.role == "user") "user" else "model") {
                text(turn.text)
            }
        }

        val chat = model.startChat(history = historyContents)
        val userContent = content("user") {
            if (attachedImage != null) {
                image(scaleBitmapForUpload(attachedImage))
            }
            text(userPrompt)
        }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                chat.sendMessageStream(userContent).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(AiStreamEvent.Chunk(accumulated.toString()))
                    }
                }
            }
            val finalOutput = accumulated.toString().trim()
            if (finalOutput.isEmpty()) {
                throw IllegalStateException("Gemini থেকে কোনো উত্তর পাওয়া যায়নি। আবার চেষ্টা করুন।")
            }
            emit(AiStreamEvent.Completed(finalOutput))
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini stream timed out", e)
            throw IllegalStateException("৬০ সেকেন্ডে উত্তর আসেনি (Timeout)। ইন্টারনেট সংযোগ পরীক্ষা করুন।", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini stream failed", e)
            throw e
        }
    }

    suspend fun testDirectKey(apiKey: String, modelId: String): GeminiDiagnosticResult {
        return directClient.verifyKeyAndModel(apiKey, modelId)
    }

    suspend fun summarizeWebUrl(
        rawUrl: String,
        engineMode: AiEngineMode,
        customApiKey: String,
        modelOption: TdbModelOption,
        language: TdbLanguage
    ): String {
        val cleanUrl = if (rawUrl.startsWith("http://") || rawUrl.startsWith("https://")) rawUrl else "https://$rawUrl"
        val extractedText = withContext(Dispatchers.IO) {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(cleanUrl).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10_000
                    readTimeout = 12_000
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Android) TDB-Assistant/3.0")
                }
                val status = conn.responseCode
                if (status !in 200..299) {
                    throw IllegalStateException("ওয়েবপেজটি খোলা যায়নি (HTTP $status)।")
                }
                val html = conn.inputStream.bufferedReader().use { it.readText() }.take(80_000)
                html
                    .replace(Regex("<script[^>]*>[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<style[^>]*>[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), " ")
                    .replace(Regex("<[^>]+>"), " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()
                    .take(6_500)
            } finally {
                conn?.disconnect()
            }
        }
        if (extractedText.length < 40) {
            throw IllegalStateException("ওয়েবপেজটি থেকে পর্যাপ্ত লেখা পড়া যায়নি।")
        }

        val prompt = "নিচের ওয়েবপেজের মূল তথ্যগুলো সংক্ষেপে পয়েন্ট আকারে বাংলায় (বা অনুরোধকৃত ভাষায়) বুঝিয়ে দাও:\nURL: $cleanUrl\n\nContent:\n$extractedText"
        return if (engineMode == AiEngineMode.CUSTOM_API_KEY && customApiKey.isNotBlank()) {
            directClient.generateContent(
                apiKey = customApiKey,
                modelId = modelOption.modelId,
                systemInstruction = TdbPersonality.buildSystemInstruction(
                    language,
                    AiResponseStyle.CONCISE,
                    emptyList()
                ),
                historyTurns = emptyList(),
                userPrompt = prompt,
                attachedImage = null
            )
        } else {
            val model = aiClient.generativeModel(modelName = modelOption.modelId)
            val response = withTimeout(45_000L) { model.generateContent(prompt) }
            response.text?.trim()?.takeIf { it.isNotEmpty() }
                ?: throw IllegalStateException("সারাংশ তৈরি করা যায়নি।")
        }
    }

    suspend fun extractMemoryIfRelevant(
        userMessage: String,
        existingMemories: List<MemoryEntity>
    ): ExtractedMemory? {
        if (userMessage.length < 12) return null
        return try {
            val existingList = existingMemories.take(15).joinToString("\n") { "- ${it.text}" }
            val prompt = """
                Analyze this single message from the user to a personal AI assistant.
                Only extract a memory if the user explicitly stated a durable personal fact, preference, life goal, active project, relationship, milestone, or habit.
                Do NOT extract transient greetings, questions, commands to open apps, or facts already listed below.
                Existing memories:
                $existingList

                User message: "$userMessage"
            """.trimIndent()

            val response = withTimeout(30_000L) {
                memoryExtractorModel.generateContent(prompt)
            }
            val rawJson = response.text ?: return null
            val obj = JSONObject(rawJson)
            if (!obj.optBoolean("shouldSave", false)) return null
            val memoryText = obj.optString("memoryText").trim()
            val categoryRaw = obj.optString("category", "identity").trim()
            if (memoryText.length < 4) return null
            if (existingMemories.any { it.text.equals(memoryText, ignoreCase = true) }) return null
            ExtractedMemory(
                category = MemoryCategory.fromId(categoryRaw),
                text = memoryText
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Background memory extraction skipped", e)
            null
        }
    }

    private fun scaleBitmapForUpload(source: Bitmap): Bitmap {
        val maxDim = 1280
        val width = source.width
        val height = source.height
        if (width <= maxDim && height <= maxDim) return source
        val scale = minOf(maxDim.toFloat() / width, maxDim.toFloat() / height)
        val newW = (width * scale).toInt().coerceAtLeast(1)
        val newH = (height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(source, newW, newH, true)
    }
}
