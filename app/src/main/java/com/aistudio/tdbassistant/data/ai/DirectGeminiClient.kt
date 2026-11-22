package com.aistudio.tdbassistant.data.ai

import android.graphics.Bitmap
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

data class GeminiDiagnosticResult(
    val ok: Boolean,
    val code: String,
    val message: String
)

object GeminiDiagnostics {
    private val keyRegex = Regex("^[A-Za-z0-9._-]{20,300}$")
    private val modelRegex = Regex("^[A-Za-z0-9._-]{1,100}$")

    fun validateKeyFormat(key: String): GeminiDiagnosticResult? {
        if (key.isBlank()) {
            return GeminiDiagnosticResult(
                ok = false,
                code = "KEY_MISSING",
                message = "API key ফাঁকা। অনুগ্রহ করে আপনার Gemini API key লিখুন বা Firebase AI Logic মোড ব্যবহার করুন।"
            )
        }
        if (!keyRegex.matches(key.trim())) {
            return GeminiDiagnosticResult(
                ok = false,
                code = "KEY_FORMAT_INVALID",
                message = "সঠিক Gemini API key দিন (স্পেস ছাড়া, AIza বা AQ ফরম্যাট)।"
            )
        }
        return null
    }

    fun validateModelFormat(modelId: String): GeminiDiagnosticResult? {
        if (!modelRegex.matches(modelId.trim())) {
            return GeminiDiagnosticResult(
                ok = false,
                code = "MODEL_FORMAT_INVALID",
                message = "সঠিক Gemini মডেল আইডি দিন।"
            )
        }
        return null
    }

    fun mapHttpFailure(status: Int, payload: String): GeminiDiagnosticResult {
        if (status == 429) {
            return GeminiDiagnosticResult(
                ok = false,
                code = "QUOTA_EXCEEDED",
                message = "এই মডেলের কোটা শেষ হয়েছে (HTTP 429)। কিছুক্ষণ পর চেষ্টা করুন বা অন্য মডেল বাছুন।"
            )
        }
        if (status >= 500) {
            return GeminiDiagnosticResult(
                ok = false,
                code = "PROVIDER_UNAVAILABLE",
                message = "Google সার্ভার সাময়িকভাবে ব্যস্ত (HTTP $status)। কিছুক্ষণ পর আবার চেষ্টা করুন।"
            )
        }

        var reason = ""
        var errMsg = ""
        runCatching {
            val error = JSONObject(payload).optJSONObject("error")
            if (error != null) {
                errMsg = error.optString("message")
                val details = error.optJSONArray("details")
                if (details != null) {
                    for (i in 0 until details.length()) {
                        val r = details.optJSONObject(i)?.optString("reason").orEmpty()
                        if (r.isNotEmpty()) {
                            reason = r
                            break
                        }
                    }
                }
            }
        }

        val known = mapOf(
            "API_KEY_INVALID" to "Google জানিয়েছে এই API key সঠিক নয়। Google AI Studio থেকে নতুন key তৈরি করুন।",
            "API_KEY_EXPIRED" to "এই API key-এর মেয়াদ শেষ হয়েছে।",
            "API_KEY_SERVICE_BLOCKED" to "এই key-এর জন্য Gemini API অনুমোদিত নয়। Google Cloud restrictions পরীক্ষা করুন।",
            "SERVICE_DISABLED" to "এই প্রজেক্টে Generative Language API চালু করা নেই।",
            "API_KEY_ANDROID_APP_BLOCKED" to "এই key-এর Android অ্যাপ restriction এই প্যাকেজের সাথে মিলছে না।",
            "CONSUMER_SUSPENDED" to "এই key-এর Google Cloud প্রজেক্ট স্থগিত অবস্থায় আছে।"
        )

        known[reason]?.let { return GeminiDiagnosticResult(false, reason, it) }
        for ((k, v) in known) {
            if (errMsg.contains(k)) return GeminiDiagnosticResult(false, k, v)
        }
        val lower = errMsg.lowercase()
        if (lower.contains("leaked") || lower.contains("exposed")) {
            return GeminiDiagnosticResult(false, "API_KEY_LEAKED", "নিরাপত্তার কারণে Google এই key ব্লক করেছে। নতুন key ব্যবহার করুন।")
        }
        if (lower.contains("api key not valid")) {
            return GeminiDiagnosticResult(false, "API_KEY_INVALID", known.getValue("API_KEY_INVALID"))
        }
        if (status == 404) {
            return GeminiDiagnosticResult(false, "MODEL_UNAVAILABLE", "এই key-এর জন্য নির্বাচিত মডেলটি পাওয়া যায়নি (HTTP 404)।")
        }
        if (status == 401 || status == 403) {
            return GeminiDiagnosticResult(false, "GOOGLE_ACCESS_DENIED", "Google অ্যাক্সেস অস্বীকার করেছে (HTTP $status)।")
        }
        return GeminiDiagnosticResult(false, "GOOGLE_HTTP_ERROR", "Google HTTP $status ত্রুটি ফেরত দিয়েছে।")
    }
}

class DirectGeminiClient {
    suspend fun verifyKeyAndModel(apiKey: String, modelId: String): GeminiDiagnosticResult =
        withContext(Dispatchers.IO) {
            GeminiDiagnostics.validateKeyFormat(apiKey)?.let { return@withContext it }
            GeminiDiagnostics.validateModelFormat(modelId)?.let { return@withContext it }

            var conn: HttpURLConnection? = null
            try {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/${modelId.trim()}")
                conn = (url.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = false
                    requestMethod = "GET"
                    connectTimeout = 10_000
                    readTimeout = 12_000
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("x-goog-api-key", apiKey.trim())
                }
                val status = conn.responseCode
                if (status !in 200..299) {
                    val err = readText(conn.errorStream)
                    return@withContext GeminiDiagnostics.mapHttpFailure(status, err)
                }
                val body = JSONObject(readText(conn.inputStream))
                val methods = body.optJSONArray("supportedGenerationMethods")
                var supportsChat = false
                if (methods != null) {
                    for (i in 0 until methods.length()) {
                        if (methods.optString(i) == "generateContent") {
                            supportsChat = true
                            break
                        }
                    }
                }
                if (!supportsChat) {
                    return@withContext GeminiDiagnosticResult(
                        false,
                        "MODEL_CHAT_UNSUPPORTED",
                        "এই মডেলটি generateContent চ্যাট সমর্থন করে না।"
                    )
                }
                GeminiDiagnosticResult(
                    ok = true,
                    code = "CONNECTED",
                    message = "সংযোগ সফল! $modelId ব্যবহারের জন্য প্রস্তুত।"
                )
            } catch (e: Exception) {
                GeminiDiagnosticResult(
                    ok = false,
                    code = "NETWORK_ERROR",
                    message = e.localizedMessage ?: "নেটওয়ার্ক সংযোগ ব্যর্থ হয়েছে।"
                )
            } finally {
                conn?.disconnect()
            }
        }

    suspend fun generateContent(
        apiKey: String,
        modelId: String,
        systemInstruction: String,
        historyTurns: List<Pair<String, String>>,
        userPrompt: String,
        attachedImage: Bitmap?
    ): String = withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val url = URL(
                "https://generativelanguage.googleapis.com/v1beta/models/${modelId.trim()}:generateContent"
            )
            val contents = JSONArray()
            historyTurns.takeLast(18).forEach { (role, text) ->
                contents.put(
                    JSONObject().apply {
                        put("role", if (role == "user") "user" else "model")
                        put("parts", JSONArray().put(JSONObject().put("text", text)))
                    }
                )
            }
            val userParts = JSONArray()
            if (attachedImage != null) {
                val baos = ByteArrayOutputStream()
                attachedImage.compress(Bitmap.CompressFormat.JPEG, 82, baos)
                val b64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
                userParts.put(
                    JSONObject().put(
                        "inline_data",
                        JSONObject().put("mime_type", "image/jpeg").put("data", b64)
                    )
                )
            }
            userParts.put(JSONObject().put("text", userPrompt))
            contents.put(JSONObject().put("role", "user").put("parts", userParts))

            val payload = JSONObject().apply {
                put(
                    "system_instruction",
                    JSONObject().put("parts", JSONArray().put(JSONObject().put("text", systemInstruction)))
                )
                put("contents", contents)
            }

            conn = (url.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 15_000
                readTimeout = 50_000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("x-goog-api-key", apiKey.trim())
            }
            conn.outputStream.use { out ->
                out.write(payload.toString().toByteArray(StandardCharsets.UTF_8))
            }

            val status = conn.responseCode
            if (status !in 200..299) {
                val err = GeminiDiagnostics.mapHttpFailure(status, readText(conn.errorStream))
                throw IllegalStateException(err.message)
            }

            val responseJson = JSONObject(readText(conn.inputStream))
            val candidates = responseJson.optJSONArray("candidates")
            val firstParts = candidates?.optJSONObject(0)
                ?.optJSONObject("content")
                ?.optJSONArray("parts")
            val reply = buildString {
                if (firstParts != null) {
                    for (i in 0 until firstParts.length()) {
                        append(firstParts.optJSONObject(i)?.optString("text").orEmpty())
                    }
                }
            }.trim()
            if (reply.isEmpty()) {
                throw IllegalStateException("Gemini থেকে কোনো উত্তর পাওয়া যায়নি।")
            }
            reply
        } finally {
            conn?.disconnect()
        }
    }

    private fun readText(stream: InputStream?): String {
        if (stream == null) return ""
        return stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }.take(131_072)
    }
}
