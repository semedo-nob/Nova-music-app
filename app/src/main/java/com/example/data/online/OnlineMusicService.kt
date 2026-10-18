package com.example.data.online

import android.net.Uri
import android.util.Log
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.Serializable
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class OnlineSearchResult(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String?,
    val previewUrl: String?,
    val externalUrl: String?,
    val durationMs: Long,
    val genre: String,
    val releaseYear: String?,
    val sourceAttribution: String = "Apple Music Preview Catalog",
    val isPlayablePreview: Boolean = true
) : Serializable {
    fun toTrack(): Track {
        return Track(
            id = "online_$id",
            title = title,
            artist = artist,
            album = album,
            duration = durationMs,
            path = previewUrl ?: "",
            genre = genre,
            isLocal = false,
            artworkUrl = artworkUrl
        )
    }
}

class OnlineMusicService {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /**
     * Searches legitimate catalog (iTunes / Apple Music public discovery catalog)
     * for songs matching the query.
     */
    suspend fun searchOnline(query: String, limit: Int = 20): List<OnlineSearchResult> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()

        try {
            val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=$limit"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NovaMusic/1.0 (Android)")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("OnlineMusicService", "Online search failed: HTTP ${response.code}")
                return@withContext emptyList()
            }

            val bodyString = response.body?.string() ?: return@withContext emptyList()
            val json = JSONObject(bodyString)
            val resultsArr = json.optJSONArray("results") ?: return@withContext emptyList()

            val list = mutableListOf<OnlineSearchResult>()
            for (i in 0 until resultsArr.length()) {
                val item = resultsArr.getJSONObject(i)
                val trackId = item.optLong("trackId", 0L)
                if (trackId == 0L) continue

                val trackName = item.optString("trackName", "Unknown Title")
                val artistName = item.optString("artistName", "Unknown Artist")
                val collectionName = item.optString("collectionName", "Unknown Album")
                val rawArtwork = item.optString("artworkUrl100").takeIf { it.isNotEmpty() }
                val artworkUrl = rawArtwork?.replace("100x100bb.jpg", "600x600bb.jpg")
                val previewUrl = item.optString("previewUrl").takeIf { it.isNotEmpty() }
                val trackViewUrl = item.optString("trackViewUrl").takeIf { it.isNotEmpty() }
                val duration = item.optLong("trackTimeMillis", 30000L)
                val genre = item.optString("primaryGenreName", "Pop")
                val releaseDate = item.optString("releaseDate", "")
                val releaseYear = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else null

                list.add(
                    OnlineSearchResult(
                        id = trackId.toString(),
                        title = trackName,
                        artist = artistName,
                        album = collectionName,
                        artworkUrl = artworkUrl,
                        previewUrl = previewUrl,
                        externalUrl = trackViewUrl,
                        durationMs = duration,
                        genre = genre,
                        releaseYear = releaseYear,
                        sourceAttribution = "Apple Music Preview Catalog",
                        isPlayablePreview = !previewUrl.isNullOrEmpty()
                    )
                )
            }
            return@withContext list
        } catch (e: Exception) {
            Log.e("OnlineMusicService", "Error searching online tracks", e)
            return@withContext emptyList()
        }
    }
}
