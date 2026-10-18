package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.model.Track
import com.example.data.online.OnlineMusicService
import com.example.data.online.OnlineSearchResult
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.util.UUID

sealed class AssistantAction {
    data class PlayLocalTracks(val tracks: List<Track>, val message: String) : AssistantAction()
    data class ControlAction(val message: String, val actionType: String) : AssistantAction()
    data class ClarificationNeeded(val question: String, val options: List<String>) : AssistantAction()
    data class LocalNotFoundOfferOnline(val message: String, val onlineResults: List<OnlineSearchResult>) : AssistantAction()
    data class GeneralResponse(val message: String) : AssistantAction()
}

data class AssistantMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: String, // "user" or "nova"
    val text: String,
    val action: AssistantAction? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class NovaMusicAssistant(
    private val context: Context,
    private val repository: MusicRepository,
    private val playbackManager: PlaybackManager,
    private val geminiManager: GeminiManager,
    private val onlineMusicService: OnlineMusicService
) {

    suspend fun processUserRequest(
        userPrompt: String,
        localTracks: List<Track>
    ): AssistantMessage = withContext(Dispatchers.IO) {
        val trimmed = userPrompt.trim()
        val lower = trimmed.lowercase()

        // 1. Direct local media controls fast-path
        if (isSkipIntent(lower)) {
            withContext(Dispatchers.Main) { playbackManager.next() }
            return@withContext AssistantMessage(
                sender = "nova",
                text = "Skipped to the next track in your queue. ⏭️",
                action = AssistantAction.ControlAction("Skipped to next track", "NEXT")
            )
        }

        if (isPreviousIntent(lower)) {
            withContext(Dispatchers.Main) { playbackManager.previous() }
            return@withContext AssistantMessage(
                sender = "nova",
                text = "Playing the previous track. ⏮️",
                action = AssistantAction.ControlAction("Returned to previous track", "PREVIOUS")
            )
        }

        if (isPauseIntent(lower)) {
            withContext(Dispatchers.Main) { playbackManager.pause() }
            return@withContext AssistantMessage(
                sender = "nova",
                text = "Paused playback. ⏸️",
                action = AssistantAction.ControlAction("Playback paused", "PAUSE")
            )
        }

        if (isResumeIntent(lower)) {
            withContext(Dispatchers.Main) { playbackManager.play() }
            return@withContext AssistantMessage(
                sender = "nova",
                text = "Resuming playback. ▶️",
                action = AssistantAction.ControlAction("Playback resumed", "RESUME")
            )
        }

        if (isShuffleIntent(lower)) {
            withContext(Dispatchers.Main) { playbackManager.toggleShuffle() }
            val isShuffled = playbackManager.shuffleEnabled.value
            val status = if (isShuffled) "enabled" else "disabled"
            return@withContext AssistantMessage(
                sender = "nova",
                text = "Shuffle mode $status! 🔀",
                action = AssistantAction.ControlAction("Shuffle mode $status", "SHUFFLE")
            )
        }

        // 2. Natural language music intent parsing
        val intentData = extractIntentWithAiOrRules(userPrompt, localTracks)

        // If clarifying question needed
        if (!intentData.clarificationQuestion.isNullOrEmpty() && intentData.options.isNotEmpty()) {
            return@withContext AssistantMessage(
                sender = "nova",
                text = intentData.clarificationQuestion,
                action = AssistantAction.ClarificationNeeded(
                    question = intentData.clarificationQuestion,
                    options = intentData.options
                )
            )
        }

        // Search local indexed tracks
        val matchedLocalTracks = findMatchingLocalTracks(intentData, localTracks)

        if (matchedLocalTracks.isNotEmpty()) {
            // Found real local tracks in user's library!
            withContext(Dispatchers.Main) {
                playbackManager.playQueue(matchedLocalTracks, 0)
            }

            val trackNames = matchedLocalTracks.take(3).joinToString { "\"${it.title}\"" }
            val extraCount = matchedLocalTracks.size - 3
            val countText = if (extraCount > 0) " and $extraCount more" else ""

            return@withContext AssistantMessage(
                sender = "nova",
                text = "Found ${matchedLocalTracks.size} matching song(s) in your local library ($trackNames$countText). Starting playback now!",
                action = AssistantAction.PlayLocalTracks(
                    tracks = matchedLocalTracks,
                    message = "Playing ${matchedLocalTracks.size} tracks from your local library"
                )
            )
        } else {
            // Not in local library! Explain clearly and search online legitimate catalog
            val queryToSearch = intentData.targetArtist
                ?: intentData.targetTitle
                ?: intentData.targetGenreOrMood
                ?: userPrompt.replace("play", "", ignoreCase = true).trim()

            val onlineResults = onlineMusicService.searchOnline(queryToSearch, limit = 10)

            if (onlineResults.isNotEmpty()) {
                val targetName = if (queryToSearch.isNotBlank()) "\"$queryToSearch\"" else "that request"
                return@withContext AssistantMessage(
                    sender = "nova",
                    text = "I couldn't find any songs for $targetName in your local device library. However, I searched online and found ${onlineResults.size} tracks in the Apple Music discovery catalog! Tap below to play a preview stream.",
                    action = AssistantAction.LocalNotFoundOfferOnline(
                        message = "Found online tracks for $targetName",
                        onlineResults = onlineResults
                    )
                )
            } else {
                return@withContext AssistantMessage(
                    sender = "nova",
                    text = "I checked your local library and searched online, but couldn't find matching songs for \"$queryToSearch\". Try asking for another artist, mood, or title!",
                    action = AssistantAction.GeneralResponse("No matching songs found locally or online.")
                )
            }
        }
    }

    private data class ParsedMusicIntent(
        val intentType: String,
        val targetArtist: String? = null,
        val targetTitle: String? = null,
        val targetGenreOrMood: String? = null,
        val clarificationQuestion: String? = null,
        val options: List<String> = emptyList()
    )

    private suspend fun extractIntentWithAiOrRules(prompt: String, localTracks: List<Track>): ParsedMusicIntent {
        // Fast keyword / regex parsing before falling back to Gemini for complex queries
        val lower = prompt.lowercase()

        // "Play songs by [Artist]"
        val artistRegex = Regex("""(?:play|find|listen to)(?: songs| music| tracks)? by\s+(.+)""", RegexOption.IGNORE_CASE)
        val artistMatch = artistRegex.find(prompt)
        if (artistMatch != null) {
            val artist = artistMatch.groupValues[1].trim()
            return ParsedMusicIntent(intentType = "PLAY_ARTIST", targetArtist = artist)
        }

        // Mood keywords
        val moods = listOf("upbeat", "relaxing", "chill", "study", "workout", "party", "focus", "instrumental", "acoustic", "sad", "happy", "sleep", "calm")
        for (m in moods) {
            if (lower.contains(m)) {
                return ParsedMusicIntent(intentType = "PLAY_GENRE_MOOD", targetGenreOrMood = m)
            }
        }

        // Similar songs intent
        if (lower.contains("similar to") || lower.contains("like this song") || lower.contains("more like")) {
            val current = playbackManager.currentTrack.value
            return ParsedMusicIntent(
                intentType = "SIMILAR_TRACKS",
                targetArtist = current?.artist,
                targetGenreOrMood = current?.genre
            )
        }

        // Try Gemini AI for complex natural language queries
        try {
            val sampleArtists = localTracks.map { it.artist }.distinct().take(15).joinToString(", ")
            val systemInstruction = """
                You are a music intent parser. Given a user prompt and a list of sample library artists ($sampleArtists),
                output ONLY a valid JSON object with fields:
                - "intentType": "PLAY_ARTIST", "PLAY_TITLE", "PLAY_GENRE_MOOD", "SIMILAR_TRACKS", or "SEARCH"
                - "targetArtist": string or null
                - "targetTitle": string or null
                - "targetGenreOrMood": string or null
                - "clarificationQuestion": string or null (only if prompt is too vague or ambiguous)
                - "options": array of strings (only if clarificationQuestion is set)
                No markdown, valid JSON only.
            """.trimIndent()

            val aiResponse = geminiManager.sendMessage(
                userText = "Parse music request: $prompt",
                modelName = "gemini-3.5-flash"
            )

            val cleanJson = aiResponse.replace("```json", "").replace("```", "").trim()
            val obj = JSONObject(cleanJson)
            val intentType = obj.optString("intentType", "SEARCH")
            val artist = obj.optString("targetArtist").takeIf { it.isNotBlank() && it != "null" }
            val title = obj.optString("targetTitle").takeIf { it.isNotBlank() && it != "null" }
            val genreOrMood = obj.optString("targetGenreOrMood").takeIf { it.isNotBlank() && it != "null" }
            val question = obj.optString("clarificationQuestion").takeIf { it.isNotBlank() && it != "null" }

            val optionsList = mutableListOf<String>()
            val optionsArr = obj.optJSONArray("options")
            if (optionsArr != null) {
                for (i in 0 until optionsArr.length()) {
                    optionsList.add(optionsArr.getString(i))
                }
            }

            return ParsedMusicIntent(
                intentType = intentType,
                targetArtist = artist,
                targetTitle = title,
                targetGenreOrMood = genreOrMood,
                clarificationQuestion = question,
                options = optionsList
            )
        } catch (e: Exception) {
            Log.w("NovaMusicAssistant", "AI parsing fallback to heuristic", e)
        }

        // Heuristic fallback
        val cleanQuery = prompt
            .replace("play", "", ignoreCase = true)
            .replace("something", "", ignoreCase = true)
            .replace("from my library", "", ignoreCase = true)
            .replace("songs", "", ignoreCase = true)
            .replace("tracks", "", ignoreCase = true)
            .trim()

        return ParsedMusicIntent(intentType = "SEARCH", targetArtist = cleanQuery, targetTitle = cleanQuery)
    }

    private fun findMatchingLocalTracks(intent: ParsedMusicIntent, localTracks: List<Track>): List<Track> {
        if (localTracks.isEmpty()) return emptyList()

        // 1. By Artist
        if (!intent.targetArtist.isNullOrBlank()) {
            val target = intent.targetArtist.lowercase()
            val matches = localTracks.filter { it.artist.lowercase().contains(target) }
            if (matches.isNotEmpty()) return matches
        }

        // 2. By Title
        if (!intent.targetTitle.isNullOrBlank()) {
            val target = intent.targetTitle.lowercase()
            val matches = localTracks.filter { it.title.lowercase().contains(target) }
            if (matches.isNotEmpty()) return matches
        }

        // 3. By Genre or Mood
        if (!intent.targetGenreOrMood.isNullOrBlank()) {
            val mood = intent.targetGenreOrMood.lowercase()
            val matches = localTracks.filter {
                it.genre.lowercase().contains(mood) ||
                it.title.lowercase().contains(mood) ||
                it.album.lowercase().contains(mood)
            }
            if (matches.isNotEmpty()) return matches
        }

        return emptyList()
    }

    private fun isSkipIntent(lower: String): Boolean {
        return lower == "skip" || lower == "next" || lower == "next song" || lower == "skip this song" ||
                lower == "play next" || lower == "play the next song"
    }

    private fun isPreviousIntent(lower: String): Boolean {
        return lower == "previous" || lower == "prev" || lower == "previous song" || lower == "go back" ||
                lower == "play previous" || lower == "play last song"
    }

    private fun isPauseIntent(lower: String): Boolean {
        return lower == "pause" || lower == "pause music" || lower == "stop" || lower == "stop music" ||
                lower == "pause playback"
    }

    private fun isResumeIntent(lower: String): Boolean {
        return lower == "resume" || lower == "continue" || lower == "unpause" || lower == "play" ||
                lower == "start playing"
    }

    private fun isShuffleIntent(lower: String): Boolean {
        return lower.contains("shuffle") || lower.contains("randomize")
    }
}
