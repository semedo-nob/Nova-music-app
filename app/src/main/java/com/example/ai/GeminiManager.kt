package com.example.ai

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    // Chat history for multi-turn chatbot
    private val chatHistory = mutableListOf<JSONObject>()

    init {
        // Reset chat with default system prompt
        resetChat()
    }

    fun resetChat() {
        chatHistory.clear()
    }

    private fun getApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            Log.e("GeminiManager", "Gemini API Key is empty or placeholder!")
        }
        return key
    }

    /**
     * AI Chatbot with Multi-turn history.
     */
    suspend fun sendMessage(userText: String, modelName: String = "gemini-3.5-flash"): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext "API Key is missing. Configure it in the AI Studio Secrets panel."

        // Add user message to history
        val userTurn = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", userText)))
        }
        chatHistory.add(userTurn)

        // Construct contents list
        val contentsArray = JSONArray()
        chatHistory.forEach { contentsArray.put(it) }

        // Request Body
        val requestBodyJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", 
                    "You are Nova AI DJ, the intelligent, charismatic holographic music guide of Nova Music app. " +
                    "Your personality is futuristic, friendly, and knowledgeable about all genres, synthesizers, and artists. " +
                    "Keep responses concise, engaging, and structured like a real radio DJ."
                )))
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBodyJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiManager", "Chat failed: Code ${response.code}, Body: $errBody")
                    return@withContext "Nova DJ is currently offline: ${response.message}"
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val modelText = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                // Save model's response to history
                val modelTurn = JSONObject().apply {
                    put("role", "model")
                    put("parts", JSONArray().put(JSONObject().put("text", modelText)))
                }
                chatHistory.add(modelTurn)

                return@withContext modelText
            }
        } catch (e: Exception) {
            Log.e("GeminiManager", "Error in chat request", e)
            return@withContext "Connection error: ${e.localizedMessage}"
        }
    }

    /**
     * AI Playlist Generator using gemini-3.1-pro-preview with HIGH thinking level.
     */
    suspend fun generateAiPlaylist(moodPrompt: String): List<SuggestedSong> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext emptyList()

        val systemPrompt = "You are a specialized music recommendation engine. Given a user mood or prompt, " +
                "recommend a curated list of exactly 5 amazing tracks. Output ONLY a valid JSON array of objects, " +
                "where each object contains: 'title', 'artist', 'genre', 'durationSeconds', and 'moodDescription'. " +
                "Do not include markdown wrapper blocks or formatting except valid JSON."

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", "Recommend tracks for: $moodPrompt")))
            }))
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            // Configure High Thinking Level as mandated by the instructions
            put("generationConfig", JSONObject().apply {
                put("thinkingConfig", JSONObject().put("thinkingLevel", "HIGH"))
                put("responseMimeType", "application/json")
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiManager", "AI Playlist failed: ${response.code}, Body: $errBody")
                    return@withContext emptyList()
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val responseText = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")

                val jsonArray = JSONArray(responseText.trim())
                val list = mutableListOf<SuggestedSong>()
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        SuggestedSong(
                            title = obj.getString("title"),
                            artist = obj.getString("artist"),
                            genre = obj.optString("genre", "Ambient"),
                            durationSeconds = obj.optLong("durationSeconds", 180),
                            moodDescription = obj.optString("moodDescription", "AI Suggested")
                        )
                    )
                }
                return@withContext list
            }
        } catch (e: Exception) {
            Log.e("GeminiManager", "Error generating smart playlist", e)
            return@withContext emptyList()
        }
    }

    /**
     * Search Grounding using gemini-3.5-flash.
     */
    suspend fun askWithSearchGrounding(question: String): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext "API Key missing."

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", question)))
            }))
            // Add googleSearch tool to enable Search Grounding
            put("tools", JSONArray().put(JSONObject().put("googleSearch", JSONObject())))
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiManager", "Search Grounding failed: ${response.code}, Body: $errBody")
                    return@withContext "Grounding request failed."
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val candidates = responseJson.getJSONArray("candidates")
                val candidateObj = candidates.getJSONObject(0)
                val contentObj = candidateObj.getJSONObject("content")
                val partsArr = contentObj.getJSONArray("parts")
                val mainText = partsArr.getJSONObject(0).getString("text")

                // Optionally extract grounding metadata if present to show citations
                val groundingMetadata = candidateObj.optJSONObject("groundingMetadata")
                val searchQueries = groundingMetadata?.optJSONArray("webSearchQueries")
                val queriesText = if (searchQueries != null && searchQueries.length() > 0) {
                    val queries = mutableListOf<String>()
                    for (i in 0 until searchQueries.length()) {
                        queries.add(searchQueries.getString(i))
                    }
                    "\n\n*Grounded Search Queries: ${queries.joinToString(", ")}*"
                } else ""

                return@withContext mainText + queriesText
            }
        } catch (e: Exception) {
            Log.e("GeminiManager", "Error in grounding request", e)
            return@withContext "Error fetching grounded info: ${e.localizedMessage}"
        }
    }

    /**
     * Text To Speech using gemini-3.1-flash-tts-preview.
     * Returns Base64 audio byte stream (or null on error).
     */
    suspend fun generateSpeech(textToSpeak: String): String? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext null

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-flash-tts-preview:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", "Read this text like a cool sci-fi musical DJ: $textToSpeak")))
            }))
            put("generationConfig", JSONObject().apply {
                put("responseModalities", JSONArray().put("AUDIO"))
                put("speechConfig", JSONObject().put("voiceConfig", JSONObject().put("prebuiltVoiceConfig", JSONObject().put("voiceName", "Kore"))))
            })
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiManager", "TTS failed: ${response.code}, Body: $errBody")
                    return@withContext null
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                val parts = responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")

                for (i in 0 until parts.length()) {
                    val part = parts.getJSONObject(i)
                    val inlineData = part.optJSONObject("inlineData")
                    if (inlineData != null) {
                        val mimeType = inlineData.optString("mimeType", "")
                        if (mimeType.contains("audio", true)) {
                            return@withContext inlineData.getString("data") // Base64 data
                        }
                    }
                }
                return@withContext null
            }
        } catch (e: Exception) {
            Log.e("GeminiManager", "Error in TTS request", e)
            return@withContext null
        }
    }

    /**
     * Audio Transcription using gemini-3.5-flash.
     */
    suspend fun transcribeAudio(audioBytes: ByteArray, mimeType: String = "audio/wav"): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty()) return@withContext "API Key missing."

        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().put(JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().apply {
                    put("text", "Please transcribe the following audio recording exactly as spoken. Do not add formatting.")
                }).put(JSONObject().apply {
                    put("inlineData", JSONObject().apply {
                        put("mimeType", mimeType)
                        put("data", base64Audio)
                    })
                }))
            }))
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errBody = response.body?.string() ?: ""
                    Log.e("GeminiManager", "Transcription failed: ${response.code}, Body: $errBody")
                    return@withContext "Transcription request failed."
                }

                val responseBodyStr = response.body?.string() ?: ""
                val responseJson = JSONObject(responseBodyStr)
                return@withContext responseJson
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
            }
        } catch (e: Exception) {
            Log.e("GeminiManager", "Error in transcription request", e)
            return@withContext "Error in transcription: ${e.localizedMessage}"
        }
    }
}

data class SuggestedSong(
    val title: String,
    val artist: String,
    val genre: String,
    val durationSeconds: Long,
    val moodDescription: String
)
