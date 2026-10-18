package com.example.playback.recognition

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.RecognizedTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

sealed class RecognitionState {
    object Idle : RecognitionState()
    data class Listening(val amplitude: Float) : RecognitionState()
    object Matching : RecognitionState()
    data class Found(val track: RecognizedTrack) : RecognitionState()
    data class NoMatch(val reason: String) : RecognitionState()
    object PermissionDenied : RecognitionState()
    data class Error(val message: String) : RecognitionState()
}

class SongRecognitionService(private val context: Context) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .build()

    /**
     * Recognizes audio from recorded WAV audio bytes.
     * Posts acoustic audio bytes to AudD recognition service with catalog enrichment.
     */
    suspend fun recognizeAudioSample(wavBytes: ByteArray): Result<RecognizedTrack> = withContext(Dispatchers.IO) {
        try {
            // Check for API token (or use trial token "test" supported by AudD for sample recognition)
            val apiToken = try {
                // If defined in BuildConfig via Secrets Gradle Plugin
                val field = BuildConfig::class.java.getField("AUDD_API_KEY")
                val key = field.get(null) as? String ?: ""
                if (key.isNotEmpty() && key != "YOUR_AUDD_KEY") key else "test"
            } catch (e: Exception) {
                "test"
            }

            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("api_token", apiToken)
                .addFormDataPart("return", "apple_music,spotify")
                .addFormDataPart(
                    "file",
                    "sample.wav",
                    wavBytes.toRequestBody("audio/wav".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url("https://api.audd.io/")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val bodyString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("SongRecognitionService", "Recognition API failed with HTTP ${response.code}: $bodyString")
                return@withContext Result.failure(Exception("Recognition server error (${response.code}). Please try again."))
            }

            val json = JSONObject(bodyString)
            val status = json.optString("status", "")

            if (status != "success") {
                val errorObj = json.optJSONObject("error")
                val errorMessage = errorObj?.optString("error_message") ?: "Could not recognize song."
                Log.w("SongRecognitionService", "Recognition unsuccessful: $errorMessage")
                return@withContext Result.failure(Exception(errorMessage))
            }

            val resultObj = json.optJSONObject("result")
            if (resultObj == null) {
                // Legitimate "no match" from music recognition database
                return@withContext Result.failure(NoMatchException("No match found in music catalog. Try moving closer to the speaker or playing a clearer section."))
            }

            val title = resultObj.optString("title", "Unknown Title")
            val artist = resultObj.optString("artist", "Unknown Artist")
            val album = resultObj.optString("album", "Unknown Album")
            val releaseDate = resultObj.optString("release_date").takeIf { it.isNotEmpty() }
            val songLink = resultObj.optString("song_link").takeIf { it.isNotEmpty() }

            // Artwork & audio preview links from Apple Music or Spotify metadata
            var artworkUrl: String? = null
            var previewUrl: String? = null

            val appleMusic = resultObj.optJSONObject("apple_music")
            if (appleMusic != null) {
                val artworkObj = appleMusic.optJSONObject("artwork")
                if (artworkObj != null) {
                    val rawUrl = artworkObj.optString("url", "")
                    artworkUrl = rawUrl.replace("{w}x{h}", "600x600")
                }
                val previewsArr = appleMusic.optJSONArray("previews")
                if (previewsArr != null && previewsArr.length() > 0) {
                    previewUrl = previewsArr.getJSONObject(0).optString("url").takeIf { it.isNotEmpty() }
                }
            }

            val spotify = resultObj.optJSONObject("spotify")
            if (spotify != null) {
                if (artworkUrl.isNullOrEmpty()) {
                    val albumObj = spotify.optJSONObject("album")
                    val imagesArr = albumObj?.optJSONArray("images")
                    if (imagesArr != null && imagesArr.length() > 0) {
                        artworkUrl = imagesArr.getJSONObject(0).optString("url").takeIf { it.isNotEmpty() }
                    }
                }
                if (previewUrl.isNullOrEmpty()) {
                    previewUrl = spotify.optString("preview_url").takeIf { it.isNotEmpty() }
                }
            }

            val recognizedTrack = RecognizedTrack(
                id = UUID.randomUUID().toString(),
                title = title,
                artist = artist,
                album = album,
                releaseDate = releaseDate,
                artworkUrl = artworkUrl,
                previewUrl = previewUrl,
                songLink = songLink,
                recognizedAt = System.currentTimeMillis(),
                source = "Acoustic Fingerprint"
            )

            return@withContext Result.success(recognizedTrack)
        } catch (e: Exception) {
            Log.e("SongRecognitionService", "Error during recognition", e)
            return@withContext Result.failure(e)
        }
    }
}

class NoMatchException(message: String) : Exception(message)

/**
 * ShazamKit developer integration bridge:
 * Explains how Apple ShazamKit for Android can be plugged in when Apple Developer credentials are available.
 */
object ShazamKitConfigurationGuide {
    const val INSTRUCTIONS = """
        To use Apple ShazamKit for Android:
        1. Obtain an Apple Developer Program membership.
        2. Create a Media ID and MusicKit private key (.p8) in the Apple Developer Console.
        3. Add the ShazamKit token generator to your secure backend or generate a signed JWT with your Team ID & Key ID.
        4. Include the ShazamKit Android SDK AAR dependency and initialize ShazamKit.createSession(tokenProvider).
        Nova Music already includes acoustic audio capture and AudD cloud recognition, ready to route to ShazamKit or AudD seamlessly.
    """
}
