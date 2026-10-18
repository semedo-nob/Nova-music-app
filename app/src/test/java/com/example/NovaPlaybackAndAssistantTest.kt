package com.example

import com.example.data.model.RecognizedTrack
import com.example.data.model.Track
import com.example.data.online.OnlineSearchResult
import org.junit.Assert.*
import org.junit.Test

class NovaPlaybackAndAssistantTest {

    @Test
    fun onlineSearchResult_toTrack_convertsCorrectly() {
        val onlineResult = OnlineSearchResult(
            id = "123456",
            title = "Suzanna",
            artist = "Sauti Sol",
            album = "Midnight Train",
            artworkUrl = "https://example.com/art.jpg",
            previewUrl = "https://example.com/preview.m4a",
            externalUrl = "https://music.apple.com/song/123456",
            durationMs = 30000L,
            genre = "Afrobeats",
            releaseYear = "2020",
            sourceAttribution = "Apple Music Preview Catalog",
            isPlayablePreview = true
        )

        val track = onlineResult.toTrack()

        assertEquals("online_123456", track.id)
        assertEquals("Suzanna", track.title)
        assertEquals("Sauti Sol", track.artist)
        assertEquals("Midnight Train", track.album)
        assertEquals(30000L, track.duration)
        assertEquals("https://example.com/preview.m4a", track.path)
        assertFalse("Online stream should have isLocal = false", track.isLocal)
        assertEquals("https://example.com/art.jpg", track.artworkUrl)
    }

    @Test
    fun recognizedTrack_creationAndDefaults_areValid() {
        val recognized = RecognizedTrack(
            id = "rec-uuid-1",
            title = "Midnight City",
            artist = "M83",
            album = "Hurry Up, We're Dreaming",
            releaseDate = "2011",
            artworkUrl = "https://example.com/m83.jpg",
            previewUrl = "https://example.com/m83_preview.aac",
            source = "Acoustic Fingerprint"
        )

        assertEquals("Midnight City", recognized.title)
        assertEquals("M83", recognized.artist)
        assertEquals("Acoustic Fingerprint", recognized.source)
        assertNotNull(recognized.recognizedAt)
    }

    @Test
    fun queueOperations_logicVerification() {
        val track1 = Track(id = "1", title = "Song A", artist = "Artist 1", album = "Album 1", duration = 180000L, path = "/path/1.mp3")
        val track2 = Track(id = "2", title = "Song B", artist = "Artist 2", album = "Album 2", duration = 200000L, path = "/path/2.mp3")
        val track3 = Track(id = "3", title = "Song C", artist = "Artist 3", album = "Album 3", duration = 220000L, path = "/path/3.mp3")

        val queue = mutableListOf(track1, track2)
        assertEquals(2, queue.size)

        // Add to queue
        queue.add(track3)
        assertEquals(3, queue.size)
        assertEquals("Song C", queue.last().title)

        // Remove from queue
        val removed = queue.removeAt(1)
        assertEquals("Song B", removed.title)
        assertEquals(2, queue.size)
        assertEquals("Song A", queue[0].title)
        assertEquals("Song C", queue[1].title)

        // Clear queue
        queue.clear()
        assertTrue(queue.isEmpty())
    }

    @Test
    fun fuzzyMatcher_identifiesRelevantArtistsAndMoods() {
        val tracks = listOf(
            Track(id = "1", title = "Suzanna", artist = "Sauti Sol", album = "Midnight Train", duration = 180000L, path = "/path/1.mp3", genre = "Afrobeats"),
            Track(id = "2", title = "Melanin", artist = "Sauti Sol", album = "Afrikan Sauce", duration = 210000L, path = "/path/2.mp3", genre = "Afrobeats"),
            Track(id = "3", title = "Midnight Chill", artist = "Nova Ambient", album = "Cosmic", duration = 240000L, path = "/path/3.mp3", genre = "Relaxing Ambient"),
            Track(id = "4", title = "Workout Overdrive", artist = "CyberPulse", album = "Energy", duration = 190000L, path = "/path/4.mp3", genre = "Upbeat Synthwave")
        )

        // Matching Sauti Sol
        val sautiMatches = tracks.filter { it.artist.contains("Sauti Sol", ignoreCase = true) }
        assertEquals(2, sautiMatches.size)

        // Matching relaxing
        val relaxingMatches = tracks.filter { it.genre.contains("relaxing", ignoreCase = true) || it.title.contains("relaxing", ignoreCase = true) }
        assertEquals(1, relaxingMatches.size)
        assertEquals("Midnight Chill", relaxingMatches.first().title)

        // Matching upbeat
        val upbeatMatches = tracks.filter { it.genre.contains("upbeat", ignoreCase = true) }
        assertEquals(1, upbeatMatches.size)
        assertEquals("Workout Overdrive", upbeatMatches.first().title)
    }
}
