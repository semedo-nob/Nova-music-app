package com.example.data.repository

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.data.database.PlaylistDao
import com.example.data.database.TrackDao
import com.example.data.model.Playlist
import com.example.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.util.UUID

class MusicRepository(
    private val context: Context,
    private val trackDao: TrackDao,
    private val playlistDao: PlaylistDao,
    private val recognizedTrackDao: com.example.data.database.RecognizedTrackDao
) {
    // Flows exposed to the UI
    val allTracks: Flow<List<Track>> = trackDao.getAllTracks()
    val recognizedTracks: Flow<List<com.example.data.model.RecognizedTrack>> = recognizedTrackDao.getAllRecognizedTracks()
    val favoriteTracks: Flow<List<Track>> = trackDao.getFavoriteTracks()
    val recentlyPlayed: Flow<List<Track>> = trackDao.getRecentlyPlayedTracks()
    val mostPlayed: Flow<List<Track>> = trackDao.getMostPlayedTracks()
    val recentlyAdded: Flow<List<Track>> = trackDao.getRecentlyAddedTracks()
    val allPlaylists: Flow<List<Playlist>> = playlistDao.getAllPlaylists()
    val allGenres: Flow<List<String>> = trackDao.getAllGenres()
    val allArtists: Flow<List<String>> = trackDao.getAllArtists()
    val allAlbums: Flow<List<String>> = trackDao.getAllAlbums()

    // Map of smart playlists built reactively or calculated
    fun getTracksInPlaylist(playlistId: String): Flow<List<Track>> {
        return when (playlistId) {
            "smart_favorites" -> favoriteTracks
            "smart_recently_added" -> recentlyAdded
            "smart_recently_played" -> recentlyPlayed
            "smart_most_played" -> mostPlayed
            "smart_never_played" -> {
                // Filter out where playCount == 0 (we can customize or use standard filter)
                trackDao.getAllTracks() // We can filter this in ViewModel
            }
            else -> playlistDao.getTracksInPlaylist(playlistId)
        }
    }

    suspend fun getTrackById(id: String): Track? = trackDao.getTrackById(id)

    suspend fun toggleFavorite(trackId: String) = withContext(Dispatchers.IO) {
        val track = trackDao.getTrackById(trackId)
        if (track != null) {
            trackDao.updateFavoriteStatus(trackId, !track.isFavorite)
        }
    }

    suspend fun recordPlay(trackId: String) = withContext(Dispatchers.IO) {
        trackDao.incrementPlayCount(trackId)
    }

    // Initialize the default "Nova Originals" streamable songs so the app has high-quality playback instantly
    suspend fun checkAndPrepopulateOriginals() = withContext(Dispatchers.IO) {
        val existing = trackDao.getAllTracks().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            Log.d("MusicRepository", "Prepopulating default Nova Originals")
            val presetTracks = listOf(
                Track(
                    id = "nova_cosmic_horizon",
                    title = "Cosmic Horizon",
                    artist = "Nova Aurora",
                    album = "Nebula Dreams",
                    duration = 180000L, // 3:00
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                    genre = "Synthwave",
                    isLocal = false,
                    artworkUrl = "https://images.unsplash.com/photo-1518709268805-4e9042af9f23?w=500&q=80"
                ),
                Track(
                    id = "nova_gemini_pulse",
                    title = "Gemini Pulse",
                    artist = "Lofi Celestial",
                    album = "Quantum Beat",
                    duration = 240000L, // 4:00
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                    genre = "Lofi",
                    isLocal = false,
                    artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500&q=80"
                ),
                Track(
                    id = "nova_cyber_lounge",
                    title = "Cyber Lounge",
                    artist = "Neo-Tokyo",
                    album = "Grid Runner",
                    duration = 300000L, // 5:00
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                    genre = "Electronic",
                    isLocal = false,
                    artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80"
                ),
                Track(
                    id = "nova_purple_pulsar",
                    title = "Purple Pulsar",
                    artist = "Hyperion Beats",
                    album = "Andromeda",
                    duration = 210000L, // 3:30
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                    genre = "Techno",
                    isLocal = false,
                    artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80"
                ),
                Track(
                    id = "nova_gravity_wave",
                    title = "Gravity Wave",
                    artist = "Chronos",
                    album = "Event Horizon",
                    duration = 270000L, // 4:30
                    path = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                    genre = "Ambient",
                    isLocal = false,
                    artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80"
                )
            )
            trackDao.insertTracks(presetTracks)

            // Setup Smart Playlists
            val smartPlaylists = listOf(
                Playlist("smart_favorites", "Liked Songs", "Your absolute favorites", isSmart = true, smartType = "LIKED"),
                Playlist("smart_recently_played", "Recently Played", "Songs you listened to lately", isSmart = true, smartType = "RECENT_PLAYED"),
                Playlist("smart_most_played", "Most Played", "Your top heavy-rotation beats", isSmart = true, smartType = "MOST_PLAYED"),
                Playlist("smart_recently_added", "Recently Added", "Newly imported or discovered tracks", isSmart = true, smartType = "RECENT_ADDED"),
                Playlist("smart_never_played", "Never Played", "Hidden gems you haven't opened yet", isSmart = true, smartType = "NEVER_PLAYED")
            )
            for (p in smartPlaylists) {
                playlistDao.insertPlaylist(p)
            }
        }
    }

    // Scan Android Local Storage for music files
    suspend fun scanLocalStorage(): Int = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val uri: Uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME
        )

        // Only query files that are music
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        var count = 0

        try {
            val cursor = resolver.query(uri, projection, selection, null, null)
            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val nameCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)

                val tracksToInsert = mutableListOf<Track>()

                while (c.moveToNext()) {
                    val mediaId = c.getLong(idCol)
                    val displayName = c.getString(nameCol).orEmpty()
                    if (displayName.startsWith(".")) continue

                    // content:// URIs stay readable under scoped storage. Raw file paths do not.
                    val contentUri = ContentUris.withAppendedId(uri, mediaId).toString()
                    val title = c.getString(titleCol)?.takeIf { it.isNotBlank() }
                        ?: displayName.substringBeforeLast('.')
                    val artist = c.getString(artistCol)?.takeIf { it.isNotBlank() } ?: "Unknown Artist"
                    val album = c.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown Album"
                    val duration = c.getLong(durationCol)
                    val size = c.getLong(sizeCol)

                    val track = Track(
                        id = contentUri,
                        title = title.ifBlank { "Unknown Title" },
                        artist = artist,
                        album = album,
                        duration = if (duration > 0) duration else 180000L,
                        path = contentUri,
                        genre = "Unknown",
                        size = size,
                        isLocal = true,
                        artworkUrl = null
                    )
                    tracksToInsert.add(track)
                }

                if (tracksToInsert.isNotEmpty()) {
                    trackDao.insertTracks(tracksToInsert)
                    trackDao.deleteUnplayableLocalFileTracks()
                    count = tracksToInsert.size
                }
            }
        } catch (e: Exception) {
            Log.e("MusicRepository", "Error scanning local media store", e)
        }

        return@withContext count
    }

    // Playlist CRUD operations
    suspend fun createPlaylist(name: String, description: String = ""): String = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val playlist = Playlist(
            id = id,
            name = name,
            description = description,
            isSmart = false
        )
        playlistDao.insertPlaylist(playlist)
        return@withContext id
    }

    suspend fun deletePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val p = playlistDao.getPlaylistById(playlistId)
        if (p != null && !p.isSmart) {
            playlistDao.deletePlaylist(p)
        }
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.addTrackToPlaylist(playlistId, trackId)
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId, trackId)
    }

    suspend fun duplicatePlaylist(playlistId: String) = withContext(Dispatchers.IO) {
        val source = playlistDao.getPlaylistById(playlistId) ?: return@withContext
        val newId = createPlaylist("${source.name} (Copy)", source.description)
        playlistDao.getTracksInPlaylist(playlistId).firstOrNull()?.forEach { track ->
            playlistDao.addTrackToPlaylist(newId, track.id)
        }
    }

    suspend fun reorderPlaylistTracks(playlistId: String, tracks: List<Track>) = withContext(Dispatchers.IO) {
        tracks.forEachIndexed { index, track ->
            playlistDao.updateTrackOrder(playlistId, track.id, index)
        }
    }

    fun getTracksByArtist(artist: String): Flow<List<Track>> = trackDao.getTracksByArtist(artist)
    fun getTracksByAlbum(album: String): Flow<List<Track>> = trackDao.getTracksByAlbum(album)
    fun getTracksByGenre(genre: String): Flow<List<Track>> = trackDao.getTracksByGenre(genre)
    fun searchTracks(query: String): Flow<List<Track>> = trackDao.searchTracks(query)

    suspend fun saveRecognizedTrack(track: com.example.data.model.RecognizedTrack) = withContext(Dispatchers.IO) {
        recognizedTrackDao.insertRecognizedTrack(track)
    }

    suspend fun deleteRecognizedTrack(track: com.example.data.model.RecognizedTrack) = withContext(Dispatchers.IO) {
        recognizedTrackDao.deleteRecognizedTrack(track)
    }

    suspend fun clearRecognizedHistory() = withContext(Dispatchers.IO) {
        recognizedTrackDao.clearAll()
    }
}
